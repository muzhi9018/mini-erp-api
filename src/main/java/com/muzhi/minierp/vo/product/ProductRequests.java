package com.muzhi.minierp.vo.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 商品操作的固定请求与结果结构
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public final class ProductRequests {

    private ProductRequests() {
    }

    /**
     * 带期望版本的删除参数；商品附属资料使用所属商品版本。
     */
    public record VersionedId(Long id, Long expectedVersion) {
    }

    /**
     * SKU 写入结果，同时返回所属商品版本。
     */
    public record SkuResult(Long skuId, BigDecimal stockQuantity, Long skuVersion, Long productVersion) {
    }

    /**
     * 图片保存结果。
     */
    public record ImagesResult(Long productId, Long productVersion, List<ProductImageVO> images) {
    }

    /**
     * SKU 图片保存结果，同时返回变更后的 SKU 行版本与商品版本。
     */
    public record SkuImagesResult(Long skuId, Long skuVersion, Long productVersion, List<ProductImageVO> images) {
    }

    /**
     * 完整创建时通过临时键选择候选值。
     */
    public record ClientSelection(String specClientKey, String valueClientKey) {
    }

    /**
     * 组合预览选项，不写数据库。
     */
    public record PreviewDimension(Long specId, List<Long> specValueIds) {
    }

    /**
     * 组合预览请求。
     */
    public record Preview(Long productId, List<PreviewDimension> dimensions) {
    }
}
