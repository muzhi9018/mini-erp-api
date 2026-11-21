package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.ProductUnit;

import java.util.List;

/**
 * <p>
 * 商品计量单位服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductUnitService extends IService<ProductUnit> {

    /**
     * 查询有效单位，按排序、ID 返回。
     */
    List<ProductUnit> listUnits();

    /**
     * 创建编码不可复用的计量单位。
     */
    ProductUnit create(ProductUnit query);

    /**
     * 修改单位名称和排序，名称变更时递增受影响商品的版本。
     */
    ProductUnit updateUnit(ProductUnit query);

    /**
     * 删除无有效规格及 SKU 引用的单位。
     */
    void delete(Long id);

    /**
     * 在已有业务事务内获取有效单位并加共享锁，调用前需先锁定所属商品。
     *
     * @param id 单位 ID
     * @return 已校验的有效单位
     */
    ProductUnit requireUnit(Long id);
}
