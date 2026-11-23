# 报价接口联调

所有路径使用 ERP 登录态，响应沿用 `JsonResult<T>`。先检查 `success`，失败时使用 `message`。分页从 `pageNum=1` 开始，默认 `pageSize=15`，数据从 `data.records` 读取。JavaScript 中的业务 ID 保持字符串。后端沿用项目的序列化规则，超过 JavaScript 安全整数的长整型以字符串返回，请求可提交字符串 ID。

## 选品与共用编辑页

`GET /quotation/sku/options` 支持 `keyword`、`categoryId`、`specs[0].specId`、`specs[0].specValueId` 或 `specs[0].minimum` / `specs[0].maximum`，以及 `referenceSaleCurrency=CNY|TWD`。只返回启用且有有效参考售价的 SKU，零价和零库存可选。用 `skuId` 精确查询某一行商品，核对币种变化时省略币种条件。单独维护模板时，用相同参数调用 `GET /quotation/template/personal/sku/options` 或 `GET /quotation/template/public/sku/options`，分别需要该范围的模板创建或修改权限。

| 编辑入口 | 编辑页数据 | 预览与保存 |
| --- | --- | --- |
| 新建 | 初始化标题「报价单」和当前时间。首行确定币种，清空明细后可以重选。 | `POST /quotation/preview`、`POST /quotation/create` |
| 套用模板 | 填入模板标题、商品行（含行备注）和两级优惠。直接用模板初始化新报价时填入模板整单备注。对进入替换确认的草稿，保留已有报价时间、客户资料和整单备注。 | 同新建 |
| 从旧报价新建 | `POST /quotation/copy` 提交 `{"id":"2193456789012345678","clearCustomer":true}`，从 `data.form` 回填。 `false` 保留客户资料。 | 同新建，提交表单中的原报价引用 |
| 修改 | `GET /quotation/detail?id=…`，从 `data.form` 回填。固定原币种，使用返回的正式单号。 | `POST /quotation/update/preview`、`POST /quotation/update` |

普通新增表单示例。 `quantity`、`discountRate`、`discountedAmount` 建议传十进制字符串，避免前端浮点舍入。折数乘以 10 后传输，`"95.55"` 表示 9.555 折。无优惠时省略 `discount`，固定优惠后金额使用 `{"discountedAmount":"100.00"}`。

```json
{
  "title": "展厅储物设备报价",
  "quotedAt": "2026-11-20T14:30:00+08:00",
  "customer": {
    "customerName": "演示客户",
    "contactName": "王女士",
    "contactPhone": "0900-000-000",
    "shippingAddress": "演示地址 1 号"
  },
  "remark": "交期另行确认",
  "discount": {"discountRate": "95.00"},
  "items": [
    {
      "skuId": "2193456789012345678",
      "productVersion": 0,
      "skuVersion": 0,
      "quantity": "2.00",
      "discount": {"discountRate": "90.00"},
      "remark": "依客户要求贴标签"
    },
    {
      "skuId": "2193456789012345679",
      "productVersion": 0,
      "skuVersion": 0,
      "quantity": "1.50",
      "discount": {"discountedAmount": "100.00"}
    }
  ]
}
```

复制和修改在顶层带上 `reference: {"id":"…","expectedVersion":0}`。沿用原行时该行只提交 `sourceItemId`、数量、优惠和备注。新增或替换行时提交当前 `skuId`、`productVersion`、`skuVersion`、数量、优惠和备注。保存后的 ID、快照、价格、币种和成交金额都由服务端确定。新增允许省略标题或时间，由服务端取默认值。修改须提交标题和时间。详情与复制返回同顺序的 `form.items`、`document.items` 和 `skuIds`，其中 `skuIds` 供另存模板使用。清空可选客户字段、备注或优惠时可省略或传 `null`。

商品版本变化时，按项目统一失败响应读取 `message`，提示包含从 1 开始的明细行号。保留当前草稿，使用该行已有的 `skuId` 调用 `GET /quotation/sku/options` 查询最新资料。对比单价、币种、规格和数量精度，经用户确认后更新该行。查询无结果时提示移除或替换该 SKU。

## 列表及客户版

- `GET /quotation/list` 支持 `keyword`、`quoteNumber`、`title`、`customerName`、`quotedFrom=YYYY-MM-DD`、`quotedTo=YYYY-MM-DD`、`orderGenerated=true|false`。日期范围包含两端，按台北时间解释。列表的 `version` 可直接用于删除。
- `POST /quotation/create` 返回 `{id, quoteNumber}`。修改返回完整详情 `{id, version, orderGenerated, form, document, skuIds}`。 `POST /quotation/delete` 提交 `{id, expectedVersion}`。已转单报价可查看或生成新报价，页面隐藏修改、删除。
- 预览直接返回客户版数据，历史客户版使用详情的 `document`。新增或复制草稿预览的 `quoteNumber` 为空或缺省，显示「未编号」。已保存或修改预览显示正式单号。日期回传为台北时间并带偏移。
- 客户版中 `issuer` 提供 `name`、`contact`、`logoUrl`，`customer` 提供四项客户资料。 `items` 按顺序提供 `skuCode`、`productName`、`model`、`specificationText`、`unitName`、`quantityScale`、`quantity`、`unitPrice`、`discount`、`originalAmount`、`discountAmount`、`lineAmount` 和 `remark`。「金额」显示 `lineAmount`。
- `totals` 提供 `originalAmount`（商品原价合计）、`lineDiscountAmount`（商品优惠）、`subtotalAmount`（商品小计）、`orderDiscountAmount`（整单优惠）、`totalAmount`（报价总额）。以上例单价为 `129.345` / `80.00` 时，商品小计为 `"332.82"`，整单优惠为 `"16.64"`，报价总额为 `"316.18"`。使用服务端预览或已保存金额渲染截图。通过 Spring Security 返回的 `Cache-Control` 响应头包含 `no-store`。

## 模板

个人和公共接口前缀分别为 `/quotation/template/personal` 和 `/quotation/template/public`。两组都有 `GET /list`（可选 `keyword`）、`GET /detail?id=…`、`POST /create`、`POST /update`、`POST /apply`（请求 `{id}`）。创建和修改共用以下结构，修改额外提交顶层 `id` 与 `expectedVersion`。

```json
{
  "name": "门店陈列 · 常用组合",
  "title": "门店陈列报价",
  "discountRate": "95.00",
  "remark": "交期另行确认",
  "items": [
    {"skuId":"2193456789012345678","discountRate":"90.00","remark":"第一行"},
    {"skuId":"2193456789012345678","discountRate":"80.00"}
  ]
}
```

两类模板都可通过 `POST /delete` 删除，请求体为 `{id, expectedVersion}`。个人与公共删除权限分别为 `sales-manager:quotation-template:personal:delete` 和 `sales-manager:quotation-template:public:delete`。仅模板创建人可执行删除。列表和详情返回 `ownedByCurrentUser`，可与删除权限一起用于展示操作。

模板列表返回 `usable`、`issueCount`（有问题的行数）。详情明细返回 `skuId`、从 0 开始的 `sortOrder`、当前 `referenceSaleCurrency`、`product`（可空）、`issues` 及已保存的优惠和备注。币种分化时保留每行的当前币种。问题码用于显示对应语言的提示。

| 问题码 | 含义 |
| --- | --- |
| `SKU_MISSING` / `PRODUCT_MISSING` | SKU 或商品已删除或不存在 |
| `SKU_DISABLED` / `PRODUCT_DISABLED` | SKU 或商品已停用 |
| `PRICE_MISSING` / `CURRENCY_INVALID` | 缺少有效参考售价或币种 |
| `CURRENCY_CONFLICT` | 模板中可用商品的售价币种分化 |
| `OPTION_UNAVAILABLE` | 当前选品资料不可用 |

套用返回 `{title, discountRate, remark, items}`，每行含完整 `product` 选品资料、`quantity: "1"` 和可选的 `discountRate`、`remark`。前端使用当前 SKU 两个版本构建报价行。另存模板时从草稿取出标题、SKU ID、折扣和备注，保持重复 SKU 的位置。

## 数据库部署

- 新库按惯例执行 `schema.sql`、`data.sql`（或部署环境的系统菜单与角色初始化），之后执行 [报价权限脚本](sql/20261122_quotation_permissions.sql)。
- 已有库且尚无报价表，先执行 [报价建表脚本](sql/20261122_quotation.sql)，再执行报价权限脚本。
- 已执行旧版报价建表定义的库执行 [报价升级脚本](sql/20261122_quotation_upgrade.sql) 和报价权限脚本。升级为已有模板补「报价单」标题，并保留 `contact_email` 的已有值。该可空列供后续业务使用，当前报价接口的客户资料沿用四项展示字段。
- 权限脚本为现有 ERP 根菜单添加报价管理、报价单列表、个人模板和公共模板入口，初始授权给 `SUPER_ADMIN`。其他角色通过 ERP 角色授权分配菜单和按钮权限。公共模板详情对已登录 ERP 用户开放，列表需要公共模板查询权限。
- 开单方部署属性为 `quotation.issuer.name`、`quotation.issuer.contact` 和 `quotation.issuer.logo-url`。名称默认「建绿新材」。菜单组件按权限脚本中的 `path`、`component` 接入前端对应页面，客户版编辑器使用独立布局。

授权、路由与验收用例见 [报价实施计划](quotation-implementation-plan.md)。
