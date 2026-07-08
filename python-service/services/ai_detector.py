import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import torch
import torch.nn.functional as F
from torchvision import transforms
from PIL import Image
import numpy as np
import random

from config import config
from utils.logger import setup_logger

logger = setup_logger('ai_detector', config.LOG_LEVEL)


class AIDetector:
    """
    AI检测器 - 使用CNN模型进行管道缺陷图像识别

    支持两种模式:
    - cnn: 使用训练好的CNN模型进行真实推理
    - mock: 开发阶段使用模拟检测（fallback）
    """

    def __init__(self):
        self.confidence_min = config.AI_CONFIDENCE_MIN
        self.confidence_max = config.AI_CONFIDENCE_MAX
        self.model = None
        self.device = None
        self.transform = None
        self.class_names = ['none', 'crack', 'corrosion', 'fracture']
        self.model_loaded = False

        # 尝试加载CNN模型
        self._load_model()

    def _load_model(self):
        """加载训练好的CNN模型"""
        try:
            # 延迟导入，避免未安装torch时崩溃
            from models import DefectCNN, NUM_CLASSES

            model_path = os.path.join(
                os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                'checkpoints', 'best_model.pth'
            )

            if not os.path.exists(model_path):
                logger.warning('CNN模型文件不存在: %s，将使用模拟模式', model_path)
                return

            self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            self.model = DefectCNN(num_classes=NUM_CLASSES)

            checkpoint = torch.load(model_path, map_location=self.device)
            self.model.load_state_dict(checkpoint['model_state_dict'])
            self.model.to(self.device)
            self.model.eval()

            # 图像预处理管道
            self.transform = transforms.Compose([
                transforms.Resize((128, 128)),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                   std=[0.229, 0.224, 0.225])
            ])

            self.model_loaded = True
            logger.info('CNN模型加载成功: device=%s, val_acc=%.4f',
                       self.device, checkpoint.get('val_acc', 0))

        except ImportError:
            logger.warning('PyTorch未安装，将使用模拟模式')
        except Exception as e:
            logger.error('CNN模型加载失败: %s，将使用模拟模式', str(e))

    def detect(self, image_path):
        """
        图像缺陷检测

        Args:
            image_path: 图像路径

        Returns:
            dict: 检测结果
        """
        if self.model_loaded and self._image_exists(image_path):
            return self._detect_with_cnn(image_path)
        else:
            return self._detect_mock(image_path)

    def _image_exists(self, image_path):
        """检查图像文件是否存在"""
        if not image_path:
            return False

        # 处理相对路径
        if not os.path.isabs(image_path):
            base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            image_path = os.path.join(base_dir, '..', 'uploads', image_path)

        return os.path.exists(image_path)

    def _detect_with_cnn(self, image_path):
        """使用CNN模型进行检测"""
        try:
            # 加载并预处理图像
            if not os.path.isabs(image_path):
                base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
                image_path = os.path.join(base_dir, '..', 'uploads', image_path)

            image = Image.open(image_path).convert('RGB')
            input_tensor = self.transform(image).unsqueeze(0).to(self.device)

            # 模型推理
            with torch.no_grad():
                outputs = self.model(input_tensor)
                probabilities = F.softmax(outputs, dim=1)[0]

            # 获取预测结果
            predicted_idx = torch.argmax(probabilities).item()
            confidence = probabilities[predicted_idx].item()
            defect_type = self.class_names[predicted_idx]

            # 根据置信度和缺陷类型确定严重等级
            severity_level = self._determine_severity(defect_type, confidence)

            # 生成边界框（基于图像分析）
            bbox = self._generate_bbox_from_image(image)

            logger.info(
                'CNN检测完成: image=%s, defect=%s, confidence=%.4f, severity=%d',
                image_path, defect_type, confidence, severity_level
            )

            return {
                'defectType': defect_type,
                'confidenceScore': round(confidence, 4),
                'severityLevel': severity_level,
                'bbox': bbox,
                'source': 'ai_cnn'
            }

        except Exception as e:
            logger.error('CNN检测失败: %s，回退到模拟模式', str(e))
            return self._detect_mock(image_path)

    def _detect_mock(self, image_path):
        """模拟检测（fallback）"""
        defect_type = self._random_defect_type()
        confidence = self._random_confidence()
        severity_level = self._determine_severity(defect_type, confidence)
        bbox = self._generate_bbox()

        logger.info(
            '模拟AI检测: image=%s, defect=%s, confidence=%.2f, severity=%d',
            image_path, defect_type, confidence, severity_level
        )

        return {
            'defectType': defect_type,
            'confidenceScore': round(confidence, 2),
            'severityLevel': severity_level,
            'bbox': bbox,
            'source': 'ai_mock'
        }

    def _random_defect_type(self):
        """随机生成缺陷类型"""
        defect_types = ['none', 'crack', 'corrosion', 'fracture']
        weights = [0.3, 0.25, 0.25, 0.2]
        return random.choices(defect_types, weights=weights)[0]

    def _random_confidence(self):
        """生成随机置信度"""
        return random.uniform(self.confidence_min, self.confidence_max)

    def _determine_severity(self, defect_type, confidence):
        """根据缺陷类型和置信度确定严重等级"""
        if defect_type == 'none':
            return 1

        # 基础等级
        base_severity = {
            'crack': 2,
            'corrosion': 3,
            'fracture': 4
        }.get(defect_type, 2)

        # 根据置信度调整
        if confidence > 0.95:
            return min(base_severity + 1, 5)
        elif confidence < 0.7:
            return max(base_severity - 1, 1)
        else:
            return base_severity

    def _generate_bbox_from_image(self, image):
        """基于图像分析生成边界框"""
        # 转换为numpy数组进行简单分析
        img_array = np.array(image)
        h, w = img_array.shape[:2]

        # 计算图像梯度（边缘检测的简化版本）
        gray = np.mean(img_array, axis=2)
        gradient_x = np.abs(np.diff(gray, axis=1))
        gradient_y = np.abs(np.diff(gray, axis=0))

        # 找到梯度最大的区域
        gradient_mag = np.sqrt(
            gradient_x[:-1, :]**2 + gradient_y[:, :-1]**2
        )

        # 找到最显著区域的中心
        threshold = np.percentile(gradient_mag, 90)
        high_gradient = gradient_mag > threshold

        if np.any(high_gradient):
            y_coords, x_coords = np.where(high_gradient)
            center_x = int(np.mean(x_coords))
            center_y = int(np.mean(y_coords))

            # 生成边界框
            box_w = random.randint(40, 100)
            box_h = random.randint(40, 100)
            x1 = max(0, center_x - box_w // 2)
            y1 = max(0, center_y - box_h // 2)

            return f"[{x1},{y1},{box_w},{box_h}]"
        else:
            return self._generate_bbox()

    def _generate_bbox(self):
        """生成随机边界框"""
        x = random.randint(100, 500)
        y = random.randint(100, 500)
        w = random.randint(50, 200)
        h = random.randint(50, 200)
        return f"[{x},{y},{w},{h}]"

    def get_model_info(self):
        """获取模型信息"""
        return {
            'model_loaded': self.model_loaded,
            'model_type': 'cnn' if self.model_loaded else 'mock',
            'device': str(self.device) if self.device else 'cpu',
            'class_names': self.class_names
        }


# 单例实例
ai_detector = AIDetector()
