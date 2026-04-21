# 古诗词推广系统

基于 Android Studio、Kotlin、XML、MVVM 和 Jetpack Navigation 开发的古诗词查询、学习与社区互动平台。

## 技术栈

- Kotlin
- XML
- MVVM
- Jetpack Navigation
- Material Design
- MySQL 8.0+

## 当前内容

- Android 单 Activity + Fragment 项目骨架
- Feature-based 模块结构
- 首页、登录注册、用户中心、查询浏览、趣味学习、社区互动 UI 框架
- 古诗词内容数据库初始化脚本
- 每日推荐与相关推荐数据脚本
- 团队开发统一业务表结构

## 目录结构

```text
app/                Android 应用源码
sql/                数据库初始化与业务脚本
docs/               项目文档
```

## 数据库初始化

请先阅读：

- `docs/数据库初始化说明.md`

核心 SQL 文件：

- `sql/poetry_full_init.sql`
- `sql/clean_gushiwen.sql`
- `sql/enrich_recommendation.sql`
- `sql/poetry_team_schema.sql`

## 团队协作建议

- 每位成员在本地初始化自己的 MySQL 数据库
- GitHub 统一管理代码、SQL 和文档
- 后续数据库结构调整请通过新增迁移脚本完成
- 建议按模块分工：home、auth、user、poem、learning、community
