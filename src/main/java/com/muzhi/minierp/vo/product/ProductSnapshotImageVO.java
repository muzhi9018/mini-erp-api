package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductSnapshotImage;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 商品快照图片视图
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSnapshotImage.class)
public class ProductSnapshotImageVO extends ProductSnapshotImage {

    /**
     * 图片授权地址
     */
    private String url;
}
