package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.vo.vault.*;

/**
 * <p>
 * {@code IVaultRotationService}：完整轮换和空库备份恢复的暂存与原子提交
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultRotationService {

    /**
     * 冻结源版本和逐项集合，创建新代并暂停写入。
     */
    VaultRotationVO start(VaultSession session, VaultProtocol.RotationStart request, VaultEnum.RotationMode mode);

    /**
     * 查询当前用户任务的持久化进度和提交回执。
     */
    VaultRotationStatusVO status(VaultSession session, String rotationId);

    /**
     * 幂等接收不可覆盖的暂存批次。
     */
    VaultRotationBatchVO batch(VaultSession session, VaultProtocol.RotationBatch request);

    /**
     * 返回冻结源集合或暂存代密文，包含服务端复制的删除元数据。
     */
    VaultItemPageVO<VaultItemVO> items(VaultSession session, String rotationId, VaultEnum.ItemScope scope, String cursor, int pageSize);

    /**
     * 默认下载暂存代进行验证。
     */
    default VaultItemPageVO<VaultItemVO> items(VaultSession session, String rotationId, String cursor, int pageSize) {
        return this.items(session, rotationId, VaultEnum.ItemScope.STAGING, cursor, pageSize);
    }

    /**
     * 近期 MFA、集合校验和清单匹配后原子切代并撤销旧会话。
     */
    VaultRotationReceiptVO commit(VaultSession session, VaultProtocol.RotationCommand request);

    /**
     * 近期 MFA 后由新登录会话显式接管仍有效的任务。
     */
    VaultRotationVO resume(VaultSession session, VaultProtocol.RotationCommand request);

    /**
     * 取消任务，保留旧活动代并清理暂存。
     */
    VaultRotationVO cancel(VaultSession session, VaultProtocol.RotationCommand request);

    /**
     * 后台按数据库时间回收超时任务，不撤销已成功提交的代。
     */
    void expire(Long ownerId, String rotationId);
}
