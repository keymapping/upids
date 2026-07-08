import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import psycopg2
import json
from config import config
from utils.logger import setup_logger

logger = setup_logger('db_client', config.LOG_LEVEL)


class DatabaseClient:
    """数据库客户端 - 用于从PostgreSQL获取管线数据"""

    def __init__(self):
        self.connection = None
        self.connect()

    def connect(self):
        """建立数据库连接"""
        try:
            if self.connection and not self.connection.closed:
                return

            self.connection = psycopg2.connect(
                host=config.DB_HOST,
                port=config.DB_PORT,
                database=config.DB_NAME,
                user=config.DB_USER,
                password=config.DB_PASSWORD
            )
            logger.info('数据库连接成功')
        except Exception as e:
            logger.error('数据库连接失败: %s', str(e))
            self.connection = None

    def get_pipeline_data(self, pipeline_id):
        """获取管线数据"""
        if not self.connection:
            self.connect()
            if not self.connection:
                logger.warning('数据库不可用，使用模拟管线数据')
                return self._mock_pipeline_data(pipeline_id)

        try:
            cursor = self.connection.cursor()
            query = """
                SELECT pipeline_id, pipeline_name, material_type, diameter, 
                       region_code, install_time, status, geom
                FROM pipeline
                WHERE pipeline_id = %s
            """
            cursor.execute(query, (pipeline_id,))
            row = cursor.fetchone()

            if row:
                return {
                    'pipelineId': row[0],
                    'pipelineName': row[1],
                    'material': row[2],
                    'diameter': row[3],
                    'regionCode': row[4],
                    'installYear': row[5].year if row[5] else 2000,
                    'status': row[6],
                    'geom': row[7]
                }
            else:
                logger.warning('未找到管线: %s', pipeline_id)
                return self._mock_pipeline_data(pipeline_id)

        except Exception as e:
            logger.error('获取管线数据失败: %s', str(e))
            return self._mock_pipeline_data(pipeline_id)
        finally:
            if 'cursor' in locals():
                cursor.close()

    def get_pipeline_location(self, pipeline_id):
        """获取管线位置信息"""
        if not self.connection:
            self.connect()
            if not self.connection:
                logger.warning('数据库不可用，使用模拟位置数据')
                return self._mock_location(pipeline_id)

        try:
            cursor = self.connection.cursor()
            query = """
                SELECT ST_X(ST_Centroid(geom)) AS lng, ST_Y(ST_Centroid(geom)) AS lat
                FROM pipeline
                WHERE pipeline_id = %s
            """
            cursor.execute(query, (pipeline_id,))
            row = cursor.fetchone()

            if row and row[0] and row[1]:
                return {
                    'lng': round(float(row[0]), 6),
                    'lat': round(float(row[1]), 6)
                }
            else:
                logger.warning('未找到管线位置: %s', pipeline_id)
                return self._mock_location(pipeline_id)

        except Exception as e:
            logger.error('获取管线位置失败: %s', str(e))
            return self._mock_location(pipeline_id)
        finally:
            if 'cursor' in locals():
                cursor.close()

    def get_inspection_record(self, record_id):
        """获取检测记录"""
        if not self.connection:
            self.connect()
            if not self.connection:
                logger.warning('数据库不可用，使用模拟检测记录')
                return None

        try:
            cursor = self.connection.cursor()
            query = """
                SELECT record_id, pipeline_id, image_path, image_name, 
                       detection_result, confidence_score, inspect_time
                FROM inspection_record
                WHERE record_id = %s
            """
            cursor.execute(query, (record_id,))
            row = cursor.fetchone()

            if row:
                return {
                    'recordId': row[0],
                    'pipelineId': row[1],
                    'imagePath': row[2],
                    'imageName': row[3],
                    'detectionResult': row[4],
                    'confidenceScore': row[5],
                    'inspectTime': row[6]
                }
            else:
                logger.warning('未找到检测记录: %s', record_id)
                return None

        except Exception as e:
            logger.error('获取检测记录失败: %s', str(e))
            return None
        finally:
            if 'cursor' in locals():
                cursor.close()

    def save_detection_result(self, record_id, result):
        """保存检测结果到数据库"""
        if not self.connection:
            self.connect()
            if not self.connection:
                logger.warning('数据库不可用，无法保存检测结果')
                return False

        try:
            cursor = self.connection.cursor()
            query = """
                UPDATE inspection_record
                SET detection_result = %s, confidence_score = %s
                WHERE record_id = %s
            """
            cursor.execute(query, (
                result.get('defectType'),
                result.get('confidenceScore'),
                record_id
            ))
            self.connection.commit()
            logger.info('检测结果已保存: recordId=%s, defect=%s', record_id, result.get('defectType'))
            return True

        except Exception as e:
            logger.error('保存检测结果失败: %s', str(e))
            if self.connection:
                self.connection.rollback()
            return False
        finally:
            if 'cursor' in locals():
                cursor.close()

    def _mock_pipeline_data(self, pipeline_id):
        """模拟管线数据（fallback）"""
        import random
        return {
            'pipelineId': pipeline_id,
            'pipelineName': f'管线-{pipeline_id}',
            'material': random.choice(['steel', 'cast_iron', 'concrete', 'pe']),
            'diameter': random.randint(100, 600),
            'regionCode': 'REGION_001',
            'installYear': random.randint(1970, 2020),
            'status': 1,
            'geom': None
        }

    def _mock_location(self, pipeline_id):
        """模拟位置数据（fallback）"""
        import random
        return {
            'lng': round(random.uniform(116.0, 117.0), 6),
            'lat': round(random.uniform(39.0, 40.0), 6)
        }

    def close(self):
        """关闭数据库连接"""
        if self.connection and not self.connection.closed:
            self.connection.close()
            logger.info('数据库连接已关闭')


db_client = DatabaseClient()