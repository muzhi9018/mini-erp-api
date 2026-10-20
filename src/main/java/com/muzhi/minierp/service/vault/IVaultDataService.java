package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.vo.vault.*;
import java.io.IOException;

/**
 * <p>
 * {@code IVaultDataService}：用户归属下的密文数据与主密码槽事务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public interface IVaultDataService {
    /** 查询库元数据，未初始化时返回明确状态。 */
    VaultMetaVO meta(VaultSession session);

    /** 一次性创建库和两个有效密钥槽。 */
    VaultMetaVO initialize(VaultSession session, VaultProtocol.Setup setup);

    /** 分页读取同一活动代，游标绑定用户、库与集合版本。 */
    VaultItemPageVO items(VaultSession session, String cursor, int pageSize);

    /** 创建或按预期版本更新密文条目。 */
    VaultItemResultVO save(VaultSession session, VaultProtocol.ItemWrite request, boolean create);

    /** 原子逻辑删除条目并记录删除人和时间。 */
    VaultItemResultVO delete(VaultSession session, VaultProtocol.ItemWrite request);

    /**
     * 在同一事务中提交密文变更，集合版本必须与客户端管理组成员时的快照一致。
     *
     * @param session 当前完整认证会话
     * @param request 同一活动代的新增、更新与删除集合
     * @return 按请求顺序排列的公开回执；不含密文或关联关系
     */
    VaultItemBatchResultVO batch(VaultSession session, VaultProtocol.ItemBatch request);

    /** 保持 VMK 不变，原子替换正常槽并撤销旧会话。 */
    VaultMetaVO changeMaster(VaultSession session, VaultProtocol.MasterChange request);

    /** 生成一致性密文快照，由调用方流式发送并关闭，不进行服务端解密。 */
    VaultSnapshot snapshot(VaultSession session) throws IOException;
}
