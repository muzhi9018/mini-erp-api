package com.muzhi.minierp.controller.product;

import com.muzhi.minierp.entity.product.ProductUnit;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.product.IProductUnitService;
import com.muzhi.minierp.util.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品计量单位前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/product/unit")
@RequiredArgsConstructor
public class ProductUnitController {

    private final IProductUnitService unitService;

    /**
     * 查询全部有效单位。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('product-manager:product-unit:query')")
    public JsonResult<List<ProductUnit>> list() {
        List<ProductUnit> units = unitService.listUnits();
        return JsonResult.success(units);
    }

    /**
     * 创建单位。
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('product-manager:product-unit:create')")
    public JsonResult<ProductUnit> create(@RequestBody ProductUnit query) {
        this.validateUnit(query);
        String code = query.getCode();
        Assert.isTrue(code == null || code.isBlank() || code.trim().length() > 32,
                "product.unit.code-invalid", "单位编码不能为空且不能超过32个字符");
        ProductUnit unit = unitService.create(query);
        return JsonResult.success(unit);
    }

    /**
     * 修改单位名称及排序。
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('product-manager:product-unit:update')")
    public JsonResult<ProductUnit> update(@RequestBody ProductUnit query) {
        this.validateUnit(query);
        this.validateId(query.getId());
        ProductUnit unit = unitService.updateUnit(query);
        return JsonResult.success(unit);
    }

    /**
     * 删除未被有效主档引用的单位。
     */
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('product-manager:product-unit:delete')")
    public JsonResult<Void> delete(@RequestBody ProductUnit query) {
        Assert.isNull(query, "product.unit.required", "单位信息不能为空");
        this.validateId(query.getId());
        unitService.delete(query.getId());
        return JsonResult.success();
    }

    private void validateId(Long id) {
        Assert.isTrue(id == null || id <= 0, "product.unit.id-invalid", "单位 ID 必须大于0");
    }

    private void validateUnit(ProductUnit query) {
        Assert.isNull(query, "product.unit.required", "单位信息不能为空");
        String name = query.getName();
        Assert.isTrue(name == null || name.isBlank() || name.trim().length() > 32,
                "product.unit.name-invalid", "单位名称不能为空且不能超过32个字符");
    }
}
