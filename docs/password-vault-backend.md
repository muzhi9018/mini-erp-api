# 密码库后端实现与联调

截至 2026-10-17，后端已有认证隔离、HTTP 错误契约、Passkey 遗失后的身份恢复，以及轮换提交与会话撤销一致性的实现。代码同时提供初始化、密文 CRUD、主密码槽替换、轮换暂存、空库恢复和一致性密文快照接口。本次补齐 TOTP 内容版本的客户端写入准入、meta 协议声明、删除用户保护及可复现的 HTTP/数据库事务测试；部署与安全验收仍需单独完成。

本次只修改后端实现、测试和说明，不修改前端或 NGINX 配置。服务端不生成或接收 MP、AK、RK、KEK、VMK、DEK 或条目明文，浏览器加解密、恢复材料下载、逐条解密验证和页面交互由前端负责。账号 TOTP 的种子和参数整体封装在条目密文中，复用现有 CRUD、轮换和快照接口，不新增服务端种子表或动态码计算接口；密码库自身的 Passkey 用于身份认证，不通过 PRF 解封 VMK。

2026-10-17 的 TOTP 准入变更未调整数据库结构。2026-10-18 账号组扩展需要下述增量 SQL；未建库环境先执行原建表脚本，再执行增量脚本。没有在业务数据库执行迁移。

## 2026-10-22 首次生产版本

密码库首次生产发布统一采用版本 1：`ITEM_SCHEMA_VERSION=1`、`LIFECYCLE_PROTOCOL_VERSION=1`，meta 的可读内容版本为 `[1]`，写入及最低客户端内容版本均为 `1`。密文盒 `formatVersion` 和备份格式原本已为 `1`。生产内容版本 1 包含当前完整 LOGIN、TOTP、ACCOUNT_GROUP 能力，生命周期版本 1 包含密码回收、恢复、永久删除及账号组直接删除。

开发期内容版本 2/3、生命周期版本 2 不再作为生产协议声明。前端须同步请求头、Worker 内容结构的读写版本及备份恢复校验，不能仅修改请求头。服务端无法改写密文内的版本；如需保留开发期数据或备份，须由客户端解密、校验并按生产格式重新加密后使用。本次不改写数据库、运行中的版本计数或前端文件。

## 2026-10-18 账号组扩展

当前账号组扩展纳入生产内容版本 1，复用 vault_item 保存加密组与账号关系，新增原子变更接口 POST `/vault/api/items/batch`（1–100 项、1 MiB）。无需新增表；需要执行 `docs/sql/20261018_password_vault_account_groups.sql` 扩展幂等操作约束。

当前 GET meta 声明 readableItemSchemaVersions=[1]、writeItemSchemaVersion=1、minimumClientSchemaVersion=1，所有数据写请求必须使用唯一 `X-Vault-Item-Schema-Version: 1`。具体内容结构见 [账号组接入契约](password-vault-account-groups.md)。前端必须完整支持生产内容版本 1 后再声明；本次未修改前端或执行业务库迁移。

2026-10-19 简化要求以 [账号组接入契约](password-vault-account-groups.md) 为准：组只保留名称与成员，允许空组，取消分组邮箱绑定、反向关系及联动录入。后端原本只接收密文，没有邮箱必填校验，因此继续使用现有接口，无需新增迁移。成员管理及删组不改写账号密文，原有账号功能、TOTP、备份恢复和安全协议保持现状；界面及 Worker 的调整需前端配合，本仓库未修改前端代码。

## 2026-10-21 已删除条目扩展

已实现 720 小时保留、已删除列表/详情、原条目恢复、手动永久删除及到期批量清理；完整轮换包含可恢复已删除项，空库恢复不能覆盖这些内容。所有数据 POST 另需唯一 `X-Vault-Lifecycle-Version: 1`，内容版本头为 1。接口、SOURCE 下载、回执和发布步骤见 [已删除条目接入契约](password-vault-deleted-items.md)。新增独立增量 `docs/sql/20261021_password_vault_deleted_items.sql`；其后执行 `docs/sql/20261021_password_vault_group_delete.sql`。按用户最新确认，账号组改用 `/items/delete-group` 或批内 `DELETE_GROUP` 直接删除，不进入已删除，成员密码保留。本次没有迁移业务数据库或修改前端。密码库相关五组测试 111 项全部通过，含一次性 PostgreSQL 上的增量迁移和事务验证；详见接入契约中的本次验证范围。

## 2026-10-22 接口路径调整

认证接口统一使用 `/vault/auth` 前缀，数据接口统一使用 `/vault/api` 前缀，由 Controller 显式声明，不配置全局 `server.servlet.context-path`。安全链、Session 过滤器、写入版本检查、失败审计和重新认证响应均使用新路径，旧路径不再作为密码库接口提供。

调用方需同步调整这两个请求前缀。NGINX 可按 `/vault/` 分流，并将完整请求路径原样转发到后端，不剥离 `/vault`；本次未修改前端或 NGINX 配置。`vault.origin` 和 RP ID 仍只包含源及主机名，不能追加 `/vault`。`__Host-vault_session` 的 Cookie Path 继续为 `/`，满足 `__Host-` Cookie 的约束。

## 启用

`VaultProperties.enabled` 的属性默认值为 false，未显式启用时密码库路径返回 HTTP 503，不能回退到 ERP JWT 认证。但当前 `application.yaml` 已配置 `vault.enabled=true` 及开发 HTTPS 源和 RP ID；实际状态以环境覆盖后的配置为准，不能将属性默认值当作当前运行配置。配置存在不代表目标数据库已迁移或部署验收已完成。

1. 新环境在目标 PostgreSQL 数据库依次执行 `docs/sql/20261015_password_vault.sql`、`docs/sql/20261018_password_vault_account_groups.sql`、`docs/sql/20261021_password_vault_deleted_items.sql`、`docs/sql/20261021_password_vault_group_delete.sql`；已有表的环境只执行尚未应用的增量脚本。共 13 张 `public.vault_*` 表，未声明物理主键或外键；内部 ID 使用 IdWorker/ASSIGN_ID，数据库保留非空、唯一、CHECK 和必要索引。实体继承公共逻辑删除与审计字段；业务归属、跨表关联和操作人由 Service 控制。
2. 配置现有 PostgreSQL、Redis 连接，并在运行环境覆盖下列属性。首次启用前确保能使用对应的 HTTPS 域名。

```yaml
vault:
  enabled: true
  origin: https://vault.example.com
  rp-id: vault.example.com
  rp-name: Mini ERP Vault
  purge-delay: 24h
  redis-configuration: AUTO
```

对应环境变量为 `VAULT_ENABLED`、`VAULT_ORIGIN`、`VAULT_RP_ID`、`VAULT_RP_NAME`、`VAULT_PURGE_DELAY`、`VAULT_REDIS_CONFIGURATION`。`origin` 必须为唯一 HTTPS 源，不含路径或末尾斜杠；RP ID 固定为该主机名。启动时验证配置，错误配置不能开放接口。

3. Redis Session namespace 为 `mini-erp:vault:sessions`，挑战和限流键分别位于 `mini-erp:vault:challenge:*`、`mini-erp:vault:rate:*`。Redis 需要支持 GETDEL（Redis 6.2+）、Lua、索引会话以及过期事件。

   - `vault.redis-configuration=AUTO`（默认）：Spring Session 读取并补齐 `notify-keyspace-events`，运行账户需要 CONFIG GET/SET 权限。
   - `vault.redis-configuration=EXTERNAL`：应用不执行 CONFIG，由运维预配置通知，至少包含 `Egx`（键事件、普通命令事件、过期事件）。支持等价的更宽事件集合；运行账户仍需对应命名空间的读写、Lua、发布订阅权限。禁止 CONFIG 的账户可以正常启动。
   - 未知枚举值在启动绑定时失败，不自动切换模式；EXTERNAL 无法通过 CONFIG 检查运维配置，需按下述真实 Redis 测试验证通知和索引清理。

   当前复用现有 Redis 连接设施；是否配置独立实例或 ACL 用户由部署决定，专用命名空间不提供权限隔离。Redis Cluster 尚未完成验收。
4. 前端同源访问下面两个路径前缀，使用 Cookie，不携带 ERP JWT。本文不涉及代理部署配置。

SQL 未自动接入迁移工具；目标业务数据库是否已执行需单独核验，本次不执行业务数据库迁移。不能在已有同名表的库上重复执行整份 CREATE TABLE 脚本。

## 认证与会话

- `/vault/auth/**`、`/vault/api/**` 使用独立且优先匹配的 Spring Security 链。JWT 过滤器取消容器重复注册，并显式跳过这些路径。ERP 原 JWT 和 HTTP 200 业务错误行为保持兼容。
- 仅在这两个路径前缀注册 Redis Session 过滤器。Cookie 为 `__Host-vault_session`，`Secure`、`HttpOnly`、`SameSite=Lax`、`Path=/`，不设置 Domain。
- 空闲超时 15 分钟，绝对超时 8 小时。待 MFA 密码状态最多 5 分钟，身份恢复引导最多 10 分钟；敏感操作要求最近 5 分钟的 Passkey 验证。会话提升会更换 Session ID 和 CSRF token，前端必须使用响应中的新 token。
- 所有 POST 必须带精确匹配 `vault.origin` 的 Origin、`application/json` 和会话 CSRF header。先 GET `/vault/auth/options` 获取 `data.csrfHeader`、`data.csrfToken`，随后携 Cookie 发请求。无请求字段的 POST 发送 `{}`。
- 每次请求查询 PostgreSQL，检查系统用户启用且未删除、账户和库未删除以及 authEpoch；数据库故障拒绝授权。写操作在事务内重新加锁检查。系统密码校验期间锁住用户行，与重置、禁用、删除的事务顺序一致，避免旧密码登录继承新代次。
- 一般请求正文上限 64 KiB；轮换批次和空库恢复 start 上限 1 MiB。每批最多 100 条，正常条目最多 2000 条；轮换保留集合还包含未到期已删除项。认证请求按账户和连接来源分别限流。

### 首次绑定

`GET options → POST login → register/options → register/verify → authenticate/options → authenticate/verify → recovery-codes/issue → recovery-codes/confirm`。

`login` 接收系统账号、系统登录密码，只建立 `PASSWORD` 状态。复用现有 AuthenticationManager 验证，不签发 ERP JWT。首次注册只允许尚无有效 Passkey 且未初始化库的用户；注册本身不完成 MFA。WebAuthn 验证 RP ID、精确 Origin、单次挑战、用户存在和用户验证标志、签名及存储的签名计数。

第一次 Passkey 验证后进入 `RECOVERY_SETUP`，签发并确认新的 MFA 恢复码后才进入 `MFA`。只有 `MFA` 状态可使用数据接口，客户端传 `mfa=true` 不改变权限。

### 正常登录与身份恢复

正常登录：`login → authenticate/options → authenticate/verify`。增加 Passkey、撤销 Passkey、重发恢复码要求近期 Passkey。撤销最后一个有效 Passkey 会被拒绝。

遗失全部 Passkey：先重新验证系统密码，再 POST `/vault/auth/mfa-recovery/verify` 提交一个 MFA 恢复码。服务器在事务中单次消费恢复码、撤销旧 Passkey、递增 authEpoch 并写 outbox，仅交接为 `RECOVERY_ENROLLMENT`。该状态不能读取密文、初始化或提交轮换。随后绑定新 Passkey，再完成一次认证，签发并确认新批次的恢复码，才交接到新 authEpoch 的完整会话。

每批 8 个随机 32 字节恢复码，以无填充 Base64url 返回，仅数据库保存 SHA-256 摘要。未确认的批次 10 分钟内有效；确认时才替代旧批次，签发响应丢失可重新签发。恢复码文件由前端保存，至少包括 `formatVersion`、`batchId`、`ownerSubject`、`codes`。API 不接收 RK 进行身份恢复；通过身份恢复后仍须在浏览器用解密材料解锁，原密文不改变。

## 接口

返回结构沿用 `JsonResult<T>`。UUID 使用规范小写字符串，二进制使用无填充 Base64url；公开版本号不超过 JavaScript 安全整数。ownerSubject 使用十进制字符串，不能转 JavaScript number。

传输模型集中在 `model/vault/VaultProtocol.java`。这些是协议入参，不能直接用数据库实体接收用户请求。Controller 校验格式；Service 只写允许修改的字段，显式设置归属、版本和审计。

| 方法与路径 | 作用 |
| --- | --- |
| GET `/vault/auth/options` | 获取 CSRF 和当前认证状态 |
| POST `/vault/auth/login` | 系统账号密码登录，进入待 MFA |
| POST `/vault/auth/passkeys/register/options`、`/verify` | 注册挑战和注册验证 |
| POST `/vault/auth/passkeys/authenticate/options`、`/verify` | 认证挑战、签名验证和近期认证 |
| POST `/vault/auth/passkeys/operations/delete-item/options` | 单条删除挑战，直接接收 ItemWrite |
| POST `/vault/auth/passkeys/operations/purge-item/options` | 永久删除挑战，直接接收 ItemLifecycle |
| POST `/vault/auth/passkeys/operations/delete-group/options` | 删除账号组挑战，直接接收 ItemLifecycle |
| POST `/vault/auth/passkeys/operations/batch-items/options` | 批量删除挑战，直接接收完整 ItemBatch；接入见[操作授权契约](password-vault-operation-passkey.md) |
| GET `/vault/auth/passkeys` | 查看有效凭据摘要 |
| POST `/vault/auth/passkeys/revoke` | 撤销凭据，预期 credentialRevision；退出旧会话 |
| POST `/vault/auth/recovery-codes/issue`、`/confirm` | 签发与确认恢复码，带预期 credentialRevision |
| POST `/vault/auth/mfa-recovery/verify` | 消费恢复码，进入受限重绑会话 |
| GET `/vault/auth/reauth` | 返回当前状态与再次认证的 options 地址 |
| POST `/vault/auth/logout` | 结束当前会话 |
| GET `/vault/api/session`、`/meta` | 完整会话状态及当前库元数据；meta 同时返回允许的密文协议、派生档案及内容版本兼容声明 |
| POST `/vault/api/initialize` | 幂等初始化正常槽和恢复槽 |
| GET `/vault/api/items` | `cursor`、`pageSize`，每页 1–100 条；集合变化后游标失效 |
| POST `/vault/api/items/create`、`/update`、`/delete` | 带幂等键的创建、乐观更新、逻辑删除 |
| POST `/vault/api/key-slots/change-master` | 只替换 NORMAL 槽，递增版本与 authEpoch，退出旧会话 |
| POST `/vault/api/rotations/start` | 冻结当前集合与版本，创建暂存代 |
| POST `/vault/api/restores/start` | 未初始化或空库恢复，显式登记 itemIds |
| POST `/vault/api/rotations/batch` | 不可覆盖的幂等批次上传 |
| GET `/vault/api/rotations/status?rotationId=...` | 预期条目、已接收批次和持久化提交回执 |
| GET `/vault/api/rotations/items?rotationId=...` | 暂存密文，支持 `cursor`、`pageSize` |
| POST `/vault/api/rotations/resume`、`/cancel`、`/commit` | 均要求近期 Passkey 认证，分别执行接管、取消、原子提交 |
| GET `/vault/api/backup-snapshot` | 近期认证后流式下载一致的元数据和全部当前密文，保留 JsonResult JSON 格式 |

注册及通用认证 WebAuthn options 的公开参数直接位于 `data`，操作挑战则位于 `data.publicKey`；浏览器调用前把 challenge、user.id、凭据 id 转为字节。verify 按 `Credential` / `Registration` 模型，将原生响应中的 ArrayBuffer 转为 Base64url。注册标签通过 `Registration.label` 提交。

`recovery-codes/issue` 请求为 `{"expectedCredentialRevision":1}`；confirm 为 `{"batchId":"...","expectedCredentialRevision":1}`。恢复码的 verify 请求为 `{"code":"..."}`。认证状态、credentialRevision 可重新 GET options 获取。

### TOTP 内容版本与客户端配合

`GET /vault/api/meta` 在未初始化、EMPTY 占位及已初始化状态下均返回下列公开字段。它们声明服务端当前采用的协议与客户端要求，不表示服务端能解析密文中的实际结构。

```json
{
  "allowedFormatVersions": [1],
  "allowedAlgorithms": ["A256GCM"],
  "allowedKdfProfiles": ["ARGON2ID_V19_M65536_T3_P4_L32"],
  "readableItemSchemaVersions": [1],
  "writeItemSchemaVersion": 1,
  "clientSchemaVersionHeader": "X-Vault-Item-Schema-Version",
  "minimumClientSchemaVersion": 1,
  "lifecycleProtocolVersion": 1,
  "clientLifecycleVersionHeader": "X-Vault-Lifecycle-Version"
}
```

所有 `/vault/api/**` 数据写请求（包括初始化、条目删除、密钥槽修改、轮换/恢复 start、batch、resume、cancel、commit）必须同时携带唯一的 `X-Vault-Item-Schema-Version: 1` 和 `X-Vault-Lifecycle-Version: 1`。缺失、旧版、未知版本或重复声明返回 HTTP 409、`data.errorCode=VAULT_CLIENT_UPGRADE_REQUIRED`，业务写入不会执行。认证端点 `/vault/auth/**` 和 GET 读取/备份不要求该头；CSRF、Origin、MFA、近期认证及归属校验仍然生效。该头不加入原请求模型或幂等摘要，历史请求的摘要规则不变。

前端必须在实现以下行为后才添加该头，不能只补请求头绕过准入：读取和保存完整生产内容版本 1，LOGIN 完整保留 TOTP 参数，ACCOUNT_GROUP 保留名称与成员；开发期内容须先完成客户端转换；未知结构阻止编辑及整库解析重写；旧客户端遇到升级错误应停止写入并提示更新。服务端不能证明客户端声明真实，也不能验证加密内容的 schema 或第三方动态码；此机制防止未升级客户端意外造成有损写入。

外层密文盒仍是 formatVersion=1、A256GCM；contentVersion、rowVersion 和 itemSchemaVersion 含义不变。16 KiB 明文/16400 字节密文上限不变，备份与轮换仍传输原有密文盒，第三方账号 P6 Passkey 不新增字段或接口。

### 删除系统用户

删除事务先锁定系统用户行，再检查密码库账户，锁顺序与登录一致。只要存在 `vault_account` 记录（包括尚未初始化及已逻辑删除的账户），就以 `system.user.vault-data-retained` 拒绝删除，角色关系和用户记录均不变；需要暂时停止访问时使用禁用用户，沿用会话撤销流程。

保护组件不依赖 `vault.enabled` 或身份 Service 是否启用，关闭模块也不能绕过。未执行密码库迁移时通过 `to_regclass` 检查后允许原有用户删除。当前不提供管理员删除密文的接口，不擅自设定用户注销保留期限；最终注销清理须另行确定保留政策、审计及备份处置后实施，不能通过软删除账户解除保护，也不能把禁用用户当成数据擦除。

## 错误契约

密码库错误的真实 HTTP 状态与 `JsonResult.code` 一致；`success=false`，`data.errorCode` 用于前端分支，`data.requestId` 为服务器生成的 UUID。文案有英文、简体和繁体资源。过滤器、CSRF、MVC 绑定、未知路由、Redis 会话和业务失败共用该契约，不回显输入或异常正文。

```json
{"code":409,"success":false,"message":"数据已更新，请重新同步","data":{"errorCode":"VAULT_VERSION_CONFLICT","requestId":"服务端UUID"}}
```

常用状态：400 参数格式，401 会话失效，403 MFA/近期认证/恢复码/CSRF/Origin，404 不存在或不属于当前用户，409 版本或幂等冲突，413 正文过大，415 媒体类型，423 轮换写锁，429 限流，503 数据库或 Redis 不可用。完整机器码见 `VaultEnum.Error`。前端不能将所有 403 一律当成退出，也不能仅凭 409 判断轮换此前未成功提交。

## 轮换与撤销一致性

统一业务锁顺序为账户 → 库 → 任务 → 代/条目。系统账号管理和密码登录先锁用户行，再锁账户。

start 固定 source/target generation、metaRevision、collectionRevision、startedAuthEpoch、逻辑 sessionBindingId 和逐项预期集合；任务绝对 30 分钟到期，期间禁止普通写入。批次由 `(rotationId,batchId)` 与规范请求摘要防重放，已写密文不可通过后续批次覆盖。start/resume 只持久化小回执，完整进度另查 status，避免 2000 条预期集合超过回执上限。

客户端分页下载全部暂存内容并逐条解密比对后，按照实施方案的 staging-manifest 数组规范计算 SHA-256，将无填充 Base64url 摘要交给 commit。服务端重算清单，并校验完整 itemId 集合、内容/行版本、keyEpoch、两个槽和源版本。服务器的清单校验不能证明客户端确实完成了解密。

一次 PostgreSQL 事务执行：旧代 RETIRED → 新代 ACTIVE → 活动指针与版本更新 → 任务 COMMITTED 与回执 → authEpoch 递增 → outbox → 审计和幂等回执。任一步失败整体回滚。提交成功后清除当前 HTTP 会话，要求重新登录；已提交的业务结果不因 Redis 删除失败回滚。响应丢失时用新完整会话查 status 或相同幂等请求，读取持久化结果。

新会话接管只允许 startedAuthEpoch 未变化且近期 MFA；发生系统密码重置、禁用/删除、凭据撤销或身份恢复后，新的认证代次只能显式取消旧任务，再重新 start，不能接管旧材料提交。近期重新认证保留逻辑 binding，不因此破坏当前任务。

撤销 worker 每 15 秒抢占待办，使用 `FOR UPDATE SKIP LOCKED` 和 30 秒租约；失败指数退避，崩溃后可重新领取。完成确认匹配领取时租约，防止旧 worker 覆盖新 worker。仅清理同账户且 epoch 小于目标的会话，保留恢复交接的新代会话。Redis 未清理完毕时，PostgreSQL 代次仍立即拒绝旧请求。

过期任务每 30 秒回收，清理暂存、恢复旧库状态并立即处理到期源行。每分钟清理任务将条目固定 720 小时保留期与旧槽/旧代 purge-delay 分开，保留任务终态和公开回执。认证成功、失败、拒绝和敏感操作写安全审计；失败审计独立事务，数据库故障时只输出固定告警。所有密码库 Mapper 日志默认 OFF，不记录参数或密文。

## 流式快照

`backup-snapshot` 在账户、库行锁保护下，用 PostgreSQL 游标每批读取 100 条有效密文，并逐条写入临时 JSON 文件。校验每条 keyEpoch 与活动代一致，最多 2000 条；元数据、两个槽及 ownerSubject 与条目来自同一次受保护的读取。服务器不构造全量密文集合，不执行解密。

临时文件完整生成、审计与数据库提交成功后才开始 HTTP 输出，并在传输前释放数据库锁；慢客户端不会长期阻塞修改或轮换。响应保持 `JsonResult` 的 code/message/success/data 结构，新增 `data.ownerSubject`，设置 `Content-Length`、`application/json` 和 `no-store`。网络端分块读取文件，客户端须在完整读取并解析成功后接受备份，不能把部分响应当作有效备份。

临时文件只包含与数据库相同的密文和公开元数据，POSIX 系统权限为 0600；生成失败、审计失败、事务回滚、传输成功或客户端断连都会关闭文件并尝试删除；删除异常不输出临时路径。进程被强制终止时可能留下密文临时文件，部署时应使用实例私有的临时目录或临时卷，并在实例销毁时清理。临时存储应为每个并发下载预留约 45 MiB（最大条目数与密文上限下的 JSON 大小）。

响应提交前的失败返回统一错误 JSON；响应已开始提交后的失败只中断传输，不在快照尾部追加错误 JSON。专用响应包装器使 Fastjson 通过响应头设置 Content-Length 时也经过 Spring Session 的提交回调，防止实际 HTTP 下匿名 Cookie 丢失或认证提升未持久化。

## 验证与当前边界

基础验证使用 `mvn clean test`，清理历史编译产物后确认实际执行数量和结果。本次新增 `VaultHttpContractTest`、`VaultProtocolTest`、`VaultPersistenceTest`；保留已有 `VaultIdentityServiceTest`。测试使用 subclass MockMaker，避免普通 mock 依赖 Java 25 的动态代理挂载。受限环境中若 MapStruct Plus 写入用户目录缓存失败，可用 `mvn -Duser.home=/tmp/mini-erp-vault-test-home clean test` 将本次缓存放入临时目录。

2026-10-17 实际执行上述带临时缓存目录的全量清理命令：`Tests run: 51, Failures: 0, Errors: 0, Skipped: 0`，编译及映射生成成功。其中 HTTP 契约 23 项、纯协议 11 项、已有身份回归 2 项、真实 PostgreSQL 事务 15 项；没有执行真实 Redis 集成测试或业务数据库迁移。

HTTP 用例加载真实密码库安全链和 Controller，验证写入版本准入、协议响应、CSRF、精确 Origin、缺少 MFA 和 ERP Bearer 拒绝；纯协议用例验证密文容量、算法、编码及历史回执兼容。数据库测试使用已有 embedded-postgres 测试依赖，自动启动只监听回环地址的一次性 PostgreSQL，执行完整原建表脚本，使用真实 Mapper、MapStruct 映射和 Spring 事务，不读取 dev/prod 配置或业务数据库。

数据库用例覆盖 13 表/无物理主外键、初始化回滚与并发、密文 CRUD/幂等/版本冲突、归属和 authEpoch、重复改主密码及 outbox、批次幂等与部分写入回滚、切代后 outbox 故障的整事务回滚及重试、任务接管/过期、空库恢复取消、2000 条快照、用户删除保护/未迁移环境/与登录的锁竞争。这些测试验证密文不丢失，不代替客户端 TOTP 算法、加解密或硬件 Passkey 验收。

现有 Redis 验证脚本可接收本机 Redis 可执行文件：

```bash
VAULT_TEST_REDIS_SERVER=/absolute/path/to/redis-server ./scripts/verify-password-vault.sh
```

Redis 已在 PATH 中时可直接运行脚本。当前脚本只检查 Redis 可执行文件并向 `mvn test` 传递 `vault.test.redis-server` 参数；当前测试不消费该参数，脚本退出成功不能证明 Redis 集成测试已执行。

待补的集成测试应仅在回环地址启动自己的 Redis、PostgreSQL 和两个 HTTP 实例，不加载项目 dev/prod 连接配置，不修改业务数据。覆盖实际 ACL 禁止 CONFIG 时的 EXTERNAL 启动、匿名 Cookie 与 CSRF 持久化、跨实例登录/Passkey 注册与验证/恢复码确认、初始化、快照和退出。测试使用虚构账户及有效签名样本；自动化样本不能替代真实浏览器或硬件 Passkey 验收。

待补故障用例包括 Redis ACL 拒绝、进程停止及重启、双实例连接和订阅恢复、旧 authEpoch 拒绝、outbox 持久化重试与并发领取、新代会话保留、过期事件索引清理、GETDEL 单次挑战和 Lua 限流；AUTO 模式需验证通知配置。

进一步安全验收仍需覆盖完整 ERP JWT 回归、Session/CSRF 更换、WebAuthn 签名及计数回放、恢复码并发消费、受限重绑、失效代次取消、租约重试、清理后的回执及 2000 条完整轮换。快照还须验证真实网络传输前锁释放、失败文件清理和禁止混代输出。

其余尚未闭环的能力：

- 前端落实 TOTP 内容 schema 兼容及请求头声明；服务端看不到密文内的 `itemSchemaVersion`，客户端必须完整保留字段并拒绝未知结构。
- 用户注销的保留期限、最终清理及备份处置策略；在此之前后端明确阻止删除仍保留密码库账户的系统用户。
- 目标数据库迁移核验、真实 HTTPS/Cookie 边界、浏览器/硬件 Passkey、客户端加解密、跨设备恢复及生产专项安全验收。

当前仅实现本地账号 + Passkey MFA，OIDC 是未实施的可选路径。第三方账号 Passkey 管理按方案保留在 P6，原生配套应用、跨端认证与密钥协议、凭据内容格式及迁移验收均须后续单独实现；不影响首版 TOTP 复用现有密文接口。
