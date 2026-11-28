# 报价接口联调

所有接口使用 ERP 登录态，响应统一为 `JsonResult<T>`，先检查 `success`，失败提示读取 `message`。列表参数为 `pageNum`、`pageSize`，默认分别为 `1`、`15`，分页数据读取 `data.records`。

报价模块只使用四个业务 VO，均继承对应实体；同一对象的新增、修改、查询和删除共用模型，各接口只处理自己的可写字段。

| 模型 | 用途 |
| --- | --- |
| `QuotationVO` | 报价主表、筛选条件、复制来源及 `items` |
| `QuotationItemVO` | 报价明细、选品版本、商品展示字段和 `images` |
| `QuotationTemplateVO` | 模板主表、可用性及 `items` |
| `QuotationTemplateItemVO` | 模板明细、实时 `product` 选品资料和 `issues` |

入参和出参均采用平铺字段，不再使用 `form`、`document`、`customer`、`issuer`、`discount`、`totals` 或平行的 `skuIds`。图片复用商品模块的 `ProductSnapshotImageVO`，选品复用 `ProductOptionVO`。

JavaScript 中业务 ID 保持字符串。后端沿用项目序列化规则，超过 JavaScript 安全整数的长整型以字符串返回，请求可提交字符串 ID。金额、单价、数量和折扣使用默认 JSON 数值序列化，不逐字段强制转字符串；成交金额计算保留两位小数，展示格式由前端处理。日期按台北业务时间返回，带时区偏移。

## 报价接口

| 方法与路径 | 入参 | 返回的 `data` | 权限 |
| --- | --- | --- | --- |
| `GET /quotation/sku/options` | 选品筛选条件、分页参数 | `IPage<ProductOptionVO>` | 报价创建或修改权限 |
| `POST /quotation/preview` | `QuotationVO`，新建或复制草稿 | 完整 `QuotationVO`，不保存、不编号 | 报价创建权限，复制来源另校验读取资格 |
| `POST /quotation/create` | 同一 `QuotationVO` | 已保存的完整 `QuotationVO`，包含正式 ID、单号、版本和明细 | 同上 |
| `GET /quotation/list` | `QuotationVO` 筛选字段、分页参数 | `IPage<QuotationVO>`，每条为主表业务字段，不返回 `items` | 报价查询权限，仅当前用户的报价 |
| `GET /quotation/detail?id=…` | 报价 ID | 主表业务字段加 `items` | 同上 |
| `POST /quotation/update/preview` | `QuotationVO`，包含 `id`、`version` 和完整内容 | 同结构的修改预览，保留正式单号 | 报价修改权限，且报价未转单 |
| `POST /quotation/update` | 同上 | 更新后的完整 `QuotationVO`，`version` 递增 | 同上 |
| `POST /quotation/delete` | `QuotationVO`，只需 `{id, version}` | `null` | 报价删除权限，且报价未转单 |
| `POST /quotation/copy` | `QuotationVO`，只需 `{id, clearCustomer}` | 平铺的复制草稿，包含 `sourceId`、`sourceVersion` 和明细 | 报价查询和创建权限，且可读取原报价 |

权限前缀为 `sales-manager:quotation`，操作后缀分别为 `create`、`update`、`query`、`delete`。详情、修改、删除和复制均校验报价归属。

### 新建与预览

两个接口提交相同结构，预览返回的数据可以直接提交新建接口：

```json
{
  "title": "展厅储物设备报价",
  "quotedAt": "2026-11-20T14:30:00+08:00",
  "customerName": "演示客户",
  "contactName": "王女士",
  "contactPhone": "0900-000-000",
  "shippingAddress": "演示地址 1 号",
  "remark": "交期另行确认",
  "discountRate": 95.00,
  "items": [
    {
      "skuId": "2193456789012345678",
      "productVersion": 0,
      "skuVersion": 0,
      "quantity": 2.00,
      "discountRate": 90.00,
      "remark": "依客户要求贴标签"
    },
    {
      "skuId": "2193456789012345679",
      "productVersion": 0,
      "skuVersion": 0,
      "quantity": 1.50,
      "discountedAmount": 100.00
    }
  ]
}
```

新建允许省略标题和报价时间，服务端默认使用「报价单」和当前业务时间。至少一行商品；数量大于零，最多六位小数，同时受该 SKU 数量精度限制。新选商品必须提交 `skuId`、`productVersion`、`skuVersion`。商品需启用且参考售价有效，零价、零库存可选。所有行售价币种必须一致，币种由服务端确定。

整单和每行直接使用 `discountRate`、`discountedAmount`，两者互斥。`95.55` 表示支付原价的 95.55%；`discountedAmount` 表示整单或整行优惠后的金额，非负且不超过优惠前金额。比例在 0～100 之间，优惠输入最多两位小数。无优惠时两者均省略或传 `null`。

服务端只写入标题、报价时间、四项客户资料、备注、两级优惠及明细来源与数量。单号、主键、归属用户、开单方、快照、价格、币种、排序、成交金额、版本、转单状态和审计字段由服务端确定；请求中的同名展示字段不会覆盖这些值。`contactEmail` 为数据库预留字段，当前接口不读写。

### 修改与沿用历史明细

直接在详情响应上修改可写字段，保留报价的 `id`、`version` 和沿用行的 `items[].id`，即可提交修改预览或修改接口。修改须提供非空标题和报价时间，`items` 是最终完整列表：省略的旧行会被移除，顺序以提交顺序为准。

- 行有 `id`：表示沿用当前报价的历史明细；复制草稿则沿用 `sourceId` 指向的原报价明细。服务端校验行归属，读取原快照、币种和单价，只应用本次数量、优惠和备注。返回的 `skuId`、版本和展示字段可以一同提交，但不改变来源。修改不允许重复使用同一原明细 ID。
- 行无 `id`：表示新选商品，须提交当前 `skuId`、`productVersion`、`skuVersion`。替换商品时清除原行 `id`，重新选品并填入两个当前版本。

同一报价修改保持原币种和正式单号。清空可选客户字段、备注或优惠时删除对应属性或传 `null`。版本冲突时重新读取详情并确认；已转单报价只允许查看或复制。

删除请求示例：

```json
{"id":"2193456789012345678","version":0}
```

### 复制

复制请求必须明确选择是否清空客户资料：

```json
{"id":"2193456789012345678","clearCustomer":true}
```

返回草稿的 `sourceId`、`sourceVersion` 标识原报价，保留各行原明细 `id`；不返回正式报价 `id`、`version`、`orderGenerated`、`quoteNumber`。复制不写入数据库，草稿可直接提交 `/quotation/preview` 或 `/quotation/create`。复制预览也保留来源字段和原明细 ID，可以直接保存。新建时生成新的报价和明细主键、正式单号及版本。

`clearCustomer=false` 保留四项客户资料，`true` 清空。后续以草稿当前填写的客户资料为准。开单方名称和联系方式沿用原单，Logo 使用现行配置。原单版本变化后需重新复制并确认。

### 返回字段

主表直接返回 `id`、`version`、`orderGenerated`、`quoteNumber`、`title`、`quotedAt`、`currency`、`customerName`、`contactName`、`contactPhone`、`shippingAddress`、`remark`、`issuerName`、`issuerContact`、`discountRate`、`discountedAmount`、`originalAmount`、`subtotalAmount`、`totalAmount`，另补充 `issuerLogoUrl`。字段为 `null` 时可能省略。新建预览和复制草稿没有正式单号，页面显示「未编号」。

`items` 每行包含明细表字段 `id`、`quotationId`、`snapshotId`、`currency`、`quantity`、`unitPrice`、`originalAmount`、`discountRate`、`discountedAmount`、`lineAmount`、`remark`、`sortOrder`，并补充 `skuId`、`productVersion`、`skuVersion`、`skuCode`、`productName`、`model`、`specificationText`、`specs`、`unitName`、`quantityScale`、`images`。商品资料和价格不随历史来源停用、删除或修改而变化。

结构化规格位于 `data.items[].specs`，复用 `ProductOptionSpecVO`，无规格时返回 `[]`。每项包含 `specId`、`specValueId`、`specName`、`valueType`（`TEXT` / `NUMBER`）、`textValue`、`numberValue`、`unitName`，空值字段可能省略。历史详情及沿用行按对应的 `snapshotId` 批量读取 `product_snapshot_spec`，按快照中的 `sortOrder`、`id` 排序；名称、值和单位均取自保存时的快照，不回查当前商品规格。两个 ID 对应快照记录的来源规格及候选值，仅用于追溯。新选行的预览使用当前选品规格，保存后使用冻结规格；复制、修改和预览沿用同一结构。`specificationText` 保留为摘要，前端需要逐项展示时读取 `specs`。

规格示例：

```json
[
  {
    "specId": "2193456789012345701",
    "specValueId": "2193456789012345801",
    "specName": "颜色",
    "valueType": "TEXT",
    "textValue": "石墨灰"
  },
  {
    "specId": "2193456789012345702",
    "specValueId": "2193456789012345802",
    "specName": "宽度",
    "valueType": "NUMBER",
    "numberValue": 900,
    "unitName": "mm"
  }
]
```

金额展示使用服务端返回值：每行成交额为 `lineAmount`；整单 `originalAmount` 是行原价合计，`subtotalAmount` 是整单优惠前金额，`totalAmount` 为最终报价。优惠差额可以由已有金额推导：商品行优惠合计为 `originalAmount - subtotalAmount`，整单优惠为 `subtotalAmount - totalAmount`，单行优惠为该行 `originalAmount - lineAmount`，不另返回三个差额字段。`discountedAmount` 是手填的优惠后金额，与优惠差额含义不同。例如上例参考单价为 `129.345` / `80.00` 时，商品小计为 `332.82`，整单优惠为 `16.64`，总额为 `316.18`。

图片位于 `data.items[].images`，无图为 `[]`。每张图片提供 `id`、`snapshotId`、`sourceImageId`、`attachmentId`、`imageSource`（`PRODUCT` / `SKU`）、`isCover`、`sortOrder`、`url`；草稿图片可能省略快照字段。历史图片读取快照，按来源、顺序和图片 ID 排序；新选商品的预览使用选品资料中的 SKU 图片。授权地址每次读取时重新生成，附件不可用时可能为空，不应长期缓存。

VO 不额外忽略实体字段：`gmtCreate`、`gmtModified`、`createUser`、`updateUser`、`isDeleted`、归属用户及取号字段在有值时正常返回，客户端提交这些字段不会改变写入白名单。预留的 `contactEmail` 仍按实体原有的 `select = false` 不查询，也不接受写入。安全响应头中的 `Cache-Control` 包含 `no-store`。

## 模板接口

个人与公共模板前缀分别为 `/quotation/template/personal`、`/quotation/template/public`，范围由路由确定，不能通过提交 `scope` 或 `ownerUserId` 改变范围或创建人。

| 方法与后缀 | 入参 | 返回的 `data` |
| --- | --- | --- |
| `GET /list` | 可选 `keyword`、`pageNum`、`pageSize` | `IPage<QuotationTemplateVO>`，不返回 `items` |
| `GET /detail?id=…` | 模板 ID | `QuotationTemplateVO`，包含 `items` |
| `POST /create` | `QuotationTemplateVO`，包含完整内容 | 已保存的完整 `QuotationTemplateVO` |
| `POST /update` | 同一模型，包含 `id`、`version` 和完整内容 | 更新后的完整模板，`version` 递增 |
| `POST /delete` | 同一模型，只需 `{id, version}` | `null` |
| `POST /apply` | 同一模型，只需 `{id}` | `QuotationVO` 报价草稿，可直接预览或新建 |
| `GET /sku/options` | 与报价选品相同的筛选条件和分页参数 | `IPage<ProductOptionVO>` |

模板新增与修改共用以下结构，修改额外提交顶层 `id` 和 `version`：

```json
{
  "name": "门店陈列 · 常用组合",
  "title": "展厅储物设备报价",
  "discountRate": 95.00,
  "remark": "交期另行确认",
  "items": [
    {"skuId":"2193456789012345678","discountRate":90.00,"remark":"第一行"},
    {"skuId":"2193456789012345678","discountRate":80.00}
  ]
}
```

模板名称、报价标题、非空明细为必填；名称最多 128 字符，标题最多 256 字符，备注最多 1024 字符，折扣比例为 0～100，最多两位小数。模板只保存整单和每行的折扣比例，不支持固定优惠后金额。同一 SKU 可以在不同位置重复出现。创建和更新只写入 `name`、`title`、`discountRate`、`remark` 和行的 `skuId`、`discountRate`、`remark`；行 ID 和排序由服务端生成。

列表和详情返回主表业务字段及 `ownedByCurrentUser`、`usable`、`issueCount`。`scope` 为 `PERSONAL` / `PUBLIC`。详情 `items` 为 `QuotationTemplateItemVO`，复用明细表的 `id`、`templateId`、`skuId`、`sortOrder`、`discountRate`、`remark`，另带 `referenceSaleCurrency`、当前 `product` 和 `issues`。来源失效时保留 SKU ID 和行顺序，供替换或移除；商品问题会阻止套用。

| 问题码 | 含义 |
| --- | --- |
| `SKU_MISSING` / `PRODUCT_MISSING` | SKU 或商品不存在或已删除 |
| `SKU_DISABLED` / `PRODUCT_DISABLED` | SKU 或商品已停用 |
| `PRICE_MISSING` / `CURRENCY_INVALID` | 缺少有效参考售价或币种 |
| `CURRENCY_CONFLICT` | 可用商品的当前售价币种不一致 |
| `OPTION_UNAVAILABLE` | 无法读取当前选品资料 |

套用直接返回平铺报价草稿：标题、整单备注和折扣来自模板，各行包含当前 SKU、两个选品版本、商品展示资料、默认数量 `1`、折扣和备注。没有模板 ID、报价 ID 或明细 ID；单价和币种读取当前选品。套用不保存报价或快照，客户端可填入客户资料和时间后直接预览或保存。替换已有草稿的商品时，客户资料、时间和原整单备注由客户端按替换规则保留。

个人查询需要 `sales-manager:quotation-template:personal:query` 且为所有者；公共列表需要 `sales-manager:quotation-template:public:query`，公共详情需要登录。维护权限使用相应范围的 `create`、`update`、`delete` 后缀。删除额外要求当前用户为创建人。公共套用需要报价创建权限；个人套用另需个人模板查询权限和归属校验。选品需要对应范围的模板创建或修改权限。

## 选品与版本冲突

报价选品和模板选品均支持 `keyword`、`categoryId`、`specs[0].specId`、`specs[0].specValueId` 或 `specs[0].minimum` / `specs[0].maximum`、`referenceSaleCurrency=CNY|TWD`。使用 `skuId` 精确查询当前行，核对币种变化时可省略币种过滤。

商品版本变化时保留草稿，按返回的行号定位问题，通过选品接口读取最新资料；对比售价、币种、规格、数量精度，经确认后刷新 `productVersion` 和 `skuVersion`。无结果时移除或替换该行。沿用历史明细不重新定价。

## 前端迁移

所有报价写接口和预览接口均切换到同一平铺模型，模板更新和两类删除统一使用 `version`。数值字段恢复默认 JSON 数值，实体审计字段正常返回；`lineDiscountAmount`、`orderDiscountAmount` 和明细 `discountAmount` 已删除，展示差额使用已有金额计算。

| 原路径或字段 | 新路径或字段 |
| --- | --- |
| `form` / `document` | 直接使用 `data` |
| `customer.customerName` 等四项客户资料 | 顶层同名字段 |
| `issuer.name` / `issuer.contact` / `issuer.logoUrl` | `issuerName` / `issuerContact` / `issuerLogoUrl` |
| `discount.discountRate` / `discount.discountedAmount` | 顶层或对应明细的 `discountRate` / `discountedAmount` |
| `totals.totalAmount` 等金额 | 顶层同名字段 |
| 更新请求 `reference.id` / `reference.expectedVersion` | `id` / `version` |
| 复制草稿 `reference.id` / `reference.expectedVersion` | `sourceId` / `sourceVersion` |
| `items[].sourceItemId` | `items[].id` |
| `skuIds[i]` | `items[i].skuId` |
| 模板更新、报价删除、模板删除的 `expectedVersion` | `version` |
| 模板套用的 `items[].product` | 报价行中的平铺商品字段、`skuId` 和两个版本 |

接口不再兼容旧包装。前端源码未在本次后端调整中修改。

## 数据库部署

本次仅调整协议和代码，不修改四张业务表，不需要数据库迁移。

- 新库依次执行 `schema.sql`、`data.sql`（或部署环境的菜单与角色初始化），以及 [报价权限脚本](sql/20261122_quotation_permissions.sql)。
- 已有库且尚无报价表，先执行 [报价建表脚本](sql/20261122_quotation.sql)，再执行权限脚本。
- 使用旧版报价表的库执行 [报价升级脚本](sql/20261122_quotation_upgrade.sql)，保留 `contact_email` 的既有值。
- 开单方配置为 `quotation.issuer.name`、`quotation.issuer.contact`、`quotation.issuer.logo-url`。名称默认「建绿新材」。

交互及权限背景见 [报价实施计划](quotation-implementation-plan.md)，接口字段以本文为准。
