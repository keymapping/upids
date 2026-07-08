from flask import Flask, jsonify
from flask_cors import CORS
from routes.detect import detect_bp
from config import config
from utils.logger import setup_logger

logger = setup_logger('upids', config.LOG_LEVEL)


def create_app():
    """创建Flask应用"""
    app = Flask(__name__)

    # 加载配置
    app.config['SECRET_KEY'] = config.SECRET_KEY
    app.config['DEBUG'] = config.DEBUG

    # 配置 CORS
    CORS(app)

    # 注册路由蓝图
    app.register_blueprint(detect_bp, url_prefix='')

    # 全局错误处理
    @app.errorhandler(400)
    def bad_request(e):
        logger.warning('400 Bad Request: %s', str(e))
        return jsonify({'error': '请求参数错误'}), 400

    @app.errorhandler(404)
    def not_found(e):
        logger.warning('404 Not Found: %s', str(e))
        return jsonify({'error': '资源未找到'}), 404

    @app.errorhandler(405)
    def method_not_allowed(e):
        logger.warning('405 Method Not Allowed: %s', str(e))
        return jsonify({'error': '请求方法不允许'}), 405

    @app.errorhandler(500)
    def internal_error(e):
        logger.error('500 Internal Server Error: %s', str(e), exc_info=True)
        return jsonify({'error': '服务器内部错误'}), 500

    logger.info('Flask 应用已创建, model=%s, debug=%s', config.MODEL_TYPE, config.DEBUG)
    return app


# 创建应用实例
app = create_app()


if __name__ == '__main__':
    logger.info('启动服务: port=%d, debug=%s', config.PORT, config.DEBUG)
    app.run(
        host='0.0.0.0',
        port=config.PORT,
        debug=config.DEBUG
    )
