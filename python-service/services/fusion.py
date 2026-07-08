import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config import config
from utils.logger import setup_logger

logger = setup_logger('fusion', config.LOG_LEVEL)


class FusionDetector:
    """双通道融合 - 规则检测 + AI检测结果融合"""

    def __init__(self):
        self.rule_weight = config.FUSION_RULE_WEIGHT
        self.ai_weight = config.FUSION_AI_WEIGHT

    def fuse(self, rule_result, ai_result):
        """
        融合规则检测和AI检测结果

        策略:
        - 规则权重 0.4，AI 权重 0.6
        - 两者结果一致 → 置信度加成 10%
        - 不一致 → 取 AI 结果
        - 严重等级取两者较高值

        Args:
            rule_result: 规则检测结果
            ai_result: AI检测结果

        Returns:
            dict: 融合后的检测结果，source="fusion"
        """
        rule_type = rule_result['defectType']
        ai_type = ai_result['defectType']

        if rule_type != 'none' and ai_type != 'none':
            # 两者都检测到缺陷
            fused_confidence = (
                rule_result['confidenceScore'] * self.rule_weight +
                ai_result['confidenceScore'] * self.ai_weight
            )

            if rule_type == ai_type:
                # 结果一致，置信度加成 10%
                fused_confidence = min(fused_confidence * 1.1, 0.99)
                defect_type = rule_type
                logger.info('融合: 规则与AI一致(%s)，置信度加成', defect_type)
            else:
                # 不一致，取AI结果
                defect_type = ai_type
                logger.info('融合: 规则(%s)与AI(%s)不一致，采用AI结果', rule_type, ai_type)

            severity_level = max(rule_result['severityLevel'], ai_result['severityLevel'])
            bbox = ai_result['bbox']
            source = 'fusion'

        elif rule_type != 'none':
            # 仅规则检测到缺陷
            defect_type = rule_type
            fused_confidence = rule_result['confidenceScore']
            severity_level = rule_result['severityLevel']
            bbox = rule_result['bbox']
            source = 'rule'
            logger.info('融合: 仅规则检测到缺陷(%s)', defect_type)

        elif ai_type != 'none':
            # 仅AI检测到缺陷
            defect_type = ai_type
            fused_confidence = ai_result['confidenceScore']
            severity_level = ai_result['severityLevel']
            bbox = ai_result['bbox']
            source = 'ai'
            logger.info('融合: 仅AI检测到缺陷(%s)', defect_type)

        else:
            # 均未检测到缺陷
            defect_type = 'none'
            fused_confidence = max(rule_result['confidenceScore'], ai_result['confidenceScore'])
            severity_level = 1
            bbox = ai_result['bbox']
            source = 'fusion'
            logger.info('融合: 均未检测到缺陷')

        return {
            'defectType': defect_type,
            'confidenceScore': round(fused_confidence, 2),
            'severityLevel': severity_level,
            'bbox': bbox,
            'source': source
        }


# 单例实例
fusion_detector = FusionDetector()
