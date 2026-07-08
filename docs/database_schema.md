# UPIDS 数据库表结构文档

> Urban Pipeline Inspection and Detection System - Database Schema

## 数据库概述

| 配置项 | 值 |
|--------|-----|
| 数据库类型 | PostgreSQL 14 + PostGIS 3.3 |
| 数据库名 | upids |
| 字符集 | UTF-8 |
| 空间参考系 | EPSG:4326 (WGS84) |

## 表结构总览

| 序号 | 表名 | 说明 | 记录数(示例) |
|------|------|------|-------------|
| 1 | sys_user | 用户账号与权限 | 2 |
| 2 | pipeline | 管线基础与空间数据 | 50+ |
| 3 | inspection_record | 巡检图像记录 | 200+ |
| 4 | detection_task | 异步识别任务 | 100+ |
| 5 | defect | 缺陷标注结果 | 30+ |
| 6 | alert_record | 预警记录 | 10+ |
| 7 | risk_report | 风险报告 | 3+ |
| 8 | operation_log | 操作审计日志 | 动态 |

---

## 1. sys_user（用户表）

### 功能说明
存储系统用户账号信息，支持角色权限管理。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| uk_username | username | UNIQUE |
| idx_role | role | NORMAL |

### ER图关系

```
sys_user
    │
    ├─── 1:N ───> inspection_record (user_id)
    │
    ├─── 1:N ───> risk_report (created_by)
    │
    └─── 1:N ───> operation_log (user_id)
```

---

## 2. pipeline（管线表）

### 功能说明
存储管线的基础属性和空间几何数据，支持GIS空间查询。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_pipeline_geom | geo_coordinates | GIST (空间索引) |
| idx_material | material_type | NORMAL |
| idx_install_time | install_time | NORMAL |

### ER图关系

```
pipeline
    │
    ├─── 1:N ───> inspection_record (pipeline_id)
    │
    ├─── 1:N ───> defect (pipeline_id)
    │
    └─── 1:N ───> alert_record (pipeline_id)
```

---

## 3. inspection_record（巡检记录表）

### 功能说明
存储管线巡检的图像记录和检测结果。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_record_pipeline | pipeline_id | NORMAL |
| idx_inspect_time | inspect_time | NORMAL |
| idx_detection_result | detection_result | NORMAL |

### ER图关系

```
inspection_record
    │
    ├─── N:1 ───> pipeline (pipeline_id)
    │
    ├─── N:1 ───> sys_user (user_id)
    │
    ├─── 1:1 ───> detection_task (record_id)
    │
    └─── 1:N ───> defect (record_id)
```

---

## 4. detection_task（检测任务表）

### 功能说明
存储异步检测任务的状态和执行信息，支持任务重试机制。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_task_status | status | NORMAL |
| idx_task_created | created_at | NORMAL |

### ER图关系

```
detection_task
    │
    └─── N:1 ───> inspection_record (record_id)
```

### 状态流转

```
pending ──> running ──> done
              │
              └──> failed (可重试)
```

---

## 5. defect（缺陷表）

### 功能说明
存储检测到的管线缺陷信息，包含空间位置和严重等级。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_defect_pipeline | pipeline_id | NORMAL |
| idx_defect_type | defect_type | NORMAL |
| idx_severity | severity_level | NORMAL |
| idx_defect_geom | location | GIST (空间索引) |
| idx_detected_at | detected_at | NORMAL |

### ER图关系

```
defect
    │
    ├─── N:1 ───> inspection_record (record_id)
    │
    ├─── N:1 ───> pipeline (pipeline_id)
    │
    └─── 1:N ───> alert_record (defect_id)
```

### 严重等级说明

| 等级 | 说明 | 对应颜色 |
|------|------|----------|
| 1 | 轻微 / 无缺陷 | 黄色 |
| 2 | 轻微裂缝 | 橙色 |
| 3 | 腐蚀 | 橙色 |
| 4 | 断裂风险 | 红色 |
| 5 | 严重缺陷 | 红色 |

---

## 6. alert_record（预警记录表）

### 功能说明
存储缺陷预警记录，支持阈值告警和异常检测。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_alert_pipeline | pipeline_id | NORMAL |
| idx_alert_read | is_read | NORMAL |
| idx_triggered_at | triggered_at | NORMAL |

### ER图关系

```
alert_record
    │
    ├─── N:1 ───> defect (defect_id)
    │
    └─── N:1 ───> pipeline (pipeline_id)
```

---

## 7. risk_report（风险报告表）

### 功能说明
存储风险评估报告，包含统计分析和建议内容。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_report_region | region_code | NORMAL |
| idx_report_time | start_time, end_time | NORMAL |

### ER图关系

```
risk_report
    │
    └─── N:1 ───> sys_user (created_by)
```

---

## 8. operation_log（操作日志表）

### 功能说明
记录系统操作审计日志，支持安全追溯。

### 字段结构

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

### 索引

| 索引名 | 字段 | 类型 |
|--------|------|------|
| idx_log_user | user_id | NORMAL |
| idx_log_module | module | NORMAL |
| idx_log_created | created_at | NORMAL |

### ER图关系

```
operation_log
    │
    └─── N:1 ───> sys_user (user_id)
```

---

## 完整ER关系图

```
sys_user ───────────────────────────────────────────────┐
    │                                                   │
    ├─ 1:N ──> inspection_record ── 1:N ──> defect ────┼── 1:N ──> alert_record
    │                  │                          │         │
    │                  │                          └── 1:N ──┘
    │                  │
    │                  └── 1:1 ──> detection_task
    │
    ├─ 1:N ──> risk_report
    │
    └─ 1:N ──> operation_log

pipeline ───────────────────────────────────────────────┐
    │                                                   │
    ├─ 1:N ──> inspection_record ── 1:N ──> defect ────┼── 1:N ──> alert_record
    │                  │                          │         │
    │                  │                          └── 1:N ──┘
    │                  │
    │                  └── 1:1 ──> detection_task
    │
    └────────────────────────────────────────────────────┘
```

---

## 数据字典汇总

### 枚举值定义

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

### 空间数据字段

| 表名 | 字段名 | 类型 | SRID | 用途 |
|------|--------|------|------|------|
| pipeline | geo_coordinates | GEOMETRY(LineString) | 4326 | 管线路径 |
| defect | location | GEOMETRY(Point) | 4326 | 缺陷位置 |

---

## 初始化数据

### 默认用户

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin | admin | 系统管理员 |
| user | admin | user | 普通用户 |

---

## 数据库连接信息

```yaml
# 开发环境
host: localhost
port: 5432
database: upids
username: upids_admin
password: upids_admin_123
schema: public
```

---

*文档生成时间: 2026-07-07*