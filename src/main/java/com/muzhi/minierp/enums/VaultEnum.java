package com.muzhi.minierp.enums;

import lombok.Getter;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.util.Assert;

/**
 * <p>
 * {@code VaultEnum}：密码库协议、状态与错误定义
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface VaultEnum {

    interface Coded {
        String getCode();
    }

    static <E extends Enum<E> & Coded> E ofCode(Class<E> type, String code) {
        for (E value : type.getEnumConstants()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw Error.INVALID_REQUEST.exception();
    }

    @Getter
    enum RedisConfiguration implements Coded {

        AUTO("AUTO", "由应用配置过期通知"),
        EXTERNAL("EXTERNAL", "由运维预先配置过期通知");

        private final String code;

        private final String name;

        RedisConfiguration(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum Stage implements Coded {

        PASSWORD("PASSWORD", "待 MFA"),
        MFA("MFA", "完整认证"),
        RECOVERY_ENROLLMENT("RECOVERY_ENROLLMENT", "身份恢复重绑"),
        RECOVERY_SETUP("RECOVERY_SETUP", "恢复码确认");

        private final String code;

        private final String name;

        Stage(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum VaultStatus implements Coded {

        EMPTY("EMPTY", "未初始化"),
        ACTIVE("ACTIVE", "活动"),
        RESTORING("RESTORING", "备份恢复中");

        private final String code;

        private final String name;

        VaultStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum GenerationStatus implements Coded {

        STAGING("STAGING", "暂存"),
        ACTIVE("ACTIVE", "活动"),
        RETIRED("RETIRED", "已替换"),
        ABORTED("ABORTED", "已取消");

        private final String code;

        private final String name;

        GenerationStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum RotationStatus implements Coded {

        UPLOADING("UPLOADING", "上传中"),
        COMMITTED("COMMITTED", "已提交"),
        CANCELLED("CANCELLED", "已取消"),
        EXPIRED("EXPIRED", "已过期");

        private final String code;

        private final String name;

        RotationStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum RotationMode implements Coded {

        ROTATION("ROTATION", "完整轮换"),
        RESTORE("RESTORE", "备份恢复");

        private final String code;

        private final String name;

        RotationMode(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /**
     * 轮换内部下载范围，源集合以任务冻结清单为准。
     */
    @Getter
    enum ItemScope implements Coded {

        SOURCE("SOURCE", "冻结源集合"),
        STAGING("STAGING", "暂存集合");

        private final String code;

        private final String name;

        ItemScope(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum SlotType implements Coded {

        NORMAL("NORMAL", "正常解锁"),
        RECOVERY("RECOVERY", "密钥恢复");

        private final String code;

        private final String name;

        SlotType(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum OutboxStatus implements Coded {

        PENDING("PENDING", "待处理"),
        PROCESSING("PROCESSING", "处理中"),
        DONE("DONE", "已完成");

        private final String code;

        private final String name;

        OutboxStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /**
     * 原子密文变更动作；组的直接删除意图由已解密内容的客户端声明。
     */
    @Getter
    enum ItemChangeAction implements Coded {

        CREATE("CREATE", "新增"),
        UPDATE("UPDATE", "更新"),
        DELETE("DELETE", "密码移入已删除"),
        DELETE_GROUP("DELETE_GROUP", "直接删除账号组");

        private final String code;

        private final String name;

        ItemChangeAction(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum Operation implements Coded {

        INITIALIZE("INITIALIZE", "初始化密码库"),
        CREATE_ITEM("CREATE_ITEM", "新增条目"),
        UPDATE_ITEM("UPDATE_ITEM", "更新条目"),
        DELETE_ITEM("DELETE_ITEM", "删除条目"),
        DELETE_GROUP("DELETE_GROUP", "直接删除账号组"),
        RESTORE_ITEM("RESTORE_ITEM", "恢复条目"),
        PURGE_ITEM("PURGE_ITEM", "永久删除条目"),
        BATCH_ITEMS("BATCH_ITEMS", "批量变更条目"),
        CHANGE_MASTER("CHANGE_MASTER", "修改主密码"),
        ROTATION_START("ROTATION_START", "开始密钥轮换"),
        ROTATION_COMMIT("ROTATION_COMMIT", "提交密钥轮换"),
        ROTATION_CANCEL("ROTATION_CANCEL", "取消密钥轮换"),
        ROTATION_RESUME("ROTATION_RESUME", "继续密钥轮换"),
        RESTORE_START("RESTORE_START", "开始备份恢复");

        private final String code;

        private final String name;

        Operation(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum AuditAction implements Coded {

        LOGIN("LOGIN", "登录密码库"),
        LOGOUT("LOGOUT", "退出密码库"),
        PASSKEY_REGISTER("PASSKEY_REGISTER", "注册通行密钥"),
        PASSKEY_AUTHENTICATE("PASSKEY_AUTHENTICATE", "通行密钥认证"),
        PASSKEY_REVOKE("PASSKEY_REVOKE", "撤销通行密钥"),
        MFA_RECOVERY("MFA_RECOVERY", "多因素认证恢复"),
        RECOVERY_CODES_ISSUE("RECOVERY_CODES_ISSUE", "签发恢复码"),
        RECOVERY_CODES_CONFIRM("RECOVERY_CODES_CONFIRM", "确认恢复码"),
        INITIALIZE("INITIALIZE", "初始化密码库"),
        ITEM_CREATE("ITEM_CREATE", "新增条目"),
        ITEM_UPDATE("ITEM_UPDATE", "更新条目"),
        ITEM_DELETE("ITEM_DELETE", "删除条目"),
        ITEM_RESTORE("ITEM_RESTORE", "恢复条目"),
        ITEM_PURGE("ITEM_PURGE", "永久清理条目"),
        CHANGE_MASTER("CHANGE_MASTER", "修改主密码"),
        ROTATION_START("ROTATION_START", "开始密钥轮换"),
        ROTATION_COMMIT("ROTATION_COMMIT", "提交密钥轮换"),
        ROTATION_CANCEL("ROTATION_CANCEL", "取消密钥轮换"),
        ROTATION_RESUME("ROTATION_RESUME", "继续密钥轮换"),
        ROTATION_EXPIRE("ROTATION_EXPIRE", "密钥轮换过期"),
        RESTORE_START("RESTORE_START", "开始备份恢复"),
        BACKUP_DOWNLOAD("BACKUP_DOWNLOAD", "下载备份"),
        SESSION_REJECT("SESSION_REJECT", "拒绝会话请求");

        private final String code;

        private final String name;

        AuditAction(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum AuditResult implements Coded {

        SUCCESS("SUCCESS", "成功"),
        DENIED("DENIED", "拒绝"),
        FAILURE("FAILURE", "失败");

        private final String code;

        private final String name;

        AuditResult(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum ChallengePurpose implements Coded {

        REGISTER("REGISTER", "注册凭据"),
        AUTHENTICATE("AUTHENTICATE", "验证凭据");

        private final String code;

        private final String name;

        ChallengePurpose(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @Getter
    enum Error {

        INVALID_REQUEST(400, "vault.invalid-request", "请求参数或协议格式无效"),
        INVALID_CREDENTIALS(401, "vault.invalid-credentials", "账号或登录密码错误"),
        SESSION_EXPIRED(401, "vault.session-expired", "密码库会话已过期，请重新登录"),
        SESSION_REVOKED(401, "vault.session-revoked", "密码库会话已失效，请重新登录"),
        MFA_REQUIRED(403, "vault.mfa-required", "请先完成多因素认证"),
        REAUTH_REQUIRED(403, "vault.reauth-required", "请重新完成 Passkey 认证后继续"),
        RECOVERY_SETUP_REQUIRED(403, "vault.recovery-setup-required", "请先确认新的 MFA 恢复码"),
        MFA_RECOVERY_INVALID(403, "vault.mfa-recovery-invalid", "MFA 恢复码无效或已使用"),
        PASSKEY_INVALID(403, "vault.passkey-invalid", "Passkey 验证失败，请重新开始认证"),
        CHALLENGE_INVALID(403, "vault.challenge-invalid", "认证挑战无效或已过期"),
        OPERATION_AUTHORIZATION_INVALID(403, "vault.operation-authorization-invalid", "本次删除的 Passkey 验证无效或已过期，请重新确认并验证"),
        CSRF_REJECTED(403, "vault.csrf-rejected", "请求校验失败，请刷新后重试"),
        ORIGIN_REJECTED(403, "vault.origin-rejected", "不允许的请求来源"),
        NOT_FOUND(404, "vault.not-found", "记录不存在"),
        METHOD_NOT_ALLOWED(405, "vault.method-not-allowed", "不支持的请求方法"),
        VERSION_CONFLICT(409, "vault.version-conflict", "数据已更新，请重新同步"),
        ITEM_ALREADY_DELETED(409, "vault.item-already-deleted", "条目已删除，保留期限不变"),
        ITEM_NOT_DELETED(409, "vault.item-not-deleted", "条目未处于已删除状态，请重新同步"),
        ITEM_EXPIRED(409, "vault.item-expired", "条目已过保留期，无法恢复"),
        ITEM_LIMIT_REACHED(409, "vault.item-limit-reached", "正常条目数量已达上限，请先释放容量"),
        CLIENT_UPGRADE_REQUIRED(409, "vault.client-upgrade-required", "请升级密码库客户端后重试，当前客户端不能安全保留条目内容"),
        IDEMPOTENCY_CONFLICT(409, "vault.idempotency-conflict", "重复请求的内容不一致"),
        ROTATION_EXPIRED(409, "vault.rotation-expired", "轮换任务已过期，请重新开始"),
        LAST_PASSKEY(409, "vault.last-passkey", "不能撤销最后一个有效 Passkey"),
        ALREADY_INITIALIZED(409, "vault.already-initialized", "密码库已经初始化"),
        PAYLOAD_TOO_LARGE(413, "vault.payload-too-large", "请求内容超过允许大小"),
        UNSUPPORTED_MEDIA_TYPE(415, "vault.unsupported-media-type", "不支持的请求内容类型"),
        WRITE_LOCKED(423, "vault.write-locked", "密码库正在轮换或恢复，请稍后编辑"),
        RATE_LIMITED(429, "vault.rate-limited", "请求过于频繁，请稍后重试"),
        UNAVAILABLE(503, "vault.unavailable", "密码库服务暂时不可用"),
        INTERNAL_ERROR(500, "vault.internal-error", "密码库服务发生错误");

        private final int status;

        private final String i18nCode;

        private final String message;

        Error(int status, String i18nCode, String message) {
            this.status = status;
            this.i18nCode = i18nCode;
            this.message = message;
        }

        public String getMachineCode() {
            return "VAULT_" + this.name();
        }

        public void rejectIf(boolean invalid) {
            Assert.isTrue(invalid, i18nCode, message);
        }

        public BusinessException exception() {
            return new BusinessException(i18nCode, message);
        }

        public static Error ofKey(String key) {
            for (Error error : Error.values()) {
                if (error.getI18nCode().equals(key)) {
                    return error;
                }
            }
            return INTERNAL_ERROR;
        }
    }
}
