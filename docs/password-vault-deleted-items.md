# 密码库已删除条目接入契约

更新日期：2026-10-22。对应实施计划 R1、R2 的后端变更；前端 R3、真实环境联调和发布 R4 仍需分别执行。本仓库未修改前端、运行配置或业务数据库。

## 生命周期与准入

用户 2026-10-21 最新确认及聊天「重新设计密码库交互与UI」最终原型覆盖旧计划的组回收条款：**已删除集合只收纳密码，账号组直接删除，成员密码保留。**

密码和账号组继续作为不透明密文保存在 `vault_item`。密码删除只改变服务端删除状态，不改变 itemId、contentVersion 或密文；保留期为从数据库删除时刻起固定 **720 小时**，与时区、夏令时和 `purge-delay` 无关。恢复要求锁后数据库时间严格早于 purgeAt。到期后立即停止列表、详情和恢复访问，清理器延迟不会延长资格。

账号组调用独立的直接删除操作，物理删除目标及同库同 itemId 的历史代副本，不设置删除时间或进入保留集合，不支持组恢复。仍存在的正常组继续参与同步、备份及完整轮换。

GET `/vault/api/meta` 在未初始化、已初始化及历史幂等回执中均声明：

```json
{
  "writeItemSchemaVersion": 1,
  "lifecycleProtocolVersion": 1,
  "clientLifecycleVersionHeader": "X-Vault-Lifecycle-Version",
  "deletedItemRetentionDays": 30
}
```

所有 `/vault/api/**` 数据 POST 同时要求两个唯一、精确的请求头：

```http
X-Vault-Item-Schema-Version: 1
X-Vault-Lifecycle-Version: 1
```

生命周期头缺失、重复、逗号拼接或版本不支持均返回 409 `VAULT_CLIENT_UPGRADE_REQUIRED`。认证端点及 GET 不要求这些头。请求头不参与历史请求摘要，已有请求记录的字段顺序与摘要规则保持兼容。客户端应完整实现保留集合轮换、密码回收及组直接删除后再声明版本，不能只添加请求头。

下列接口均使用已有完整 MFA、authEpoch、CSRF 和精确 Origin 规则。2026-10-23 起，普通删除、永久删除、直接删除账号组及含删除批次还须提供绑定本次业务正文的单次 Passkey 证明，不能复用登录或通用近期认证；见[删除操作 Passkey 接入契约](password-vault-operation-passkey.md)。已删除查询和恢复不追加验证；轮换 start/resume/commit/cancel 和用户备份仍沿用已有近期认证要求。

## 查询和回执

所有路径以下省略 `/vault/api`。

| 接口 | 作用 |
| --- | --- |
| GET `/items` | 正常列表，排除已删除项 |
| GET `/items/deleted?cursor=...&pageSize=100` | 当前活动代未到期的已删除项，删除时间降序、itemId 升序 |
| GET `/items/deleted/detail?itemId=...` | 重新核实状态并返回只读详情、准确期限和服务端时间 |
| POST `/items/delete` | 原请求格式，仅用于密码移入已删除集合 |
| POST `/items/delete-group` | 直接删除正常账号组，不进入已删除集合，成员不变 |
| POST `/items/batch` | 原请求格式，DELETE 回收密码；DELETE_GROUP 直接删除组；逐项返回最终集合版本 |
| POST `/items/restore` | 恢复原条目 |
| POST `/items/purge` | 立即永久删除已删除条目及同库同 itemId 的 RETIRED/ABORTED 代副本 |

已删除分页的 pageSize 默认为 100，范围 1–100。外层为 `generation`、`collectionRevision`、`serverTime`、`items`、`nextCursor`；末页 nextCursor 明确为 null。每项包含原六个密文字段 `itemId/contentVersion/rowVersion/keyEpoch/wrappedDek/encryptedItem` 以及 `deleted=true/deletedAt/purgeAt`。详情额外返回 `generation/collectionRevision/serverTime`。

时间使用带时区的 ISO 8601 字符串。正常、已删除、SOURCE、STAGING 游标不能混用；均应视为不透明值。已删除游标绑定用户、库、活动代、集合版本、首屏数据库时间和首屏集合的最早到期边界。跨越该边界，即使清理器还没运行，也返回 409 `VAULT_VERSION_CONFLICT`；客户端从首屏重新同步。

删除成功回执包含 `itemId/deleted=true/rowVersion/contentVersion/collectionRevision/deletedAt/purgeAt/serverTime`。rowVersion 为删除后的值。相同幂等键重试返回原回执，不重新计算期限。批量回执中的 collectionRevision 均为整批完成后的最终值。

恢复密码、永久删除已删除密码，以及直接删除账号组共用以下请求格式；不传入密文、删除时间、操作人或归属字段：

```json
{
  "itemId": "规范小写 UUID",
  "generation": 1,
  "expectedRowVersion": 2,
  "idempotencyKey": "规范小写 UUID"
}
```

恢复成功返回 `itemId/deleted=false/rowVersion/contentVersion/collectionRevision/serverTime`。原 ID、密文、内容版本、创建信息保持不变，删除字段清空，行版本和集合版本递增。再次删除从新的删除时间起算。

永久删除成功返回 `itemId/permanentlyDeleted=true/collectionRevision/purgedAt/serverTime`，不返回密文或虚构行版本。不删除其他条目、成员账号或密钥槽。已到期但尚未清理的已删除行仍可由该请求清理；正常行不能直接永久删除。此操作不会删除用户已经下载的备份或其他设备持有的副本。

账号组直接删除要求记录仍正常、代次与行版本匹配，并沿用完整会话、写锁及归属检查。成功回执同样为 `itemId/permanentlyDeleted=true/collectionRevision/purgedAt/serverTime`，没有删除后行版本或 30 天期限；幂等操作为 `DELETE_GROUP`，沿用 `ITEM_DELETE` 审计。批量 DELETE_GROUP 的字段要求与 DELETE 一致，但返回直接删除回执，按组只递增一次集合版本。批内其他项失败时，组的当前及历史副本删除一并回滚。

这些操作均通过原幂等机制持久化最小回执。响应丢失时保留完全相同的请求重试；同键不同正文返回幂等冲突。成功重放先返回历史回执，客户端随后重新同步，不能将历史回执视为当前状态。

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `VAULT_ITEM_ALREADY_DELETED` | 409 | 不同幂等键重复删除，不刷新期限 |
| `VAULT_ITEM_NOT_DELETED` | 409 | 对正常或已经恢复的条目执行恢复/永久删除 |
| `VAULT_ITEM_EXPIRED` | 409 | 本人的已删除行尚存，但详情/恢复已过期限 |
| `VAULT_ITEM_LIMIT_REACHED` | 409 | 正常条目已满 2,000，创建或恢复失败；删除期限不变 |
| `VAULT_VERSION_CONFLICT` | 409 | 代次、行版本、集合版本或分页时间边界发生变化 |
| `VAULT_WRITE_LOCKED` | 423 | 正在轮换或恢复，不允许删除、恢复或永久删除 |
| `VAULT_NOT_FOUND` | 404 | 已清理、不存在或错误归属，统一响应 |

## 轮换、恢复与备份

普通改主密码保持当前 VMK，不触碰已删除内容或期限。完整轮换（包括使用 RK 解锁后的换钥）在 start 持锁后冻结正常条目和当时未到期已删除项。存在可恢复已删除项时不允许空库备份恢复。正常条目上限仍为 2,000；轮换的预期集合没有该总量上限，不能截断保留记录。

start 成功后使用 GET `/rotations/items?rotationId=...&scope=SOURCE&cursor=...&pageSize=100` 下载冻结源集合。省略 scope 默认 `STAGING`，供上传后验证使用。两者外层带 `rotationId/generation/scope/serverTime/items/nextCursor`，条目包含删除状态与原期限；SOURCE/STAGING 游标绑定任务、代和范围，不能互换。SOURCE 只按冻结清单读取，轮换期间自然到期不会导致缺项。下载、上传和验证均受原 30 分钟任务期限及会话绑定约束。

上传继续使用原 `RotationBatch` 和六字段 `Item`，无需也不得由客户端控制删除状态；服务端从源行复制 is_deleted、gmt_deleted、delete_user。客户端须为每条内容重新生成密钥和密文。新代 rowVersion 从 1 开始，删除时间和 purgeAt 保持不变。服务端不能验证客户端是否实际执行了完整加密。

staging-manifest 数组和摘要格式不变，但条目清单包括全部暂存行，含已删除项。commit 先核对完整集合、版本、删除元数据及摘要，再在同一事务处理期间到期的源/目标行，避免缺项绕过或到期复活。取消/超时同样在清理暂存后立即清理源库到期行。清理器在任务进行中不破坏冻结集合；到期详情和恢复始终拒绝。

用户 `/backup-snapshot` 仍仅含正常条目，备份 v1 格式不变。前端须提示备份不包含已删除项；不得将用户备份当作轮换 SOURCE。

组关系在客户端密文内维护：删除密码时沿用原子批次解除组引用，恢复密码回到全部密码，提示手动归组；已删除或缺失成员只能显示不可用。删除组只删除该组，不更新或删除成员密码、TOTP，不进入已删除列表，也不加入后续轮换保留集合。

**类型边界：** LOGIN / ACCOUNT_GROUP 保存在加密内容中，后端没有解密密钥，不能验证调用方声明的真实类型或按类型过滤密文。客户端必须在 Worker 解密并校验类型后分流：LOGIN 使用 DELETE 或 `/items/delete`，ACCOUNT_GROUP 使用 DELETE_GROUP 或 `/items/delete-group`；已删除查询、详情、恢复和 purge 按密码流程使用，不能继续用 DELETE 删除组后仅在 UI 隐藏。服务端约束操作行为、归属、版本和事务，不宣称能够识别密文中的类型。若此前测试或旧客户端留下组的软删除行，不能通过 SQL 自动识别或清除；需由已授权客户端解密识别并确认处理。

## 清理和发布

每分钟任务每轮最多选择 20 个库，每库独立事务、至多清理 100 条到期删除行。按账户→库→条目顺序加锁，跳过已锁库并在锁内重新检查期限和轮换。实际删除后每库递增一次集合版本并记录系统 `ITEM_PURGE` 审计；失败整库事务回滚，下轮重试，其他库可继续。剩余过期记录记录最早到期时间，失败告警不含身份或密文。

`vault.purge-delay` 仅控制旧槽和 RETIRED/ABORTED 代的原清理策略，不再控制活动代条目保留期。已确认切代的旧代副本仍可按旧策略清理；可恢复内容已迁入新代，不依赖旧密钥。

发布顺序：

1. 暂停旧清理器和新建轮换，等待已有轮换提交或取消；本次没有操作运行环境。
2. 核对历史删除行、原删除时间和活动代密钥；旧任务已清除的密文或无法再解密的数据不能补回。
3. 短暂停止数据写入，按顺序执行独立增量 [20261021_password_vault_deleted_items.sql](sql/20261021_password_vault_deleted_items.sql)、[20261021_password_vault_group_delete.sql](sql/20261021_password_vault_group_delete.sql)。前者新增错误/审计白名单、删除查询索引，并放宽轮换总量 CHECK；后者补充组直接删除的幂等操作白名单。不重建表，不改写期限。
4. 协调部署支持生产生命周期版本 1 的后端与客户端，再恢复写入及新清理器。仍保留的历史行直接按原删除时间纳入/到期处理。
5. 回退只能使用兼容生命周期的版本，不能恢复 24 小时条目清理器。执行真实浏览器、跨设备和运维验收后再发布结论。

前端需完成计划 R3 中适用密码的流程，并将账号组单条及批量删除切换为直接删除操作：已删除导航和独立索引、只读详情、准确永久删除时间、剩余天数、恢复/永久删除及二次确认、三语文案、锁定/到期秘密清理，并支持 SOURCE 和超过 2,000 条保留集合。当前仓库边界禁止直接修改前端。

## 本次验证

已执行 `mvn -Dtest=VaultProtocolTest,VaultItemBatchProtocolTest,VaultHttpContractTest,VaultPersistenceTest,VaultIdentityServiceTest test`：111 项全部通过（HTTP 45、真实 PostgreSQL 事务 44、纯协议 20、身份回归 2），无失败、错误或跳过；编译及 MapStruct 映射生成通过。

数据库测试只启动一次性回环 PostgreSQL，并顺序执行原 DDL 和对应增量迁移。新增覆盖 720 小时边界、锁等待跨过到期、篡改期限游标、并发恢复/永久删除、完整密文保留、审计失败回滚、容量满时拒绝恢复、每轮 100 条清理及失败重试、轮换期间到期的提交/取消处理、2,001 条保留集合完整轮换，以及流式 staging-manifest 与原数组摘要格式一致。测试不读取业务库，不代表前端流程、真实设备或线上清理任务验收完成。

## 删除原型核对与账号组修正

已读取关联聊天最终确认、`vault-deleted-design/interaction-spec.md`、交接提示及密码详情/永久删除确认参考图。对应关系如下：

| 原型行为 | 后端接口与约束 | 核对结果 |
| --- | --- | --- |
| 密码移入已删除，显示准确期限 | DELETE、deletedAt/purgeAt/serverTime，固定 720 小时 | 支持 |
| 独立列表、只读详情、剩余天数 | deleted 分页与 detail 返回完整密文及期限；摘要、搜索及倒计时由客户端本地处理 | 支持，前端待接入 |
| 直接恢复且无额外近期 Passkey | `/items/restore`，完整 MFA、CSRF、Origin，原 ID/密文不变 | 支持 |
| 确认后永久删除；取消不请求 | `/items/purge`，服务端原子物理删除；确认、禁用和本地秘密清理由前端处理 | 支持，前端待接入 |
| 到期、404、容量、版本冲突、423、结果未知 | 既有错误码、锁后数据库时间及相同幂等键回执 | 支持 |
| 账号组直接删除，成员和已删除密码集合不变 | `/items/delete-group` 或批内 DELETE_GROUP，仅删除目标 ID | 本次补齐；旧 DELETE 调用必须切换 |

新增真实数据库回归覆盖组当前/历史副本直接删除、成员密文不变、已删除列表为空、不可恢复、幂等重放、普通密码与组混合删除的不同回执、批内版本冲突回滚、审计失败回滚以及 MFA/authEpoch/轮换写锁。HTTP 和纯协议测试覆盖新路由、参数及 DELETE_GROUP 字段约束。

本次没有修改前端、参考原型或执行目标数据库迁移；不将设计预览当作真实联调结果。
