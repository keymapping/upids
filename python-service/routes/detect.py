import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from flask import Blueprint, request, jsonify
from services.rule_detector import rule_detector
from services.ai_detector import ai_detector
from services.fusion import fusion_detector
from utils.db_client import db_client
from config import config
from utils.logger import setup_logger
from datetime import datetime
import random
import subprocess
import threading

logger = setup_logger('detect', config.LOG_LEVEL)

detect_bp = Blueprint('detect', __name__)

# 训练状态
training_status = {
    'is_training': False,
    'progress': 0,
    'message': '',
    'last_trained': None
}


@detect_bp.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    model_info = ai_detector.get_model_info()
    return jsonify({
        'status': 'UP',
        'model': config.MODEL_TYPE,
        'model_version': config.MODEL_VERSION,
        'ai_model': model_info,
        'time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    })


@detect_bp.route('/detect', methods=['POST'])
def detect_defect():
    """
    图像缺陷识别接口

    Request:
        {
            "imagePath": "...",
            "pipelineId": "...",
            "recordId": 123
        }

    Returns:
        JSON: {
            "defectType": "none|crack|corrosion|fracture",
            "confidenceScore": 0.92,
            "severityLevel": 3,
            "bbox": "[120,80,200,150]",
            "source": "rule|ai|fusion",
            "location": { "lng": 113.65, "lat": 34.75 }
        }
    """
    try:
        data = request.get_json()

        if not data:
            logger.warning('请求数据为空')
            return jsonify({'error': '请求数据不能为空'}), 400

        image_path = data.get('imagePath')
        pipeline_id = data.get('pipelineId')
        record_id = data.get('recordId')

        if not all([image_path, pipeline_id, record_id]):
            logger.warning('缺少必需参数: imagePath=%s, pipelineId=%s, recordId=%s', image_path, pipeline_id, record_id)
            return jsonify({'error': '缺少必需参数'}), 400

        logger.info('收到检测请求: imagePath=%s, pipelineId=%s, recordId=%s', image_path, pipeline_id, record_id)

        # 模拟获取管线数据
        pipeline_data = _get_pipeline_data(pipeline_id)

        # 规则检测
        rule_result = rule_detector.detect(pipeline_data)

        # AI检测（使用CNN模型或模拟）
        ai_result = ai_detector.detect(image_path)

        # 双通道融合
        fusion_result = fusion_detector.fuse(rule_result, ai_result)

        # 添加位置信息
        fusion_result['location'] = _get_location(pipeline_id)
        fusion_result['success'] = True

        logger.info('检测完成: result=%s', fusion_result)
        return jsonify(fusion_result)

    except Exception as e:
        logger.error('检测异常: %s', str(e), exc_info=True)
        return jsonify({'error': str(e)}), 500


@detect_bp.route('/model/status', methods=['GET'])
def model_status():
    """获取模型状态"""
    model_info = ai_detector.get_model_info()

    # 检查模型文件是否存在
    model_exists = os.path.exists(config.CNN_MODEL_PATH)

    # 获取训练历史
    history_path = os.path.join(config.CNN_MODEL_DIR, 'training_history.json')
    training_history = None
    if os.path.exists(history_path):
        import json
        with open(history_path, 'r') as f:
            training_history = json.load(f)

    return jsonify({
        'model_type': config.MODEL_TYPE,
        'model_version': config.MODEL_VERSION,
        'model_file_exists': model_exists,
        'model_info': model_info,
        'training_status': training_status,
        'training_history': training_history,
        'config': {
            'image_size': config.CNN_IMAGE_SIZE,
            'classes': config.CNN_CLASSES,
            'epochs': config.TRAIN_EPOCHS,
            'batch_size': config.TRAIN_BATCH_SIZE,
            'learning_rate': config.TRAIN_LEARNING_RATE
        }
    })


@detect_bp.route('/model/train', methods=['POST'])
def train_model():
    """触发模型训练"""
    global training_status

    if training_status['is_training']:
        return jsonify({'error': '模型正在训练中，请稍后再试'}), 409

    def run_training():
        global training_status
        training_status['is_training'] = True
        training_status['progress'] = 0
        training_status['message'] = '开始训练...'

        try:
            # 获取训练参数
            data = request.get_json() if request.is_json else {}
            epochs = data.get('epochs', config.TRAIN_EPOCHS)
            samples = data.get('samples', config.TRAIN_SAMPLES_PER_CLASS)

            training_status['message'] = f'生成训练数据 ({samples} samples/class)...'

            # 执行训练脚本
            script_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'train.py')

            cmd = [
                sys.executable, script_path,
                '--epochs', str(epochs),
                '--samples', str(samples)
            ]

            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1
            )

            # 读取输出
            for line in iter(process.stdout.readline, ''):
                line = line.strip()
                if line:
                    logger.info('训练输出: %s', line)

                    # 解析进度
                    if 'Epoch' in line and '/' in line:
                        try:
                            parts = line.split('Epoch [')[1].split(']')[0]
                            current, total = parts.split('/')
                            progress = int(current) / int(total) * 100
                            training_status['progress'] = progress
                            training_status['message'] = f'训练中: Epoch {current}/{total}'
                        except:
                            pass
                    elif '生成' in line:
                        training_status['message'] = line
                    elif '训练完成' in line:
                        training_status['message'] = '训练完成!'
                        training_status['progress'] = 100

            process.wait()

            if process.returncode == 0:
                training_status['message'] = '训练完成!'
                training_status['progress'] = 100
                training_status['last_trained'] = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                logger.info('模型训练成功')
            else:
                training_status['message'] = f'训练失败 (返回码: {process.returncode})'
                logger.error('模型训练失败')

        except Exception as e:
            training_status['message'] = f'训练异常: {str(e)}'
            logger.error('训练异常: %s', str(e), exc_info=True)
        finally:
            training_status['is_training'] = False

    # 在后台线程中运行训练
    thread = threading.Thread(target=run_training)
    thread.daemon = True
    thread.start()

    return jsonify({'message': '训练已启动', 'status': training_status})


@detect_bp.route('/model/train/status', methods=['GET'])
def train_status():
    """获取训练状态"""
    return jsonify(training_status)


def _get_pipeline_data(pipeline_id):
    """获取管线数据（从数据库）"""
    data = db_client.get_pipeline_data(pipeline_id)
    return {
        'diameter': data.get('diameter', random.randint(100, 600)),
        'material': data.get('material', random.choice(['steel', 'iron', 'concrete', 'pe'])),
        'install_year': data.get('installYear', random.randint(1970, 2020))
    }


def _get_location(pipeline_id):
    """获取管线位置（从数据库）"""
    return db_client.get_pipeline_location(pipeline_id)
