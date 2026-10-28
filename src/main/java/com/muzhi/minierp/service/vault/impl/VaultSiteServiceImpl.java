package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhi.minierp.entity.vault.VaultSite;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.mapper.vault.VaultSiteMapper;
import com.muzhi.minierp.service.vault.IVaultSiteService;
import com.muzhi.minierp.util.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 密码库站点 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-28
 */
@Service
public class VaultSiteServiceImpl extends ServiceImpl<VaultSiteMapper, VaultSite> implements IVaultSiteService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSite create(VaultSite query) {
        String siteDomain = query.getSiteDomain().trim();
        this.validateDomainUnique(siteDomain, null);

        // 仅接收业务字段，主键、默认值和审计字段由服务端维护。
        VaultSite site = new VaultSite();
        Long siteId = IdWorker.getId();
        String siteName = query.getSiteName().trim();
        Short status = query.getStatus() == null ? VaultEnum.SiteStatus.ENABLED.getCode() : query.getStatus();
        int defaultSort = 0;
        Integer sort = query.getSort() == null ? defaultSort : query.getSort();
        site.setId(siteId);
        site.setSiteName(siteName);
        site.setSiteDomain(siteDomain);
        site.setStatus(status);
        site.setSort(sort);
        site.setIsDeleted(false);
        try {
            int inserted = super.baseMapper.insert(site);
            Assert.isTrue(inserted != 1, "vault.site.create-failed", "新增站点配置失败，请重试");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("vault.site.domain-already-exists", "站点域名已存在", exception);
        }
        return site;
    }

    @Override
    public IPage<VaultSite> list(VaultSite query, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<VaultSite> siteQuery = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getSiteName())) {
            String siteName = query.getSiteName().trim();
            siteQuery.like(VaultSite::getSiteName, siteName);
        }
        if (StringUtils.isNotBlank(query.getSiteDomain())) {
            String siteDomain = query.getSiteDomain().trim();
            siteQuery.like(VaultSite::getSiteDomain, siteDomain);
        }
        if (query.getStatus() != null) {
            siteQuery.eq(VaultSite::getStatus, query.getStatus());
        }
        siteQuery.orderByAsc(VaultSite::getSort);
        siteQuery.orderByDesc(VaultSite::getId);
        IPage<VaultSite> page = new Page<>(pageNum, pageSize);
        return super.baseMapper.selectPage(page, siteQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long siteId) {
        int deleted = super.baseMapper.deleteById(siteId);
        Assert.isTrue(deleted != 1, "vault.site.not-found", "站点配置不存在或已删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(VaultSite query) {
        Long siteId = query.getId();
        VaultSite site = super.baseMapper.selectById(siteId);
        Assert.isNull(site, "vault.site.not-found", "站点配置不存在或已删除");
        String siteDomain = query.getSiteDomain().trim();
        this.validateDomainUnique(siteDomain, siteId);

        VaultSite update = new VaultSite();
        String siteName = query.getSiteName().trim();
        update.setId(siteId);
        update.setSiteName(siteName);
        update.setSiteDomain(siteDomain);
        update.setStatus(query.getStatus());
        update.setSort(query.getSort());
        try {
            int updated = super.baseMapper.updateById(update);
            Assert.isTrue(updated != 1, "vault.site.edit-failed", "修改站点配置失败，请重试");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("vault.site.domain-already-exists", "站点域名已存在", exception);
        }
    }

    /**
     * 校验未删除站点的域名唯一性，修改时排除当前站点。
     */
    private void validateDomainUnique(String siteDomain, Long siteId) {
        LambdaQueryWrapper<VaultSite> domainQuery = Wrappers.lambdaQuery();
        domainQuery.eq(VaultSite::getSiteDomain, siteDomain);
        if (siteId != null) {
            domainQuery.ne(VaultSite::getId, siteId);
        }
        boolean domainExisted = super.baseMapper.exists(domainQuery);
        Assert.isTrue(domainExisted, "vault.site.domain-already-exists", "站点域名已存在");
    }
}
