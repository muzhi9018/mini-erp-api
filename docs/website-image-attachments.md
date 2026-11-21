# 官网图片附件接入

保存图片时，先通过现有附件上传接口获取 `Attachment.id`，再传入以下字段（Java 为 Long、数据库为 bigint）：

| 位置 | 保存字段 | 查询返回的授权地址 |
| --- | --- | --- |
| 分类语言内容 `categoryI18n` | `imageAttachmentId` | `imageUrl` |
| 商品语言内容 `productI18n` | `coverImageAttachmentId` | `coverImageUrl` |
| 商品语言内容 `productI18n` | `featureImageAttachmentId` | `featureImageUrl` |
| `applications[]`、`cases[]` | `imageAttachmentId` | `imageUrl` |

`POST /website/product/addI18n` 和 `POST /website/product/category/addI18n` 的语言内容直接作为请求体。应用场景、案例图片附件 ID 必填；分类、封面、特点图片沿用原有可选规则。填写的附件必须存在且未逻辑删除。

`GET /website/product/list`、`GET /website/product/category/list` 返回附件 ID 和授权地址；商品列表还返回所属语言的应用场景、案例媒体。原有列表 `id` 仍为商品/分类 ID，商品新增 `productI18nId` 用于标识语言记录。列表是现有管理接口，媒体保留 `isShow` 状态，不额外过滤隐藏内容。

地址沿用 OSS Bridge 的默认有效期（1 小时）。到期后重新查询，不将签名地址保存为业务数据。附件被删除后保留关联 ID，查询地址为 null。一个批次内同一附件只查询、签名一次。URL 字段仅在 VO 中用于返回，不参与数据库写入。

## PostgreSQL 迁移

执行 `docs/sql/20260920_website_image_attachments.sql`，再部署新版本和适配后的前端。代码和表结构需要同步切换，脚本不自动运行。

有历史图片时，先把图片上传到 OSS 并生成附件记录，再在 SQL 的临时映射表 INSERT 位置填入“旧 URL → 附件 ID”。已有附件可直接使用其 ID，不必重复上传。脚本包括逻辑删除记录的历史地址；未映射完整会报错并使事务失败，需要 ROLLBACK 后补齐重试。没有历史地址时可直接执行。

迁移把旧 URL 列改名为 `*_url_legacy` 留存，新增 `*_attachment_id` 列。不会根据 URL 猜测附件，也不会清空旧地址。旧列的非空约束和默认值会移除，避免新版本插入记录时仍被旧字段约束。新字段沿用项目的逻辑关联方式。SQL 尚未在实际 PostgreSQL 数据库执行。
