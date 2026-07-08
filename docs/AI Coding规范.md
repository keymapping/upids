# AI Coding 规范

> 版本：v1.0  
> 适用对象：AI Agent 编码  
> 关联文档：[产品文档](./产品文档.md) · [接口文档](./接口文档.md) · [协作开发规范](./协作开发规范.md)

---

## 一、文档使用说明（AI Coding 必读）

### 1.1 文档优先级

| 优先级 | 文档                      | 用途                             |
| ------ | ------------------------- | -------------------------------- |
| P0     | [产品文档](./产品文档.md) | 功能范围、架构、数据库、验收标准 |
| P0     | [接口文档](./接口文档.md) | 接口路径、参数、响应、错误码     |
| P0     | 本文档                    | 工程结构、编码约定、实现顺序     |

**冲突处理**：数据库选型以产品文档为准（PostgreSQL + PostGIS）；接口字段以接口文档为准。

### 1.2 Agent 开发原则

1. **先读后写**：编码前阅读产品文档 §3 功能模块、§6 数据库、接口文档对应模块。
2. **最小可用闭环**：优先打通「登录 → 导入管线 → 上传图像 → 异步识别 → GIS 展示 → 预警」主链路，再补管理功能。
3. **禁止静态页**：所有页面必须调用真实后端 API，不得 hardcode 假数据作为最终交付。
4. **保持一致**：命名、响应格式、错误码全项目统一，见本文档 §四、§五。
5. **可追溯**：关键操作写 `operation_log`；识别任务状态必须可查询。

### 1.3 推荐实现顺序

```
Phase 1 基础框架
  ├── 初始化 backend / frontend / python-service 工程
  ├── 数据库 DDL + 初始 admin 账号
  └── 统一响应体、全局异常、JWT 鉴权

Phase 2 用户与数据接入
  ├── 登录 / 用户管理
  ├── 管线 GIS 导入（GeoJSON + Excel）
  └── 模拟数据生成器

Phase 3 核心业务
  ├── 图像上传 + detection_task 入队
  ├── 队列消费 + Python Flask 调用
  ├── defect 回写 + 预警触发
  └── GIS / 缺陷 / 统计 API

Phase 4 前端页面
  ├── 登录 + 路由守卫
  ├── 数据导入页 + 任务列表
  ├── GIS 地图（Leaflet）+ ECharts
  └── 预警中心 + 风险报告

Phase 5 完善
  ├── Redis 缓存 GIS 查询
  ├── Swagger 文档
  ├── 操作日志查询
  └── WebSocket 预警推送（加分项）
```

---

## 二、项目工程结构（强制）

### 2.1 monorepo 目录

```
UPIDS/
├── backend/                 # Spring Boot 后端
├── frontend/                # Vue3 前端
├── python-service/          # Flask 图像识别服务
├── sql/                     # DDL、初始化数据、模拟数据脚本
├── docs/                    # 产品/接口/开发文档
└── docker-compose.yml       # 可选：一键启动 PG/Redis/服务
```

### 2.2 后端结构（backend/）

```
backend/src/main/java/com/upids/
├── UpidsApplication.java
├── config/                  # Security, Redis, Swagger, Cors
├── controller/              # 仅做参数校验 + 调用 Service
├── service/
│   ├── impl/
│   └── ...                  # 业务接口
├── mapper/                  # MyBatis-Plus Mapper
├── entity/                  # 与数据库表一一对应
├── dto/
│   ├── request/             # 入参 DTO
│   └── response/            # 出参 DTO / VO
├── common/
│   ├── result/              # Result<T> 统一响应
│   ├── exception/           # 业务异常 + GlobalExceptionHandler
│   ├── enums/               # 枚举：Role, TaskStatus, DefectType...
│   └── util/                # JwtUtil, GeoUtil...
├── queue/                   # DetectionTaskConsumer
├── client/                  # PythonDetectClient (RestTemplate/WebClient)
└── aspect/                  # OperationLogAspect 操作日志切面
```

### 2.3 前端结构（frontend/）

```
frontend/src/
├── api/                     # 按模块拆分：auth.ts, pipeline.ts, defect.ts...
├── views/
│   ├── login/
│   ├── dashboard/
│   ├── pipeline/            # 管线管理、导入
│   ├── inspection/          # 图像上传
│   ├── gis/                 # GIS 地图
│   ├── statistics/          # ECharts 统计
│   ├── alert/               # 预警中心
│   ├── report/              # 风险报告
│   ├── task/                # 识别任务
│   └── system/              # 用户管理、日志（admin）
├── components/
│   ├── GisMap.vue           # Leaflet 封装
│   ├── DefectChart.vue      # ECharts 封装
│   └── FileUpload.vue
├── router/index.ts          # 路由 + 权限 meta
├── stores/                  # Pinia: user, alert
├── utils/request.ts         # Axios 拦截器
└── types/                   # TS 类型与接口文档对齐
```

### 2.4 Python 服务结构（python-service/）

```
python-service/
├── app.py                   # Flask 入口
├── routes/detect.py         # POST /detect, GET /health
├── services/
│   ├── rule_detector.py     # 规则检测
│   └── ai_detector.py       # 模型/模拟 AI 检测
├── models/                  # 可选：YOLOv8 权重
└── requirements.txt
```

---

## 三、系统必须包含的工程模块（强制）

每个项目必须包含以下 6 大模块（详见产品文档 §3）：

| 模块       | 本课题实现要点                                              |
| ---------- | ----------------------------------------------------------- |
| 用户与权限 | JWT + RBAC；角色 `admin` / `user`                           |
| 数据接入   | GeoJSON/Excel 管线导入；图像/ZIP 上传；`/api/mock/generate` |
| 数据存储   | PostgreSQL + PostGIS；8 张核心表；Redis 缓存 GIS            |
| 业务处理   | 坐标 WGS84、拓扑、规则+AI 双通道、异步队列                  |
| 可视化     | Leaflet 管线/缺陷图层；ECharts ≥3 种图表                    |
| 预警/决策  | severity≥阈值 + 异常检测；`alert_record` + 页面通知         |

---

## 四、编码规范

### 4.1 命名约定

| 类型       | 规范                  | 示例                            |
| ---------- | --------------------- | ------------------------------- |
| 数据库表   | snake_case            | `inspection_record`             |
| 数据库字段 | snake_case            | `pipeline_id`, `created_at`     |
| Java 类    | PascalCase            | `PipelineController`            |
| Java 字段  | camelCase             | `pipelineId`                    |
| API 路径   | kebab 或复数资源      | `/api/pipelines`, `/api/alerts` |
| JSON 字段  | camelCase             | `pipelineId`, `severityLevel`   |
| 枚举值     | 小写 snake 或固定常量 | `pending`, `crack`, `admin`     |
| Vue 组件   | PascalCase 文件名     | `GisMap.vue`                    |
| 前端变量   | camelCase             | `defectList`                    |

### 4.2 统一响应体

所有 Spring Boot API 必须使用统一包装（与接口文档一致）：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1719667200000
}
```

分页响应 `data` 结构：

```json
{
  "list": [],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

### 4.3 统一错误码

| code | 含义       | 场景                    |
| ---- | ---------- | ----------------------- |
| 200  | 成功       | —                       |
| 400  | 参数错误   | 校验失败、非法枚举      |
| 401  | 未认证     | Token 缺失或过期        |
| 403  | 无权限     | 普通用户访问 admin 接口 |
| 404  | 资源不存在 | ID 无效                 |
| 409  | 冲突       | 用户名重复              |
| 500  | 服务器错误 | 未捕获异常              |
| 503  | 依赖不可用 | Python 识别服务超时     |

### 4.4 鉴权规范

- 登录成功返回 JWT，`Authorization: Bearer <token>`。
- 白名单：`POST /api/auth/login`、Swagger、`GET /api/health`。
- 管理员专用接口在 Controller 加 `@PreAuthorize("hasRole('ADMIN')")` 或自定义注解。
- 前端路由 `meta.roles: ['admin']`，Axios 401 跳转登录页。

### 4.5 日志规范

- 使用 SLF4J；业务关键节点 `INFO`，异常 `ERROR`。
- 以下操作**必须**写 `operation_log`（AOP 或 Service 显式调用）：
  - 登录/登出
  - GIS/图像导入
  - 模拟数据生成
  - 报告生成
  - 预警规则变更
  - 用户管理

### 4.6 数据库规范

- ORM：MyBatis-Plus；Entity 与产品文档 §6.4 表结构一致。
- 时间字段：Java 用 `LocalDateTime`；JSON 格式 `yyyy-MM-dd HH:mm:ss`。
- 空间字段：PostGIS `GEOMETRY`；Java 可用 WKT 字符串传输，Service 层转换。
- 软删除：本课题仅 `sys_user.status`、`pipeline.status`，不做全局 deleted 字段。
- **DDL 文件**：`sql/schema.sql` + `sql/init_data.sql`，Agent 必须生成可执行脚本。

### 4.7 异步任务规范

```
创建 inspection_record → 同事务创建 detection_task(status=pending)
→ 队列消费改为 running → 调 Python → 写 defect → task=done
→ 失败：task=failed, error_message, retry_count+1（最多 3 次）
```

- 禁止同步阻塞等待 Python 识别完成再返回上传接口。
- 上传接口立即返回 `{ recordId, taskId }`。

### 4.8 Python 服务规范

- 必须提供 `GET /health` 和 `POST /detect`（见接口文档 §12）。
- 识别超时：30s；Java 侧配置 connect/read timeout。
- 开发阶段可用规则+随机模拟；接口契约不得变。

### 4.9 前端规范

- Vue3 Composition API + `<script setup lang="ts">`。
- UI：Element Plus；地图：Leaflet；图表：ECharts。
- API 调用统一走 `src/api/*`，禁止在组件内硬编码 URL。
- GIS 坐标系：WGS84（EPSG:4326），Leaflet 默认 `[lat, lng]`。

---

## 五、技术栈与版本

### 5.1 统一标准

| 层次   | 技术                 | 版本建议       |
| ------ | -------------------- | -------------- |
| 后端   | Spring Boot          | 2.7.x 或 3.2.x |
| ORM    | MyBatis-Plus         | 3.5.x          |
| 文档   | Knife4j / Swagger    | 3.x            |
| 数据库 | PostgreSQL + PostGIS | 14+ / 3.x      |
| 缓存   | Redis                | 6.x+           |
| 前端   | Vue3 + Vite          | 3.x / 5.x      |
| UI     | Element Plus         | 2.x            |
| 地图   | Leaflet              | 1.9.x          |
| 图表   | ECharts              | 5.x            |
| HTTP   | Axios                | 1.x            |
| 算法   | Python + Flask       | 3.x            |

> 作业统一要求写 MySQL，本课题以 **PostgreSQL + PostGIS** 为准（产品文档 §5、§6）。

### 5.2 本地开发环境变量

**backend `application-dev.yml` 关键项：**

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/upids
    username: upids
    password: upids123
  redis:
    host: localhost
    port: 6379
upids:
  jwt:
    secret: ${JWT_SECRET:change-me-in-production}
    expire-hours: 24
  python:
    base-url: http://localhost:5000
  upload:
    path: ./uploads
  alert:
    severity-threshold: 4
```

**frontend `.env.development`：**

```
VITE_API_BASE_URL=http://localhost:8080
```

---

## 六、数据库设计要求（强制）

- 至少 5 张表；本课题 **8 张表**（见产品文档 §6.2）。
- 必须包含：用户表、数据采集表、业务分析表、结果输出表、日志表。
- 每张表：主键 + 时间字段 + 必要索引。
- 完整 DDL 见 `sql/schema.sql`，Agent 建库时必须与产品文档 §6.4 字段一致。

---

## 七、系统功能复杂度要求

### 7.1 数据规模

- 单系统模拟数据 **≥ 10,000 条**（管线 + 检测记录合计）。
- 提供 `POST /api/mock/generate` 一键生成。
- 支持 GeoJSON / Excel / ZIP 批量导入。

### 7.2 分析逻辑（至少满足 3 项）

| 类型     | 本课题实现                                           |
| -------- | ---------------------------------------------------- |
| 分类模型 | Python/规则缺陷分类：crack/corrosion/fracture        |
| 异常检测 | 同 pipeline 7 日内 defect 数突增触发 anomaly 预警    |
| 多源融合 | GIS 管线属性 + 图像 AI 结果写入 defect.source=fusion |

---

## 八、本课题技术约束摘要

| 项       | 约束                                            |
| -------- | ----------------------------------------------- |
| 架构     | Controller → Service → Mapper                   |
| 异步     | Kafka 或 **内存 BlockingQueue**（开发可用后者） |
| 任务状态 | pending / running / done / failed               |
| 坐标     | 统一 WGS84                                      |
| 缺陷类型 | crack / corrosion / fracture                    |
| 严重等级 | 1–5，≥4 触发 threshold 预警                     |
| 文件     | 本地 `./uploads`，按日期分子目录                |

---

## 九、Redis 缓存策略

| Key 模式                   | TTL   | 说明             |
| -------------------------- | ----- | ---------------- |
| `gis:pipelines:{bboxHash}` | 5min  | 管线空间范围查询 |
| `gis:defects:{bboxHash}`   | 3min  | 缺陷点范围查询   |
| `stat:overview:{date}`     | 10min | 统计概览         |

- 管线/缺陷**写入/导入**后删除相关 Key 或按前缀 `gis:*` 批量失效。

---

## 十、安全要求

- 密码 BCrypt 加密，禁止明文存储。
- 文件上传：限制类型 `jpg,jpeg,png,bmp`；单文件 ≤ 20MB；ZIP ≤ 200MB。
- 路径穿越防护：存储文件名 UUID 化，禁止用户控制绝对路径。
- SQL 注入：MyBatis 参数化；禁止拼接 SQL。
- CORS：开发环境允许 `localhost:5173`。

---

## 十一、测试要求（Agent 自检）

### 11.1 接口测试

- 每个 Controller 至少手动/Postman 验证 happy path。
- 鉴权：无 Token 401；user 访问 admin 403。

### 11.2 主链路测试

1. admin 登录 → Token 有效
2. 导入 GeoJSON → pipeline 表有数据
3. 上传图像 → 返回 taskId → 轮询至 done
4. GET defects → 有 defect 记录
5. severity≥4 → alert_record 有记录
6. GIS 接口返回 GeoJSON FeatureCollection

### 11.3 前端测试

- 登录守卫、GIS 图层加载、ECharts 有数据、预警列表可标记已读。

---

## 十二、Agent 编码检查清单

开发完成后逐项确认：

- [ ] 工程结构符合 §二
- [ ] 6 大模块均已实现（§三）
- [ ] 8 张表 + DDL 脚本（§六）
- [ ] 接口与 [接口文档](./接口文档.md) 一致
- [ ] 统一响应体 + 错误码（§4.2、§4.3）
- [ ] JWT 鉴权 + 角色权限
- [ ] 异步识别 + 任务状态查询
- [ ] Python Flask `/detect` 可调用
- [ ] 模拟数据 ≥1 万条
- [ ] Leaflet GIS + ECharts ≥3 图
- [ ] 预警页面 + operation_log
- [ ] Swagger/Knife4j 可访问
- [ ] README 含启动步骤

---

## 十三、参考链接

| 资源       | 路径                              |
| ---------- | --------------------------------- |
| 产品需求   | [docs/产品文档.md](./产品文档.md) |
| 接口明细   | [docs/接口文档.md](./接口文档.md) |
| 数据库 DDL | `sql/schema.sql`（待生成）        |
