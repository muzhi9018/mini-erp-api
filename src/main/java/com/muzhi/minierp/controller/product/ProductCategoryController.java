package com.muzhi.minierp.controller.product;

import com.muzhi.minierp.entity.product.ProductCategory;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.product.IProductCategoryService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.product.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 内部商品类目 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/product/category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final IProductCategoryService productCategoryService;

    /**
     * 查询全部有效类目，逐级按展示顺序、ID 排序。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('product-manager:product-category:query')")
    public JsonResult<List<ProductCategoryVO>> tree() {
        List<ProductCategoryVO> tree = productCategoryService.tree();
        return JsonResult.success(tree);
    }

    /**
     * 新增类目，未指定父级和排序时分别使用 0。
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('product-manager:product-category:create')")
    public JsonResult<ProductCategory> create(@RequestBody ProductCategory query) {
        this.validateCategory(query);
        String code = StringUtils.trim(query.getCode());
        boolean codeBlank = StringUtils.isBlank(code);
        Assert.isTrue(codeBlank, "product.category.code-required", "商品分类编码不能为空");
        int maximumCodeLength = 64;
        Assert.isTrue(
                code.length() > maximumCodeLength,
                "product.category.code-too-long",
                "商品分类编码不能超过{0}个字符",
                maximumCodeLength
        );
        if (query.getParentId() != null) {
            this.validateParentId(query.getParentId());
        }
        ProductCategory category = productCategoryService.create(query);
        return JsonResult.success(category);
    }

    /**
     * 修改名称、排序和备注，编码、父级及审计字段不接受普通编辑。
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('product-manager:product-category:update')")
    public JsonResult<ProductCategory> update(@RequestBody ProductCategory query) {
        this.validateCategory(query);
        this.validateId(query.getId());
        ProductCategory category = productCategoryService.updateCategory(query);
        return JsonResult.success(category);
    }

    /**
     * 逻辑删除没有有效子类目和商品的类目。
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('product-manager:product-category:delete')")
    public JsonResult<Boolean> delete(@RequestBody ProductCategory query) {
        Assert.isNull(query, "product.category.required", "商品分类信息不能为空");
        this.validateId(query.getId());
        productCategoryService.delete(query.getId());
        return JsonResult.success(true);
    }

    private void validateCategory(ProductCategory query) {
        Assert.isNull(query, "product.category.required", "商品分类信息不能为空");
        String name = StringUtils.trim(query.getName());
        boolean nameBlank = StringUtils.isBlank(name);
        Assert.isTrue(nameBlank, "product.category.name-required", "商品分类名称不能为空");
        int maximumNameLength = 128;
        Assert.isTrue(
                name.length() > maximumNameLength,
                "product.category.name-too-long",
                "商品分类名称不能超过{0}个字符",
                maximumNameLength
        );
        String remark = StringUtils.trimToNull(query.getRemark());
        int maximumRemarkLength = 1000;
        boolean remarkTooLong = remark != null && remark.length() > maximumRemarkLength;
        Assert.isTrue(
                remarkTooLong,
                "product.category.remark-too-long",
                "商品分类备注不能超过{0}个字符",
                maximumRemarkLength
        );
    }

    private void validateId(Long id) {
        Assert.isTrue(id == null || id <= 0, "product.category.id-invalid", "商品分类 ID 必须大于0");
    }

    private void validateParentId(Long parentId) {
        Assert.isTrue(parentId == null || parentId < 0, "product.category.parent-id-invalid", "父分类 ID 必须大于或等于0");
    }
}
