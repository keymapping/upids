# UPIDS 数据库数据字典

## 1. sys_user（用户表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGSERIAL | PRIMARY KEY | - | 用户ID，自增主键 |
| username | VARCHAR(50) | UNIQUE NOT NULL | - | 用户名，唯一标识 |
| password | VARCHAR(255) | NOT NULL | - | 密码（BCrypt加密） |
| real_name | VARCHAR(50) | | NULL | 真实姓名 |
| role | VARCHAR(20) | NOT NULL | - | 角色：admin（管理员）/ user（普通用户） |
| status | SMALLINT | | 1 | 状态：0（禁用）/ 1（启用） |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | | NULL | 更新时间 |

## 2. pipeline（管线表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| pipeline_id | VARCHAR(64) | PRIMARY KEY | - | 管线唯一标识 |
| pipeline_name | VARCHAR(100) | | NULL | 管线名称 |
| geo_coordinates | GEOMETRY(LineString, 4326) | NOT NULL | - | 管线空间坐标（WGS84坐标系） |
| material_type | VARCHAR(50) | | NULL | 材质：PE / PVC / 铸铁 / 混凝土 / 钢管 / 铜管 |
| diameter | NUMERIC(8,2) | | NULL | 管径（毫米） |
| install_time | DATE | | NULL | 安装时间 |
| region_code | VARCHAR(20) | | NULL | 区域编码：如 BJ001, SH001 |
| status | SMALLINT | | 1 | 状态：0（停用）/ 1（正常） |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | | NULL | 更新时间 |

## 3. inspection_record（巡检记录表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| record_id | BIGSERIAL | PRIMARY KEY | - | 记录ID，自增主键 |
| pipeline_id | VARCHAR(64) | NOT NULL, FOREIGN KEY | - | 关联管线ID |
| user_id | BIGINT | FOREIGN KEY | NULL | 操作人ID |
| image_path | VARCHAR(500) | NOT NULL | - | 图片存储路径 |
| image_name | VARCHAR(200) | | NULL | 图片原始名称 |
| detection_result | VARCHAR(20) | | NULL | 检测结果：none / crack / corrosion / fracture |
| confidence_score | NUMERIC(5,4) | | NULL | 置信度（0-1） |
| inspect_time | TIMESTAMP | NOT NULL | - | 检测时间 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 4. detection_task（检测任务表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| task_id | BIGSERIAL | PRIMARY KEY | - | 任务ID，自增主键 |
| record_id | BIGINT | UNIQUE, FOREIGN KEY | NULL | 关联检测记录ID（一对一） |
| status | VARCHAR(20) | NOT NULL | - | 状态：pending / running / done / failed |
| retry_count | SMALLINT | | 0 | 重试次数（最大3次） |
| error_message | TEXT | | NULL | 错误信息 |
| started_at | TIMESTAMP | | NULL | 任务开始时间 |
| finished_at | TIMESTAMP | | NULL | 任务结束时间 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 5. defect（缺陷表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| defect_id | BIGSERIAL | PRIMARY KEY | - | 缺陷ID，自增主键 |
| record_id | BIGINT | FOREIGN KEY | NULL | 关联检测记录ID |
| pipeline_id | VARCHAR(64) | NOT NULL, FOREIGN KEY | - | 关联管线ID |
| defect_type | VARCHAR(20) | NOT NULL | - | 缺陷类型：none / crack / corrosion / fracture |
| severity_level | SMALLINT | NOT NULL | - | 严重等级：1-5（越高越严重） |
| location | GEOMETRY(Point, 4326) | | NULL | 缺陷位置坐标（WGS84） |
| bbox | VARCHAR(100) | | NULL | 图像边界框：[x,y,w,h] |
| confidence_score | NUMERIC(5,4) | | NULL | 置信度（0-1） |
| source | VARCHAR(20) | | NULL | 检测来源：rule / ai / fusion |
| detected_at | TIMESTAMP | NOT NULL | - | 检测时间 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 6. alert_record（预警记录表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| alert_id | BIGSERIAL | PRIMARY KEY | - | 预警ID，自增主键 |
| defect_id | BIGINT | FOREIGN KEY | NULL | 关联缺陷ID |
| pipeline_id | VARCHAR(64) | FOREIGN KEY | NULL | 关联管线ID |
| alert_level | SMALLINT | NOT NULL | - | 预警等级：1-5 |
| alert_type | VARCHAR(30) | NOT NULL | - | 预警类型：threshold / anomaly |
| alert_message | VARCHAR(500) | | NULL | 预警消息 |
| is_read | BOOLEAN | | FALSE | 是否已读 |
| triggered_at | TIMESTAMP | NOT NULL | - | 触发时间 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 7. risk_report（风险报告表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| report_id | BIGSERIAL | PRIMARY KEY | - | 报告ID，自增主键 |
| report_title | VARCHAR(200) | NOT NULL | - | 报告标题 |
| region_code | VARCHAR(20) | | NULL | 区域编码 |
| start_time | TIMESTAMP | | NULL | 统计开始时间 |
| end_time | TIMESTAMP | | NULL | 统计结束时间 |
| total_defects | INT | | NULL | 缺陷总数 |
| high_risk_count | INT | | NULL | 高风险缺陷数（等级≥4） |
| report_content | JSONB | | NULL | 报告内容（JSON格式） |
| file_path | VARCHAR(500) | | NULL | 导出文件路径 |
| created_by | BIGINT | FOREIGN KEY | NULL | 创建人ID |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 8. operation_log（操作日志表）

| 字段名 | 类型 | 约束 | 默认值 | 说明 |
|--------|------|------|--------|------|
| log_id | BIGSERIAL | PRIMARY KEY | - | 日志ID，自增主键 |
| user_id | BIGINT | FOREIGN KEY | NULL | 操作用户ID |
| username | VARCHAR(50) | | NULL | 用户名 |
| module | VARCHAR(50) | NOT NULL | - | 操作模块 |
| operation | VARCHAR(100) | NOT NULL | - | 操作内容 |
| request_uri | VARCHAR(200) | | NULL | 请求URI |
| request_params | TEXT | | NULL | 请求参数（JSON） |
| result | VARCHAR(20) | | NULL | 操作结果：success / fail |
| error_msg | TEXT | | NULL | 错误信息 |
| ip_address | VARCHAR(50) | | NULL | 客户端IP地址 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

## 枚举值定义

| 表名 | 字段名 | 枚举值 | 说明 |
|------|--------|--------|------|
| sys_user | role | admin, user | 角色 |
| sys_user | status | 0, 1 | 禁用, 启用 |
| pipeline | status | 0, 1 | 停用, 正常 |
| detection_task | status | pending, running, done, failed | 任务状态 |
| defect | defect_type | none, crack, corrosion, fracture | 缺陷类型 |
| defect | source | rule, ai, fusion | 检测来源 |
| alert_record | alert_type | threshold, anomaly | 预警类型 |
| operation_log | result | success, fail | 操作结果 |

## 外键关系

| 子表 | 子表字段 | 父表 | 父表字段 | 关系类型 |
|------|----------|------|----------|----------|
| inspection_record | pipeline_id | pipeline | pipeline_id | N:1 |
| inspection_record | user_id | sys_user | id | N:1 |
| detection_task | record_id | inspection_record | record_id | N:1 |
| defect | record_id | inspection_record | record_id | N:1 |
| defect | pipeline_id | pipeline | pipeline_id | N:1 |
| alert_record | defect_id | defect | defect_id | N:1 |
| alert_record | pipeline_id | pipeline | pipeline_id | N:1 |
| risk_report | created_by | sys_user | id | N:1 |
| operation_log | user_id | sys_user | id | N:1 |