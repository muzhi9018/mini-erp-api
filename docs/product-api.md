# 内部商品接口联调说明

更新日期为 2026-11-20。对应 [商品管理设计](product-management-design.md)，仅修改后端；未对业务数据库执行建表。数据库需预先按设计使用 `docs/sql/20261119_product_management.sql` 或完整 `schema.sql` 初始化，二选一，已有表不可重复执行完整建表脚本。

## 1. 业务归属和权限

只保留三个 Controller：`ProductCategoryController` 维护类目，`ProductUnitController` 维护计量单位，`ProductController` 集中维护商品、图片、规格、候选值及所属 SKU。规格、图片、SKU 等不再保留按表生成的空 Controller。

全部接口需登录，返回 `JsonResult<T>`；分页数据为 `IPage<T>`。角色需配置下列权限码。商品名称、类目、规格等业务资料只维护一套内容；错误信息与校验提示按请求 `Accept-Language` 返回英文、简体中文或繁体中文，在 `i18n/product/messages*.properties` 维护，中文仅作为资源缺失时的兜底。

| 方法和路径 | 权限码 | 用途 |
| --- | --- | --- |
| GET `/product/category/tree` | `product-manager:product-category:query` | 类目树 |
| POST `/product/category/create` | `product-manager:product-category:create` | 创建类目 |
| POST `/product/category/update` | `product-manager:product-category:update` | 修改名称、排序、备注 |
| POST `/product/category/delete` | `product-manager:product-category:delete` | 删除空类目 |
| GET `/product/unit/list` | `product-manager:product-unit:query` | 单位列表 |
| POST `/product/unit/create` | `product-manager:product-unit:create` | 创建单位 |
| POST `/product/unit/update` | `product-manager:product-unit:update` | 修改单位 |
| POST `/product/unit/delete` | `product-manager:product-unit:delete` | 删除未引用单位 |
| GET `/product/list` | `product-manager:product:query` | 商品分页 |
| GET `/product/detail` | `product-manager:product:query` | 完整管理详情 |
| POST `/product/create` | `product-manager:product:create` | 完整创建商品 |
| POST `/product/update` | `product-manager:product:update` | 编辑共同资料 |
| POST `/product/images/save` | `product-manager:product:update` | 保存完整图片列表 |
| POST `/product/change-status` | `product-manager:product:change-status` | 商品启停 |
| POST `/product/delete` | `product-manager:product:delete` | 删除商品及附属主档 |
| POST `/product/spec/create`、`/update`、`/delete` | `product-manager:product:update` | 规格维度维护 |
| POST `/product/spec/value/create`、`/update`、`/delete` | `product-manager:product:update` | 候选值维护 |
| GET `/product/sku/list` | `product-manager:product:query` | SKU 管理分页 |
| GET `/product/sku/options` | `product-manager:product:sku:options` | 业务选品，无进价和审计字段 |
| POST `/product/sku/create`、`/update`、`/preview` | `product-manager:product:update` | SKU 批量新增、编辑、组合预览 |
| POST `/product/sku/images/save` | `product-manager:product:update` | 保存 SKU 自身的完整图片列表 |
| POST `/product/sku/change-status` | `product-manager:product:change-status` | SKU 启停 |
| POST `/product/sku/stock/set` | `product-manager:product:stock:set` | 独立设置库存绝对数量 |
| POST `/product/sku/delete` | `product-manager:product:delete` | 删除零库存 SKU |

单位创建提交 `code`、`name` 和可选的 `sortOrder`。编辑提交 `id`、`name` 和可选的 `sortOrder`。删除提交 `id`。数量小数位在 SKU 表单中维护。单位名称修改时，同步递增受影响商品的版本，已生成的快照保留原名称。类目参数见 [类目接口文档](product-category-api.md)。

## 2. 完整创建商品

`POST /product/create` 与商品编辑等接口共用 `ProductVO`，嵌套 SKU 共用 `ProductSkuVO`。下面的请求创建一个颜色维度和一条实际经营 SKU。

```json
{
  "categoryId": 100,
  "code": "CABINET",
  "name": "两门衣柜",
  "brand": "示例品牌",
  "description": "商品说明",
  "status": "ENABLED",
  "sortOrder": 0,
  "images": [
    {"attachmentId": 201, "isCover": true},
    {"attachmentId": 202, "isCover": false}
  ],
  "specs": [
    {
      "clientKey": "color",
      "name": "颜色",
      "valueType": "TEXT",
      "sortOrder": 0,
      "values": [
        {"clientKey": "white", "textValue": "白色", "sortOrder": 0},
        {"clientKey": "gray", "textValue": "灰色", "sortOrder": 1}
      ]
    }
  ],
  "skus": [
    {
      "code": "CABINET-WHITE",
      "model": "M-01",
      "unitId": 300,
      "quantityScale": 2,
      "referencePurchasePrice": 100,
      "referencePurchaseCurrency": "CNY",
      "referenceSalePrice": 500,
      "referenceSaleCurrency": "TWD",
      "images": [{"attachmentId": 203}],
      "stockQuantity": 0,
      "status": "ENABLED",
      "selections": [
        {"specClientKey": "color", "valueClientKey": "white"}
      ]
    }
  ]
}
```

- `status` 显式提交 `ENABLED` 或 `DISABLED`。商品图片、规格和 SKU 自身图片均可省略，按空列表处理；SKU 至少一条。无规格时不填写维度，SKU 的 `selections` 为空或省略，同一无规格商品只允许一条有效 SKU。
- 临时维度键在请求内唯一，候选值键在所属维度内唯一；SKU 必须为每个维度选择一个候选值。临时键不入库，详情返回数据库 ID，后续维护使用 ID。
- 数值维度使用 `NUMBER`，候选值填写 `numberValue`；`unitId` 可选。文本维度不能填写单位。文本值和数值互斥，`900` 与 `900.0` 不能重复建立。
- SKU 的 `quantityScale` 可填 0～6，新增时省略或传 null 按 0 保存。初始库存省略时为 0，填写时按该 SKU 的小数位校验。金额和库存均可为 0，最多 14 位整数及 6 位小数，且须非负。采购价和售价各自与币种成对提交，每组可同时留空，币种支持 `CNY` 和 `TWD`。
- 主键、关联归属、审计字段、组合摘要和版本由服务端维护。任一步失败，商品、图片、规格、候选值、SKU 及选择关系全部回滚。成功返回完整 `ProductVO`，商品和 SKU 初始版本均为 0。

## 3. 编辑和版本

商品资料、启停、图片保存及批量新增 SKU 的请求共用 `ProductVO`，SKU 资料、图片、启停及库存请求共用 `ProductSkuVO`。下表中使用 `productId` 或 `skuId` 的共用模型请求也接受 `id`。同时发送两者时，值须一致。

商品资料编辑提交 `id`、`expectedVersion`、`categoryId`、`name`、可选 `brand`、`description`、`sortOrder`。编码、启停、图片、库存和 SKU 列表不由该接口修改；品牌、说明为空可清除。

| 操作 | 固定请求结构 | expectedVersion 的含义 |
| --- | --- | --- |
| 商品启停 | `productId`、`expectedVersion`、`status` | 商品版本 |
| 商品删除 | `id`、`expectedVersion` | 商品版本 |
| 保存商品图片 | `productId`、`expectedVersion`、`images` | 商品版本 |
| 维度创建 | `productId`、`expectedVersion`、`name`、`valueType`、可选 `unitId`、`sortOrder`、`values` | 商品版本 |
| 维度编辑 | `id`、`expectedVersion`、`name`、可选 `sortOrder` | 商品版本 |
| 维度删除 | `id`、`expectedVersion` | 所属商品版本 |
| 候选值创建 | `specId`、`expectedVersion`、`textValue` 或 `numberValue`、可选 `sortOrder` | 商品版本 |
| 候选值编辑 | `id`、`expectedVersion`、可选值及 `sortOrder` | 商品版本 |
| 候选值删除 | `id`、`expectedVersion` | 所属商品版本 |
| 批量创建 SKU | `productId`、`expectedVersion`、`skus` | 商品版本 |
| SKU 编辑 | `id`、`expectedVersion`、`unitId`、可选 `model`、`quantityScale`、参考金额与币种、`sortOrder` | SKU 行版本 |
| 保存 SKU 图片 | `id`、`expectedVersion`、`images` | SKU 行版本 |
| SKU 启停 | `skuId`、`expectedVersion`、`status` | SKU 行版本 |
| SKU 删除 | `id`、`expectedVersion` | SKU 行版本 |
| 库存设置 | `skuId`、`stockQuantity`、`expectedVersion` | SKU 行版本 |

每次成功维护同一商品只递增一次商品版本，批量新增多条 SKU 也只加 1；SKU 编辑、图片保存、启停、库存及删除还递增该 SKU 的行版本。版本不匹配时返回业务异常，需重新读取后确认，不能自动用新版本覆盖。商品编辑、启停返回新商品版本；SKU 编辑、启停、库存设置和删除返回 `skuId`、`stockQuantity`、`skuVersion`、`productVersion`。商品图片保存返回 `productId`、`productVersion`、`images`，SKU 图片保存返回 `skuId`、`skuVersion`、`productVersion`、`images`。规格及候选值维护后刷新商品详情获取最新统一版本。

SKU 普通编辑可更新型号、单位、数量小数位、参考价格与币种及排序。省略 `quantityScale` 或传 null 时保留原值。提交新精度时，当前库存必须能用该精度准确表示。例如库存为 1.50 时，改为 0 位会被拒绝，改为 1 位可以保存。单位仅在零库存且没有业务快照使用时可修正。清除某组参考价时，该组金额和币种同时留空。

已有 SKU 时不能增删维度；维度类型、单位不通过编辑修改。已被 SKU 使用的候选值不能改写或删除，编辑时省略文本、数值，仅传排序可调整顺序。已有业务使用的商品不能通过删光 SKU 重建维度，应新建商品档案。

已有商品新增 SKU 时，各项使用 `ProductSkuVO`，规格选择改用数据库 ID，例如：

```json
{
  "productId": 400,
  "expectedVersion": 2,
  "skus": [
    {
      "code": "CABINET-GRAY",
      "unitId": 300,
      "quantityScale": 2,
      "status": "ENABLED",
      "specValues": [{"specId": 401, "specValueId": 403}]
    }
  ]
}
```

组合预览 `POST /product/sku/preview` 提交 `productId` 和 `dimensions: [{specId, specValueIds: [...]}]`，需覆盖每个维度。返回只含 ID 选择关系的组合列表，不写数据库，单次最多预览 1000 个组合。

## 4. 图片与库存

附件沿用 `POST /attachment/upload`，模块参数使用新增的 `model=PRODUCT`。关联前检查附件存在且为 JPEG、PNG、WebP、GIF、AVIF 或 BMP 图片。创建和图片保存只持久化附件 ID，读取时批量生成授权 URL。

图片保存必须提交完整 `images`，缺失或 null 被拒绝，`[]` 明确清空。列表顺序即展示顺序，同一附件不能重复，多张主图被拒绝；未指定主图时第一张自动补位。有效关联按附件 ID 复用，移除关联不删除附件及实际对象。两种图片都复用 `ProductImageVO`，保存时仅处理条目中的 `attachmentId`、`isCover`，归属由服务端确定。商品和各 SKU 分别维护图片顺序与主图，可以使用同一附件。

当前图片条目通过 `imageType` 和 `ownerId` 表示归属。类型为 `PRODUCT` 时 `ownerId` 是商品 ID，类型为 `SKU` 时是 SKU ID。保存请求仍通过外层商品或 SKU 参数定位归属。

库存按 SKU 的 `quantityScale` 校验，只忽略小数部分的末尾零。库存只设置绝对值，例如 `{ "skuId": 500, "stockQuantity": 8, "expectedVersion": 0 }` 表示当前数量保存为 8。没有库存流水、自动扣减、预占或出入库语义。库存为 0 不影响选品，非零库存阻止 SKU 或商品删除。库存权限独立于资料编辑权限。

## 5. 查询与字段隔离

商品列表参数为 `pageNum`、`pageSize`、可选 `keyword`、`categoryId`、`status`。关键字覆盖商品名称、编码和 SKU 货号、型号，按商品去重分页；类目筛选包含后代。详情通过 `productId` 查询，包含图片、规格及候选值、SKU 和结构化规格。

SKU 列表与选品额外支持 `productId`、`skuStatus` 和多个结构化 `specs` 条件：

```text
/product/sku/list?pageNum=1&pageSize=15&specs[0].specId=401&specs[0].specValueId=402
/product/sku/list?pageNum=1&pageSize=15&specs[0].specId=410&specs[0].minimum=900&specs[0].maximum=1200
```

多个维度条件全部满足，数值边界包含上下限，不能同时填写候选值和数值区间。分页稳定按排序、ID 返回。

管理详情、SKU 列表和选品都从 SKU 返回 `quantityScale`，供前端设置数量输入的小数位。管理详情及管理 SKU 查询包含采购价、售价及其各自的币种。选品接口同时要求商品和 SKU 启用，返回独立 `ProductOptionVO`，包括 SKU 身份、名称、型号、规格、单位、数量精度、参考售价、`referenceSaleCurrency`、库存参考值、SKU 自身的 `images` 及 `productVersion`、`skuVersion`。进价、采购币种和管理审计字段只用于管理资料。

商品详情中的 SKU、SKU 管理列表和选品均返回 SKU 自身的图片和授权地址。前端收到 `images: []` 时，从已查询的商品列表或详情取得商品主图。

## 6. 快照与后续单据

`IProductSnapshotService` 沿用实体 Service 的 MyBatis-Plus 默认继承。快照业务通过 `getOrCreate` 获取或创建，通过 `readForDocument` 读取。快照 HTTP 查询随报价和订单业务接入，读取权限由关联单据校验。

单据 Service 在完成写权限校验后，在自己的保存事务中调用：

```java
ProductSnapshotVO snapshot = snapshotService.getOrCreate(skuId, productVersion, skuVersion);
// 明细保存 snapshot.getId()，任一步失败时与本次新建快照整体回滚。
```

服务强制加入已有事务，先锁商品、再锁 SKU，校验状态及选品期望版本后按 `(source_sku_id, product.version)` 查找并复用。不存在时整体保存快照主记录、全部选中规格、商品图片和当前 SKU 自身图片。快照中的 `quantityScale` 取生成时的 SKU 精度，关联单据按该快照的精度校验数量。快照图片以 `imageSource` 区分 `PRODUCT` 与 `SKU`，每组保留各自的主图和顺序。快照只冻结描述资料，单据自行保存交易金额、币种和数量。商品资料更新只递增版本，不主动生成快照。

同版本并发获取会复用同一完整快照。数据库发生需要回滚的死锁、序列化或唯一冲突时，单据调用方必须重新执行整个保存事务并再次校验版本，不能在失败事务中继续写入。当前不单独重试快照步骤。

历史读取由有权读取关联单据的 Service 调用 `readForDocument(snapshotId)`，不能直接作为通用商品 HTTP 接口使用。报价转订单直接沿用报价的快照 ID；当前来源商品、SKU、图片关联删除不影响旧快照。现有附件接口只有上传能力，商品删除及移除图片均不删除附件；未来增加附件删除或孤立清理时，必须同时检查当前图片和全部快照图片引用。
