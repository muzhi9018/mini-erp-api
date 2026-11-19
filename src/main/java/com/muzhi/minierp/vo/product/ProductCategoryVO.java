package com.muzhi.minierp.vo.product;

import com.muzhi.minierp.entity.product.ProductCategory;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 商品类目树视图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProductCategory.class)
public class ProductCategoryVO extends ProductCategory {

    @Serial
    private static final long serialVersionUID = 4824178605132040901L;

    /**
     * 有效子类目，叶节点返回空列表
     */
    private List<ProductCategoryVO> children;
}
