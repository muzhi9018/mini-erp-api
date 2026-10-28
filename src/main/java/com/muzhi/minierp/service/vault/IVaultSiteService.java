package com.muzhi.minierp.service.vault;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.vault.VaultSite;

/**
 * <p>
 * 密码库站点 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-28
 */
public interface IVaultSiteService extends IService<VaultSite> {

    /**
     * 新增站点配置，域名不能与未删除站点重复。
     *
     * @param query 站点配置
     * @return 已保存的站点配置
     */
    VaultSite create(VaultSite query);

    /**
     * 分页查询站点配置，按排序值升序、ID 降序排列。
     *
     * @param query 名称、域名和状态筛选条件
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 站点分页结果
     */
    IPage<VaultSite> list(VaultSite query, Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除站点配置，不存在或已删除时拒绝操作。
     *
     * @param siteId 站点 ID
     */
    void delete(Long siteId);

    /**
     * 修改站点业务字段，不允许修改主键、逻辑删除标记和审计字段。
     *
     * @param query 站点 ID 及待修改的配置
     */
    void edit(VaultSite query);
}
