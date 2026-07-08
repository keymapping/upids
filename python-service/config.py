import os

class Config:
    """Flask应用配置"""

    # 服务配置
    PORT = int(os.environ.get('PORT', 5000))
    DEBUG = os.environ.get('FLASK_DEBUG', 'True').lower() == 'true'
    LOG_LEVEL = os.environ.get('LOG_LEVEL', 'INFO')

    # 数据库配置
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_PORT = os.environ.get('DB_PORT', '5432')
    DB_NAME = os.environ.get('DB_NAME', 'upids')
    DB_USER = os.environ.get('DB_USER', 'postgres')
    DB_PASSWORD = os.environ.get('DB_PASSWORD', 'password')

    # Flask配置
    SECRET_KEY = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')

    # 检测配置
    MODEL_VERSION = '2.0.0'
    MODEL_TYPE = 'rule+cnn'  # CNN模型 + 规则检测双通道

    # 规则检测器配置
    RULE_RISK_WEIGHTS = {
        'diameter': 0.3,      # 管径权重
        'material': 0.3,      # 材质权重
        'age': 0.4            # 年限权重
    }

    # AI检测器配置
    AI_CONFIDENCE_MIN = 0.85
    AI_CONFIDENCE_MAX = 0.99

    # CNN模型配置
    CNN_MODEL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'checkpoints')
    CNN_MODEL_PATH = os.path.join(CNN_MODEL_DIR, 'best_model.pth')
    CNN_IMAGE_SIZE = 128
    CNN_CLASSES = ['none', 'crack', 'corrosion', 'fracture']

    # 训练配置
    TRAIN_EPOCHS = 30
    TRAIN_BATCH_SIZE = 32
    TRAIN_LEARNING_RATE = 0.001
    TRAIN_SAMPLES_PER_CLASS = 250

    # 融合配置
    FUSION_RULE_WEIGHT = 0.4
    FUSION_AI_WEIGHT = 0.6

    @property
    def DATABASE_URI(self):
        return f'postgresql://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}'

config = Config()
