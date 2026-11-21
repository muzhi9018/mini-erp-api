package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.ProductSpec;
import com.muzhi.minierp.vo.product.*;

import java.util.List;

/**
 * <p>
 * 商品结构化规格服务，维度和值统一维护
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSpecService extends IService<ProductSpec> {

    /**
     * 完整创建事务内初始化规格，不单独递增商品版本。
     */
    List<ProductSpecVO> initialize(Long productId, List<ProductSpecVO> specs);

    /**
     * 查询维度与候选值，按排序、ID 返回。
     */
    List<ProductSpecVO> listByProduct(Long productId);

    /**
     * 没有 SKU 及历史业务使用时添加维度。
     */
    ProductSpecVO create(ProductSpecVO query);

    /**
     * 仅修正维度名称及排序。
     */
    ProductSpecVO updateSpec(ProductSpecVO query);

    /**
     * 删除没有引用的维度及其候选值。
     */
    void deleteSpec(ProductRequests.VersionedId query);

    /**
     * 添加指定维度的候选值。
     */
    ProductSpecValueVO createValue(ProductSpecValueVO query);

    /**
     * 修改未被使用的候选值，已使用的值仅允许修改排序。
     */
    ProductSpecValueVO updateValue(ProductSpecValueVO query);

    /**
     * 删除未被使用的候选值。
     */
    void deleteValue(ProductRequests.VersionedId query);
}
