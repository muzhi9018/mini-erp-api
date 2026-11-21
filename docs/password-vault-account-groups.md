# 密码库账号组：后端契约与前端接入

创建日期：2026-10-19；更新日期：2026-10-22。依据关联聊天「重新设计密码库交互与UI」的分组简化规格及用户最新确认。本文替代 2026-10-18 版的分组邮箱绑定、反向关系及联动录入要求；账号组只保留“名称 + 成员账号”，并直接删除，不进入已删除。已补齐后端操作、接入说明和回归验证；前端源码未修改，业务数据库未执行迁移。

## 本轮调整边界

- 创建组只填写名称，允许空组；添加已有账号只更新组成员。
- 在组内新建账号复用原有新增流程，只预选所属组，不复制组名、邮箱、密码或 TOTP。
- 邮箱和其他平台账号都是独立 LOGIN，详情复用原有账号功能。分组不增加绑定邮箱区块、反向关联列表、同组关联卡片或跳转，不根据同名或相同邮箱推断关系。
- 重命名、添加或移出成员只修改组；删除组直接物理删除组密文，保留所有成员账号及 TOTP，不进入已删除列表。
- 原有账号的新增、编辑、密码、TOTP、二维码、安全、备份恢复及轮换功能保持现状。本轮不重构账号内容模型或批量清理用户数据。

后端现有接口只接收密文，从未要求组邮箱或成员邮箱，不存在需要解除的邮箱必填校验。首次生产发布的内容版本统一为 1；组编辑继续复用原接口，组删除使用新增 `/items/delete-group` 或批内 `DELETE_GROUP`，不新增表。需执行本文末尾的幂等白名单增量；界面及 Worker 的分流由前端配合实施。

## 存储选择

账号组作为独立的加密内容存入现有 `vault_item`，与密码条目各自使用稳定 UUID、独立 DEK、原有密文盒和 AAD。业务类型、组名和成员引用都在 `encryptedItem` 内，服务端不创建明文组表、成员表或搜索索引。

正常账号组参与现有分页同步、密文快照、完整轮换及从备份恢复；删除组使用直接删除，不进入密码的 30 天保留集合。无需第二套密钥。组不是权限边界。组、密码共享每库 2000 条有效密文记录的容量；四条密码加一个组占五条记录，界面密码数只统计 LOGIN。

原账号组版本需在已执行初始密码库建表脚本的数据库上执行 [增量脚本](sql/20261018_password_vault_account_groups.sql)，为 `vault_idempotency_record.operation` 增加 `BATCH_ITEMS`。脚本只扩展 CHECK 白名单，不改写密文；已应用该脚本的环境无需为本次简化再次执行。新环境按初始脚本、增量脚本的顺序执行，初始 CREATE TABLE 脚本不能在已有表上重复执行。

## 生产内容版本 1

GET `/vault/api/meta` 声明 `readableItemSchemaVersions=[1]`、`writeItemSchemaVersion=1`、`minimumClientSchemaVersion=1`。所有 `/vault/api/**` 写请求必须携带唯一的 `X-Vault-Item-Schema-Version: 1`。缺失、非 1 或重复声明返回 HTTP 409 / `VAULT_CLIENT_UPGRADE_REQUIRED`。认证和 GET 不要求该头。

此版本是加密明文结构的版本，和外层 formatVersion=1、编辑次数 contentVersion、并发 rowVersion 相互独立。原密文盒、派生档案、AAD 及 16 KiB 明文 / 16400 字节密文上限保持原协议。

以下是客户端加密前的数据契约，**不得作为明文请求体发给服务器**。有限业务类型使用 `itemType` 的 LOGIN / ACCOUNT_GROUP 两个编码；未知 schema 或类型禁止编辑、整库重写及轮换提交，不能当 LOGIN 处理。

### 独立账号

LOGIN 沿用已经实现的账号内容、表单和校验，原六字段、可选 TOTP 及其他已有通用字段不因分组而改变。登录名可以是邮箱地址，无须特殊处理；邮箱账号没有父节点或绑定中心的身份，不设置组共用密码或 TOTP。

生产内容版本 1 包含完整 LOGIN、可选 TOTP 及 ACCOUNT_GROUP 结构，与开发期的六字段 v1、TOTP v2、账号组 v3 编号不等价。开发期数据及备份需由客户端解密校验并转换后使用；服务端不会重写密文。不因查看、建组、重命名或成员管理批量重写 LOGIN，不新增分组邮箱必填条件。

旧分组设计中的 `defaultBoundEmail`、`boundEmail`、`emailItemId` 等绑定元数据不再参与分组校验、展示、关系推断或联动保存，新分组流程不再主动生成这些字段。原密文可以兼容保留，不进行存量清理；原有账号通用字段中的邮箱或用户填写的信息也不自动改写或删除。

### 账号组

```json
{
  "itemSchemaVersion": 1,
  "itemType": "ACCOUNT_GROUP",
  "name": "常用账号",
  "memberItemIds": []
}
```

- 组 ID 使用外层 itemId，不在明文里重复存储。name 非空，创建时不提供或自动补齐任何绑定邮箱字段。
- memberItemIds 为不重复的 LOGIN UUID 数组，可以为空；不引用组，不嵌套组，不内嵌账号密码或 TOTP。邮箱与其他平台账号只作为平级成员。
- 不在 LOGIN 再存一份 groupIds。沿用现有同一 LOGIN 可被多个组引用的归属规则，本轮不增加或收紧跨组能力，也不增加权限能力。
- 按 JSON 的实际 UTF-8 字节数检查 16 KiB 限制，包括全部成员 UUID；不能假定一个组可以容纳全库 2000 条引用。达到上限时提示拆组，不截断引用。

上述明文格式由客户端在 Worker 内校验、加解密及建立本地索引；服务器只能验证密文盒和外层版本，无法验证组名、引用目标类型、引用完整性或客户端版本声明是否属实。

## 原子变更接口

POST `/vault/api/items/batch`，沿用密码库 Cookie、完整 MFA、Origin、JSON、CSRF 和版本头。普通组编辑无需额外要求近期认证；备份、轮换等原有敏感操作仍要求近期 Passkey。含 DELETE 或 DELETE_GROUP 的批次及单独删除组自 2026-10-23 起要求本次操作 Passkey 证明，绑定完整有序业务正文；见[删除操作 Passkey 接入契约](password-vault-operation-passkey.md)。

```json
{
  "generation": 1,
  "keyEpoch": 1,
  "expectedCollectionRevision": 1,
  "changes": [
    {
      "action": "CREATE",
      "itemId": "00000000-0000-4000-8000-000000000001",
      "expectedRowVersion": null,
      "contentVersion": 1,
      "wrappedDek": { "formatVersion": 1, "algorithm": "A256GCM", "iv": "合法Base64url", "ciphertext": "合法Base64url" },
      "encryptedItem": { "formatVersion": 1, "algorithm": "A256GCM", "iv": "合法Base64url", "ciphertext": "合法Base64url" }
    }
  ],
  "idempotencyKey": "00000000-0000-4000-8000-000000000099"
}
```

示例中的密文占位符必须替换为真实加密结果。每批 1–100 项，总请求体不超过 1 MiB；不接受同批同一 itemId 多次操作。创建空组只提交一条组 CREATE；在组内新建账号时，先完成原有账号表单，再原子提交账号 CREATE 和组 UPDATE，不增加专用邮箱或平台录入向导。

| action | 字段要求 | 行为 |
| --- | --- | --- |
| CREATE | expectedRowVersion=null，contentVersion=1，两个密文盒必填 | 创建独立 UUID；已占用的 UUID 返回 409，包括尚未清理的逻辑删除记录 |
| UPDATE | expectedRowVersion 为当前行版本，contentVersion 为当前内容版本+1，两个密文盒必填 | 仅替换该条密文，rowVersion+1 |
| DELETE | expectedRowVersion 必填，contentVersion / wrappedDek / encryptedItem 均为空 | 密码进入 30 天保留集合，rowVersion+1 |
| DELETE_GROUP | expectedRowVersion 必填，contentVersion / wrappedDek / encryptedItem 均为空 | 直接删除正常组及同库历史副本，成员不变，不返回行版本或保留期 |

服务端锁顺序仍为账户→库→条目。同批 generation、keyEpoch、expectedCollectionRevision 必须匹配当前状态；逐项检查归属、存在性、行版本和内容版本。轮换期间返回 423。内部先删除释放容量，再按其余请求顺序写入，支持满库时原子替换；外部结果仍按原请求顺序排列。

任何一项失败，密文、逻辑删除、集合版本、审计和幂等回执全部回滚。每个变更沿用单项操作的集合版本递增规则，成功批次使 collectionRevision 增加 changes 数量。响应仅含公开回执，不回传密文或关系：

```json
{
  "code": 200,
  "success": true,
  "message": "成功",
  "data": {
    "generation": 1,
    "collectionRevision": 2,
    "results": [
      {
        "itemId": "00000000-0000-4000-8000-000000000001",
        "rowVersion": 1,
        "contentVersion": 1,
        "collectionRevision": 2
      }
    ]
  }
}
```

密码 DELETE 逐项回执带 deleted=true、itemId、删除后的 rowVersion/contentVersion 及 deletedAt/purgeAt/serverTime；组 DELETE_GROUP 回执带 permanentlyDeleted=true、itemId、purgedAt/serverTime，不返回虚构行版本或保留期。所有逐项回执的 collectionRevision 统一为整批最终版本，不代表可见的中间状态。

## 并发与重试

客户端必须先读取完整、版本一致的当前集合，再建立关联或处理删除。分页中集合变化会使游标失效，须重新同步，不能用半份列表判断某条不存在。

保存时同时提交当前 collectionRevision 与各被修改行的 rowVersion，避免另一设备增加引用后本设备用旧关系覆盖。HTTP 409 版本冲突后重新同步、重新核对关系并由用户解决冲突，不自动丢弃并发成员。

UUID、密文及 idempotencyKey 在首次发出前生成并保留。超时或响应丢失时，用**完全相同**请求重试：已有成功回执会在集合版本校验前返回，既不重复创建，也不重复审计。同键内容不同返回 `VAULT_IDEMPOTENCY_CONFLICT`；失败事务不保留成功回执，可在同步后修正请求再发出。业务结果已提交后不可把超时笼统标为全部失败。

## 业务操作对应关系

| 用户操作 | 客户端需要提交的加密变更 |
| --- | --- |
| 创建空组 | CREATE 一条 ACCOUNT_GROUP，只含名称与空 memberItemIds |
| 添加已有账号／移出成员 | UPDATE 组的 memberItemIds；不重写任何 LOGIN |
| 在组内新建账号 | 原有账号表单生成 LOGIN，原子提交该账号 CREATE 与组 UPDATE；不复制或改写其他成员 |
| 重命名组 | 只 UPDATE 组名，保留成员；不改变任何账号内容 |
| 查看邮箱或其他成员 | 使用现有独立账号详情，不产生写入或反向关系 |
| 修改单条密码、TOTP 或其他账号字段 | 沿用原有账号保存流程；分组不触发其他账号或组的联动写入 |
| 删除组 | 只 DELETE_GROUP 对应组，或调用 `/items/delete-group`；不删除任何成员密码或 TOTP，不进入已删除 |
| 删除独立账号 | 沿用已有账号删除流程及组成员引用维护，不扫描或改写其他账号的弃用邮箱元数据 |

客户端只在用户确认后添加已有成员，组列表保持“组 → 账号”两层。搜索沿用现有本地能力，不增加同邮箱提示或自动归组；详情只展示当前账号，通过原有组列表切换成员。

原有账号删除需要维护的组成员引用超过单批数量或体积上限时，继续按已有流程分批解除组引用并同步，每批保存后更新版本；最后重新检查完整集合，再原子删除目标。中间只解除分组，不提前删除密码。不能把多个批次声称为一个事务，也不能静默漏掉超限引用。遇到历史悬空成员显示成员不可用，不创建副本或级联删除其他账号。

## 备份、轮换与前端交付边界

GET items 返回所有密文记录，客户端解密后区分 LOGIN / ACCOUNT_GROUP。“全部密码”按 LOGIN 的独立 ID 去重；账号组视图按 memberItemIds 展示，不因成员登录名是邮箱而增加特殊关系。

原 backup-snapshot 和轮换/恢复接口必须保留**全部**密文记录，包括旧组、账号及兼容保留的元数据。完整轮换清单不得只选择 LOGIN；恢复沿用原 UUID，使组成员保持可对应。恢复继续复用既有兼容处理、空库规则和 ownerSubject 绑定约束，不借此清理旧字段或改变账号内容。

后端仅接受生产内容版本 1 与生命周期版本 1 的写入声明，开发期版本 2/3 的声明会被拒绝。首次上线账号组时仍须应用原增量 SQL 并协调支持生产内容版本 1 的前端；前端必须实际完成 typed schema 解析、Worker 分流、组草稿、原子提交、成员校验、本地搜索、完整备份恢复及轮换后，才能声明版本 1，不能只改请求头。GET meta 是当前兼容声明的权威入口，历史幂等回执里的声明不作为版本协商依据。

后端自动化覆盖协议/HTTP 边界，以及一次性 PostgreSQL 中的批量回滚、幂等、并发、容量、组删除不级联、密文快照、缺项轮换拒绝和恢复。本轮补充无邮箱空组、组名和成员变更不改写已有账号、旧绑定元数据密文保留的回归。服务端用例验证不透明密文和 ID 集合的生命周期，不能替代前端上述业务操作、真实加解密、TOTP 或浏览器交互验收。

2026-10-18 最终验证命令：`mvn -Duser.home=/tmp/mini-erp-vault-test-home test`，70 项通过，0 失败、0 错误、0 跳过；编译和 MapStruct 映射生成通过。数据库测试仅使用回环地址的一次性 PostgreSQL，不读取 dev/prod 连接配置。

2026-10-19 本轮验证：

```bash
mvn -Duser.home=/tmp/mini-erp-vault-test-home -Dtest=VaultHttpContractTest,VaultPersistenceTest,VaultProtocolTest,VaultItemBatchProtocolTest,VaultIdentityServiceTest test
```

74 项通过，0 失败、0 错误、0 跳过；编译和 MapStruct 映射生成通过。HTTP 回归确认单项及批量创建只要求现有密文协议字段；数据库回归使用加密的无邮箱空组样本，逐步执行添加成员、重命名、移出成员与删组，逐次比较既有 v2/v3 账号的完整密文和版本，确认密码、TOTP、备注及旧元数据未被改写；旧组密文及成员在快照中原样保留。测试只使用本机一次性 PostgreSQL，未连接业务数据库。前端表单、详情及 Worker 行为不在这些后端用例的验证范围内。

2026-10-21 最新确认覆盖旧计划中的组回收条款：只有密码进入 30 天保留集合，账号组使用 `/items/delete-group` 或批内 `DELETE_GROUP` 直接删除，不影响成员。所有数据 POST 还需唯一 `X-Vault-Lifecycle-Version: 1`。类型仍仅存在密文内，客户端必须在 Worker 解密校验后选择正确操作；后端不声称能够识别密文类型。需在生命周期增量后执行 `20261021_password_vault_group_delete.sql` 扩展幂等操作白名单，旧迁移不重跑。当前契约及发布步骤见 [已删除条目接入契约](password-vault-deleted-items.md)。
