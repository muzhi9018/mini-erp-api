package com.muzhi.minierp.service.vault;

import com.muzhi.minierp.model.vault.*;
import java.util.Map;
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
    Map<String, Object> meta(VaultSession session);

    /** 一次性创建库和两个有效密钥槽。 */
    Map<String, Object> initialize(VaultSession session, VaultProtocol.Setup setup);

    /** 分页读取同一活动代，游标绑定用户、库与集合版本。 */
    Map<String, Object> items(VaultSession session, String cursor, int pageSize);

    /** 创建或按预期版本更新密文条目。 */
    Map<String, Object> save(VaultSession session, VaultProtocol.ItemWrite request, boolean create);

    /** 原子逻辑删除条目并记录删除人和时间。 */
    Map<String, Object> delete(VaultSession session, VaultProtocol.ItemWrite request);

    /** 保持 VMK 不变，原子替换正常槽并撤销旧会话。 */
    Map<String, Object> changeMaster(VaultSession session, VaultProtocol.MasterChange request);

    /** 生成一致性密文快照，由调用方流式发送并关闭，不进行服务端解密。 */
    VaultSnapshot snapshot(VaultSession session) throws IOException;
}
