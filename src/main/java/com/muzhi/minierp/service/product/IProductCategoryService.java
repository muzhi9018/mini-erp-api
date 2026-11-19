package com.muzhi.minierp.service.product;

import com.muzhi.minierp.entity.product.ProductCategory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.vo.product.ProductCategoryVO;

import java.util.List;

/**
 * <p>
 * 内部商品类目 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductCategoryService extends IService<ProductCategory> {

    /**
     * 查询有效类目树，同级按展示顺序、ID 升序排列。
     */
    List<ProductCategoryVO> tree();

    /**
     * 新增类目，编码全表唯一，同父级的有效类目名称唯一。
     */
    ProductCategory create(ProductCategory query);

    /**
     * 修改名称、排序和备注；改名时同步递增子树内商品版本。
     */
    ProductCategory updateCategory(ProductCategory query);

    /**
     * 逻辑删除空类目，有未删除子类目或商品时拒绝删除。
     */
    void delete(Long id);
}
