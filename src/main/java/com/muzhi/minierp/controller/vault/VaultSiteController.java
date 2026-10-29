package com.muzhi.minierp.controller.vault;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.vault.VaultSite;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.vault.IVaultSiteService;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 密码库站点 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-28
 */
@RestController
@RequestMapping("/vaultSite")
@RequiredArgsConstructor
public class VaultSiteController {

    private final IVaultSiteService vaultSiteService;

    /**
     * 新增站点配置，未指定状态和排序时使用默认值。
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('vault:vault-site:create')")
    public JsonResult<VaultSite> create(@RequestBody VaultSite query) {
        this.validateSite(query);
        VaultSite site = vaultSiteService.create(query);
        return JsonResult.success(site);
    }

    /**
     * 分页查询站点配置，支持名称、域名模糊查询和状态筛选。
     *
     * @param query 筛选条件
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 站点分页结果
     */
    @GetMapping("/list")
    public JsonResult<IPage<VaultSite>> list(VaultSite query, @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam(name = "pageSize", defaultValue = "15") Integer pageSize) {
        Assert.isTrue(pageNum == null || pageNum <= 0, "pagination.page-num-invalid", "当前页必须大于0");
        Assert.isTrue(pageSize == null || pageSize <= 0, "pagination.page-size-invalid", "每页数量必须大于0");
        this.validateStatus(query.getStatus());
        IPage<VaultSite> page = vaultSiteService.list(query, pageNum, pageSize);
        return JsonResult.success(page);
    }

    /**
     * 根据站点 ID 逻辑删除站点配置。
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('vault:vault-site:delete')")
    public JsonResult<Boolean> delete(@RequestBody VaultSite query) {
        Assert.isNull(query, "vault.site.required", "站点配置信息不能为空");
        this.validateId(query.getId());
        vaultSiteService.delete(query.getId());
        return JsonResult.success(true);
    }

    /**
     * 修改站点名称、域名、状态和排序，未指定状态或排序时保留原值。
     */
    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('vault:vault-site:edit')")
    public JsonResult<Boolean> edit(@RequestBody VaultSite query) {
        this.validateSite(query);
        this.validateId(query.getId());
        vaultSiteService.edit(query);
        return JsonResult.success(true);
    }

    /**
     * 校验新增和修改共用的站点字段。
     */
    private void validateSite(VaultSite query) {
        Assert.isNull(query, "vault.site.required", "站点配置信息不能为空");
        String siteName = StringUtils.trim(query.getSiteName());
        boolean nameBlank = StringUtils.isBlank(siteName);
        Assert.isTrue(nameBlank, "vault.site.name-required", "站点名称不能为空");
        int maximumNameLength = 128;
        Assert.isTrue(
                siteName.length() > maximumNameLength,
                "vault.site.name-too-long",
                "站点名称不能超过{0}个字符",
                maximumNameLength
        );

        String siteDomain = StringUtils.trim(query.getSiteDomain());
        boolean domainBlank = StringUtils.isBlank(siteDomain);
        Assert.isTrue(domainBlank, "vault.site.domain-required", "站点域名不能为空");
        int maximumDomainLength = 256;
        Assert.isTrue(
                siteDomain.length() > maximumDomainLength,
                "vault.site.domain-too-long",
                "站点域名不能超过{0}个字符",
                maximumDomainLength
        );
        this.validateStatus(query.getStatus());
    }

    private void validateId(Long siteId) {
        Assert.isTrue(siteId == null || siteId <= 0, "vault.site.id-invalid", "站点 ID 必须大于0");
    }

    private void validateStatus(Short code) {
        VaultEnum.SiteStatus status = VaultEnum.SiteStatus.ofCode(code);
        Assert.isTrue(code != null && status == null, "vault.site.status-invalid", "站点状态无效");
    }
}
