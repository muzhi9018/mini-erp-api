# 删除操作 Passkey 接入契约

更新日期：2026-10-24。对应前端实施方案第 7.7 节及 I2。登录仍使用“系统账号密码 → Passkey MFA”；本次增加删除操作的逐次授权，不修改登录或通用近期认证流程，不涉及 P6 第三方账号 Passkey 存储。

## 覆盖范围

| 操作 | 挑战路径（POST） | 提交路径（POST） | 业务请求模型 |
| --- | --- | --- | --- |
| DELETE_ITEM | `/vault/auth/passkeys/operations/delete-item/options` | `/vault/api/items/delete` | 原 ItemWrite 字段 |
| PURGE_ITEM | `/vault/auth/passkeys/operations/purge-item/options` | `/vault/api/items/purge` | 原 ItemLifecycle 字段 |
| DELETE_GROUP | `/vault/auth/passkeys/operations/delete-group/options` | `/vault/api/items/delete-group` | 原 ItemLifecycle 字段 |
| BATCH_ITEMS | `/vault/auth/passkeys/operations/batch-items/options` | `/vault/api/items/batch` | 含 DELETE 或 DELETE_GROUP 的完整 ItemBatch |

每个新删除必须使用新的操作挑战和断言。纯 CREATE/UPDATE 批次、恢复、移出组成员、仅移除 TOTP 及内部到期清理保持原认证规则。HTTP 请求没有后台任务豁免参数。账号组直接删除仍保留成员密码及 TOTP，不进入已删除集合。

## 前端调用顺序

1. 用户确认目标和删除后果后，冻结原业务正文及 idempotencyKey。删除密码所附带的组引用 UPDATE 与 DELETE 仍放在一个有序原子批次中。
2. POST 上表对应的挑战路径，直接发送原业务模型，不包含 passkeyAuthorization；不发送客户端摘要、标题、密码或 TOTP 明文。旧通用路径 `/vault/auth/passkeys/operations/options` 已移除，前端需按操作选择对应路径，并去掉原有 `{ operation, request }` 外层包装。
3. 使用返回的 `data.publicKey` 调用已有 WebAuthn helper 和 `navigator.credentials.get()`。将 challenge 及 allowCredentials[].id 从无填充 Base64url 转为字节。取消或失败不提交删除。
4. 将断言中的 ArrayBuffer 转为无填充 Base64url，在原业务正文末尾附上 `passkeyAuthorization: { challengeId, credential }`，提交对应原路由。credential 使用现有 VaultProtocol.Credential 格式；没有单独的操作 verify 端点。
5. 收到服务端成功回执后才移除列表或详情及本地秘密。验证期间改变任何业务字段时，应重新确认并取得新的挑战。

挑战请求示例（POST `/vault/auth/passkeys/operations/purge-item/options`）：

```json
{
  "itemId": "11111111-1111-4111-8111-111111111111",
  "generation": 1,
  "expectedRowVersion": 2,
  "idempotencyKey": "22222222-2222-4222-8222-222222222222"
}
```

成功响应的 data 为：

```json
{
  "challengeId": "无填充Base64url编码的32字节随机标识",
  "publicKey": {
    "challenge": "无填充Base64url编码的32字节随机挑战",
    "timeout": 120000,
    "rpId": "vault.example.com",
    "userVerification": "required",
    "credentialRevision": 1,
    "allowCredentials": [{ "type": "public-key", "id": "当前用户有效凭据ID" }]
  },
  "expiresAt": "2026-10-23T10:00:00Z"
}
```

所有请求使用密码库 Cookie、CSRF、精确 Origin、JSON 和 no-store 规则。认证 options 不要求数据版本头；原数据提交仍要求 `X-Vault-Item-Schema-Version: 1` 和 `X-Vault-Lifecycle-Version: 1`。批次挑战与批次提交请求体上限均为 1 MiB，其余挑战及删除提交上限为 64 KiB；最终附带断言的正文也须在上限内。

每个挑战接口只接收一种普通 record，直接复用 `ItemWrite`、`ItemLifecycle` 或 `ItemBatch`。操作类型由服务端路径固定，不接收客户端操作编码。统一 Fastjson HTTP 转换器直接解析完整请求及嵌套的变更、密文盒；接口入参和出参不使用 `Map`、`JSONObject` 或 `Object` 承载业务字段，不使用多态入参或二次 JSON 转换。

## 服务端保证与重试

服务端在账户锁和库锁内检查完整 MFA、authEpoch、归属及可写状态后签发挑战。挑战最多有效两分钟，Redis 只保存随机挑战、截止时间和绑定摘要；摘要由服务端按原业务 DTO 规范序列化计算，覆盖 owner、account、vaultId、operation、稳定 sessionBindingId、authEpoch、credentialRevision 和完整业务正文（含幂等键、有序 changes、预期版本及密文）。Session ID 更新不改变同一逻辑登录的绑定，新的登录不能使用旧挑战。

首次执行在既有账户锁下先查持久化成功回执：同操作、同键、同业务摘要返回原回执，摘要不同返回 409。无成功回执时才消费并验证本次证明。原子 GETDEL 在验证尝试时消费挑战，验证失败或业务事务回滚均不能再次使用。WebAuthn4J 继续校验签名、challenge、webauthn.get、origin、RP ID、UP、UV 和凭据元数据；凭据必须属于当前账户且未撤销。

证明排除于原业务 DTO 序列化和幂等摘要之外，不写入业务回执。验证成功后仍在同一事务重检当前代、行/集合版本、状态及轮换写锁，再写条目、审计及回执；批次失败整体回滚。操作验证不刷新通用 mfaAt，也不签发可复用授权 token。到期时间、密码恢复和后台清理策略不变；本次无新增数据库迁移或依赖。

| 情况 | 响应与处理 |
| --- | --- |
| 缺少、过期、已消费或绑定不匹配的操作证明 | 403 `VAULT_OPERATION_AUTHORIZATION_INVALID`；保留内容，重新确认及验证 |
| 签名、用户验证、RP/origin 或有效凭据检查失败 | 403 `VAULT_PASSKEY_INVALID`；不降级为系统密码、主密码或恢复码删除 |
| 畸形字段、非删除操作或纯新增/更新批次申请挑战 | 400 `VAULT_INVALID_REQUEST` |
| 不存在或错误归属、版本变化、轮换写锁、设施故障 | 沿用 404/409/423/503；不产生部分删除或成功审计 |
| 已成功但响应丢失 | 重发完全相同业务正文和键，可不附证明读取原回执；不重复删除或审计 |
| 未提交、已知回滚、挑战已消费/过期 | 无回执时不能免验证执行；重新确认和验证后可用相同正文及键附新证明重试 |

404 不能当作原请求已经成功的证据。不能自动换幂等键、重加密组引用、重排批次或自动重新弹出认证。旧客户端不附证明的新删除会被拒绝；发布须同步接入前端。回退须保留服务端验证或关闭删除接口。

## 验证范围

`VaultOperationProtocolTest` 校验三个原业务模型的历史 JSON 字段顺序、null 字段和摘要保持一致，证明可由实际 Fastjson 配置读入但不会序列化进摘要。

`VaultOperationAuthorizationTest` 使用一次性 PostgreSQL、真实 ES256 签名及实际 VaultWebAuthn/WebAuthn4J，覆盖删除入口缺证明、独立 purge、UP/UV、签名、challenge、origin/RP/type/userHandle、目标/版本/键/路由/逻辑会话绑定、凭据版本与撤销、过期、批次密文篡改、事务回滚、并发幂等和 Redis 故障关闭。挑战存储使用内存 Redis 替身模拟 SET/GETDEL；不把它称为真实 Redis 验证。

`VaultHttpContractTest` 覆盖四个固定入参挑战端点的完整 MFA、CSRF、Origin、no-store、证明反序列化、批次请求体上限，以及旧通用端点返回 404。既有生命周期测试隔离操作授权组件，继续验证删除、恢复、轮换及清理业务；真实授权与事务由新测试组合验证。

真实 Redis GETDEL/TTL/ACL 和跨实例挑战、真实桌面/手机 Passkey 成功/取消/失败，以及前端确认、取消和迟到断言丢弃仍需联调验收。本次未修改前端、业务数据库或部署配置。

2026-10-24 执行 `mvn -Dtest='Vault*Test' test`：197 项通过，0 失败、0 错误、0 跳过；Java 编译和 MapStruct Plus 映射生成通过。测试仅使用虚构凭据和回环地址的一次性 PostgreSQL，未连接业务数据库。
