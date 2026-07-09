# 水下管线检测与缺陷识别系统 (UPIDS)

> Underwater Pipeline Inspection and Defect Detection System

## 项目简介

本系统用于黄河水域及沿岸水下管线的运行状态监测，实现管线结构数据的数字化管理与缺陷自动识别。系统采用前后端分离架构，结合 GIS 空间建模、图像识别融合和缺陷预警决策技术。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | PostgreSQL + PostGIS | 14+ / 3.x |
| 缓存 | Redis | 7.x |
| 算法服务 | Python + Flask | 3.x |
| 前端框架 | Vue3 + Vite | 3.x / 5.x |
| UI组件 | Element Plus | 2.x |
| GIS地图 | Leaflet | 1.9.x |
| 数据图表 | ECharts | 5.x |
| 接口文档 | Knife4j | 4.x |

## 目录结构

```
UPIDS/
├── backend/                 # Spring Boot 后端
│   └── src/main/java/com/upids/
│       ├── controller/      # REST接口
│       ├── service/         # 业务逻辑
│       ├── mapper/          # MyBatis-Plus DAO
│       ├── entity/          # 实体类（8张表）
│       ├── dto/             # 请求/响应对象
│       ├── config/          # 安全、Redis、Swagger配置
│       ├── common/          # 统一响应、异常、枚举
│       ├── queue/           # 任务队列消费
│       ├── client/          # Python服务客户端
│       └── aspect/          # 操作日志切面
├── frontend/                # Vue3 前端
│   └── src/
│       ├── api/             # Axios接口封装
│       ├── views/           # 页面组件
│       ├── components/      # 公共组件（GIS、图表、上传）
│       ├── router/          # 路由与权限守卫
│       ├── stores/          # Pinia状态管理
│       ├── utils/           # 工具函数
│       └── types/           # TypeScript类型定义
├── python-service/          # Flask 图像识别服务
│   ├── app.py               # Flask入口
│   ├── routes/              # 路由（/detect, /health）
│   └── services/            # 检测逻辑（规则+AI）
├── sql/                     # 数据库脚本
│   ├── init.sql             # PostGIS扩展+账号创建
│   ├── schema.sql           # 表结构DDL
│   ├── init_data.sql        # 初始数据
│   └── migrations/          # 版本化迁移脚本
├── docs/                    # 产品/接口/开发文档
│   ├── 产品文档.md
│   ├── 接口文档.md
│   ├── AI Coding规范.md
│   └── 协作开发规范.md
│   └── 数据库协作解决方案.md
├── docker-compose.yml       # Docker容器编排
├── .gitignore
└── README.md
```

## 本地启动步骤

### 1. 启动数据库服务

```bash
# 启动 PostgreSQL + Redis + pgAdmin
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

等待 PostgreSQL 健康检查通过后，执行数据库初始化：

```bash
# 方式一：通过 pgAdmin 手动执行
# 打开 http://localhost:5050，登录 admin@upids.com / admin123
# 连接数据库后执行 sql/init.sql, sql/schema.sql, sql/init_data.sql

# 方式二：通过命令行执行
docker exec -i upids-postgres psql -U upids_admin -d upids -f /docker-entrypoint-initdb.d/init.sql
```

**数据库连接信息：**

| 配置项 | 值 |
|--------|-----|
| Host | localhost (或数据库服务器IP) |
| Port | 5432 |
| Database | upids |
| Username | upids |
| Password | upids123 |

### 2. 启动后端服务

```bash
cd backend

# 安装依赖
mvn clean install

# 启动开发环境
mvn spring-boot:run

# 或使用 IDE 运行 UpidsApplication.java
```

后端启动后访问：
- API: http://localhost:8080
- Swagger文档: http://localhost:8080/doc.html

### 3. 启动前端服务

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：http://localhost:3000

### 4. 启动Python识别服务

```bash
cd python-service

# 安装依赖
pip install -r requirements.txt

# 启动服务
python app.py
```

Python服务启动后访问：http://localhost:5000

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin |
| 普通用户 | user | admin |

## 环境变量说明

### backend application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:upids}
    username: ${DB_USER:upids}
    password: ${DB_PASSWORD:upids123}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

upids:
  jwt:
    secret: ${JWT_SECRET:change-me-in-production}
    expire-hours: ${JWT_EXPIRE:24}
  python:
    base-url: ${PYTHON_URL:http://localhost:5000}
  upload:
    path: ${UPLOAD_PATH:./uploads}
  alert:
    severity-threshold: ${ALERT_THRESHOLD:4}
```

### frontend .env.development

```
VITE_API_BASE_URL=http://localhost:8080
```

## 功能模块

| 模块 | 功能 |
|------|------|
| 用户与权限 | JWT登录、角色权限（admin/user） |
| 数据接入 | GIS数据导入（GeoJSON/Excel）、图像上传、模拟数据生成 |
| 数据存储 | PostgreSQL+PostGIS空间数据、Redis缓存 |
| 业务处理 | 坐标转换、规则检测、AI图像识别、异步任务队列 |
| 可视化 | Leaflet GIS地图、ECharts统计图表 |
| 预警决策 | 阈值判断、异常检测、页面告警 |

## AI模型与检测流程

### 模型架构

系统采用**双通道融合检测**架构，结合规则检测和AI图像识别：

#### 1. CNN图像识别模型

| 配置项 | 说明 |
|--------|------|
| 模型类型 | 自定义CNN（3卷积层 + 2全连接层） |
| 输入尺寸 | 3 x 128 x 128 RGB图像 |
| 输出类别 | 4类：none / crack(裂缝) / corrosion(腐蚀) / fracture(断裂) |
| 特征提取 | 32 → 64 → 128 通道 |
| 正则化 | BatchNorm + Dropout(0.5) |
| 优化器 | Adam |

**网络结构：**
```
输入(3×128×128) → Conv1(32) → BN → ReLU → MaxPool(2×2)
              → Conv2(64) → BN → ReLU → MaxPool(2×2)
              → Conv3(128) → BN → ReLU → MaxPool(2×2)
              → Flatten → FC1(256) → Dropout → FC2(4) → Softmax
```

#### 2. 规则检测器

基于管线属性进行风险评估，权重配置：

| 属性 | 权重 | 风险判定 |
|------|------|----------|
| 管径 (diameter) | 30% | <150mm(0.8) / 150-300mm(0.5) / >300mm(0.2) |
| 材质 (material) | 30% | 铸铁(0.9) > 钢材(0.7) > 混凝土(0.5) > PE(0.2) |
| 年限 (age) | 40% | >40年(0.9) / 30-40年(0.7) / 20-30年(0.5) |

#### 3. 双通道融合策略

| 情况 | 策略 |
|------|------|
| 两者一致 | 置信度加成10% |
| 两者不一致 | 采用AI结果 |
| 严重等级 | 取两者较高值 |
| 权重分配 | 规则40% + AI 60% |

### 检测流程

```
图像上传 → 创建检测任务(PENDING) → 任务队列消费
    ↓
规则检测(管线属性) ←→ AI检测(CNN图像)
    ↓
双通道融合 → 保存缺陷记录 → 触发预警判断 → 更新任务状态(DONE/FAILED)
```

### 严重等级判定

| 等级 | 缺陷类型 | 说明 |
|------|----------|------|
| 1 | none | 无缺陷 |
| 2 | crack | 轻微裂缝 |
| 3 | corrosion | 腐蚀 |
| 4 | fracture | 断裂风险 |
| 5 | - | 高置信度(>0.95)缺陷加成 |

## API接口

详细接口文档请参考：
- [接口文档.md](./docs/接口文档.md)
- Swagger UI: http://localhost:8080/doc.html

## 开发规范

- [AI Coding规范.md](./docs/AI%20Coding规范.md) - AI Agent 编码规范
- [协作开发规范.md](./docs/协作开发规范.md) - 团队协作规范

## 数据库设计

系统包含8张核心表：

| 表名 | 说明 |
|------|------|
| sys_user | 用户账号与权限 |
| pipeline | 管线基础与空间数据 |
| inspection_record | 巡检图像记录 |
| detection_task | 异步识别任务 |
| defect | 缺陷标注结果 |
| alert_record | 预警记录 |
| risk_report | 风险报告 |
| operation_log | 操作审计日志 |

详细表结构请参考：
- [产品文档.md §6](./docs/产品文档.md)
- [sql/schema.sql](./sql/schema.sql)

## 常用命令

```bash
# Docker
docker-compose up -d          # 启动所有服务
docker-compose down           # 停止所有服务
docker-compose logs -f        # 查看日志
docker-compose ps             # 查看状态

# Backend
mvn clean install             # 编译打包
mvn spring-boot:run           # 启动服务

# Frontend
npm install                   # 安装依赖
npm run dev                   # 启动开发服务器
npm run build                 # 生产构建

# Python
pip install -r requirements.txt  # 安装依赖
python app.py                    # 启动服务
```

## 许可证

本项目为课程作业项目，仅供学习演示使用。