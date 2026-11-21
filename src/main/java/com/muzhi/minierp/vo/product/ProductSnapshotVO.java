package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.muzhi.minierp.entity.product.ProductSnapshotSpec;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 不可变商品快照详情
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductSnapshot.class)
public class ProductSnapshotVO extends ProductSnapshot {

    /**
     * 冻结的规格明细
     */
    private List<ProductSnapshotSpec> specs;

    /**
     * 冻结的完整图片列表
     */
    private List<ProductSnapshotImageVO> images;
}
