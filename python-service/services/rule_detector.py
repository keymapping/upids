import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config
from utils.logger import setup_logger
import random

logger = setup_logger('rule_detector', config.LOG_LEVEL)


class RuleDetector:
    """规则检测器 - 基于管线属性判定风险"""

    def __init__(self):
        self.weights = config.RULE_RISK_WEIGHTS

    def detect(self, pipeline_data):
        """
        基于管线属性进行规则检测

        Args:
            pipeline_data: 包含管线属性的字典
                - diameter: 管径 (mm)
                - material: 材质 (steel/iron/concrete/pe/etc)
                - install_year: 安装年份

        Returns:
            dict: 检测结果
        """
        diameter = pipeline_data.get('diameter', 300)
        material = pipeline_data.get('material', 'steel')
        install_year = pipeline_data.get('install_year', 2000)

        diameter_score = self._calculate_diameter_risk(diameter)
        material_score = self._calculate_material_risk(material)
        age_score = self._calculate_age_risk(install_year)

        total_score = (
            diameter_score * self.weights['diameter'] +
            material_score * self.weights['material'] +
            age_score * self.weights['age']
        )

        defect_type = self._determine_defect_type(total_score)
        severity_level = self._determine_severity(total_score)
        confidence = self._calculate_confidence(total_score)

        logger.info(
            '规则检测完成: diameter=%d, material=%s, year=%d, score=%.2f, defect=%s',
            diameter, material, install_year, total_score, defect_type
        )

        return {
            'defectType': defect_type,
            'confidenceScore': round(confidence, 2),
            'severityLevel': severity_level,
            'bbox': self._generate_bbox(),
            'source': 'rule'
        }

    def _calculate_diameter_risk(self, diameter):
        """管径风险：小管径(<300mm)风险更高"""
        if diameter < 150:
            return 0.8
        elif diameter < 300:
            return 0.5
        else:
            return 0.2

    def _calculate_material_risk(self, material):
        """材质风险：铸铁>钢材>混凝土>PE"""
        material_risks = {
            'iron': 0.9,
            'cast_iron': 0.9,
            'steel': 0.7,
            'concrete': 0.5,
            'pvc': 0.3,
            'pe': 0.2
        }
        return material_risks.get(material.lower(), 0.5)
    
    def _calculate_age_risk(self, install_year):
        """年限风险：年限越长风险越高"""
        from datetime import datetime
        current_year = datetime.now().year
        age = current_year - install_year
        
        if age > 40:
            return 0.9
        elif age > 30:
            return 0.7
        elif age > 20:
            return 0.5
        elif age > 10:
            return 0.3
        else:
            return 0.1
    
    def _determine_defect_type(self, score):
        """根据风险分数确定缺陷类型"""
        if score < 0.3:
            return 'none'
        elif score < 0.5:
            return 'corrosion'
        elif score < 0.7:
            return 'crack'
        else:
            return 'fracture'
    
    def _determine_severity(self, score):
        """根据风险分数确定严重等级 (1-5)"""
        if score < 0.2:
            return 1
        elif score < 0.4:
            return 2
        elif score < 0.6:
            return 3
        elif score < 0.8:
            return 4
        else:
            return 5
    
    def _calculate_confidence(self, score):
        """计算置信度：规则检测的置信度通常较低"""
        # 规则检测置信度在0.6-0.8之间
        return 0.6 + (score * 0.2)
    
    def _generate_bbox(self):
        """生成边界框坐标（模拟）"""
        x = random.randint(100, 500)
        y = random.randint(100, 500)
        w = random.randint(50, 200)
        h = random.randint(50, 200)
        return f"[{x},{y},{w},{h}]"


# 单例实例
rule_detector = RuleDetector()