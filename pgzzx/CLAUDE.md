# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **PGZC (试验任务综合评估系统)** — an aerospace equipment evaluation platform built on **RuoYi-Cloud 3.6.0** microservices framework. The core business module is **ZHPG** (综合评估), which manages the full lifecycle of evaluation tasks across 7 subsystems.

## Tech Stack

- **Backend**: Java 1.8, Spring Boot 2.7.2, Spring Cloud 2021.0.3, Spring Cloud Alibaba 2021.0.1.0
- **ORM**: MyBatis-Plus 3.5.1 (replaced original MyBatis)
- **Frontend**: Vue 2.6 + Element UI 2.15 + Vuex + Vue Router
- **Registry/Config**: Nacos 2.x (local instance at `D:\zzxpg\nacos`, default port 8848)
- **Gateway**: Spring Cloud Gateway (port 8080)
- **Auth**: JWT-based via RuoYi Security module
- **Database**: MySQL 8.0, tables prefixed `pgzc_`
- **Other**: Sentinel (flow control), Seata (distributed transactions), Swagger 3.0

## Build & Run Commands

### Backend (Maven, from `pgzzx/` root)
```bash
# Full build
mvn clean package -DskipTests

# Run individual services (bat scripts in bin/)
bin/run-gateway.bat       # Gateway - port 8080
bin/run-auth.bat          # Auth center - port 9200
bin/run-modules-system.bat # System module - port 9201
bin/run-modules-file.bat  # File service - port 9300
bin/run-monitor.bat       # Monitor - port 9100
# ZHPG module runs on port 9303 (no bat script, run via IDE or:)
# cd ruoyi-modules/ZHPG && mvn spring-boot:run
```

### Frontend (from `pgzzx/ruoyi-ui/`)
```bash
npm install
npm run dev          # Dev server on port 80, proxies API to localhost:8080
npm run build:prod   # Production build
npm run lint         # ESLint check
```

### Prerequisites
Start Nacos first: `D:\zzxpg\nacos\bin\startup.cmd -m standalone`

## Module Architecture

```
pgzzx/
├── ruoyi-gateway         # API Gateway (8080) - routes by path prefix
├── ruoyi-auth            # Auth center (9200) - JWT token management
├── ruoyi-api/
│   └── ruoyi-api-system  # Feign client interfaces for system service
├── ruoyi-common/         # Shared libraries
│   ├── ruoyi-common-core       # Core utilities, base classes, constants
│   ├── ruoyi-common-security   # Token validation, permission annotations
│   ├── ruoyi-common-redis      # Redis cache operations
│   ├── ruoyi-common-log        # @Log annotation for audit logging
│   ├── ruoyi-common-datascope  # Data permission filtering
│   ├── ruoyi-common-datasource # Multi-datasource support
│   ├── ruoyi-common-swagger    # API documentation config
│   ├── ruoyi-common-web        # Web layer utilities
│   ├── ruoyi-common-dubbo      # Dubbo RPC support
│   ├── ruoyi-common-job        # Job scheduling support
│   └── common-kafka            # Kafka messaging
├── ruoyi-modules/
│   ├── ruoyi-system      # System management (9201) - users, roles, menus, depts
│   ├── ruoyi-file         # File upload service (9300)
│   ├── ZHPG               # **Core business module** (9303) - 74 Java files
│   ├── pggc               # 评估归档 module (minimal, 1 file)
│   ├── txxt               # 体系协同 module (6 files)
│   ├── ruoyi-xxl-job-admin # XXL-Job admin console
│   └── XxlJobExecutor     # XXL-Job executor
├── ruoyi-visual/
│   └── ruoyi-monitor      # Spring Boot Admin monitor (9100)
├── ruoyi-ui/              # Vue 2 frontend
└── sql/                   # DB init scripts (zhpg_init.sql)
```

## ZHPG Module — 7 Subsystems

The core ZHPG module (`ruoyi-modules/ZHPG`, service name `zhpg`, port 9303) is organized into 7 subsystems following the evaluation workflow:

1. **RWGJ** (评估任务构建) — task creation, templates, criteria → `/zhpg/task/**`, `/zhpg/template/**`, `/zhpg/criterion/**`
2. **ZBGJ** (指标体系构建) — indicator trees, indicator systems, co-sign approval → `/zhpg/indicator/**`, `/zhpg/indicatorSystem/**`, `/zhpg/cosign/**`
3. **SFMX** (算法模型配置) — 32 built-in algorithm models (traditional + AI) → `/zhpg/model/**`
4. **PGSJ** (评估数据匹配) — data sources, auto-mapping → `/zhpg/datasource/**`, `/zhpg/dataMapping/**`
5. **ZHFX** (综合分析计算) — calculation flow templates, task execution → `/zhpg/calcFlow/**`, `/zhpg/calc/**`
6. **ZNFX** (评估结果智能分析) — 5 analysis dimensions → `/zhpg/analysis/**`
7. **BGSC** (评估报告生成) — Word/PDF report generation → `/zhpg/report/**`

**Main workflow**: RWGJ → ZBGJ → SFMX → PGSJ → ZHFX → ZNFX → BGSC

## Code Conventions

- Backend follows standard RuoYi layering: `controller/` → `service/` → `service/impl/` → `mapper/` → `domain/`
- All ZHPG entities, mappers, and services are under `com.ruoyi.*.zhpg` sub-packages
- Business tables use `pgzc_` prefix (15 tables, see `sql/zhpg_init.sql`)
- Frontend API modules at `src/api/zhpg/`, views at `src/views/zhpg/{subsystem}/`
- Dev proxy: frontend requests to `VUE_APP_BASE_API` are forwarded to gateway at `localhost:8080`
- Gateway routes requests by path prefix to downstream services via Nacos service discovery

## Nacos Configuration

Service configs are managed in Nacos (not local YAML). Each service's `bootstrap.yml` only contains Nacos connection info. The gateway route for ZHPG must be configured in Nacos:
```yaml
- id: zhpg
  uri: lb://zhpg
  predicates:
    - Path=/zhpg/**
  filters:
    - StripPrefix=0
```
