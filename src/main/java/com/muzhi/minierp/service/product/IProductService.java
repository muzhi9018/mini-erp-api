package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.Product;
import com.muzhi.minierp.vo.product.*;

/**
 * <p>
 * 内部商品档案服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductService extends IService<Product> {

    /**
     * 原子创建商品、图片、规格与至少一条 SKU，初始统一版本为 0。
     */
    ProductVO create(ProductVO query);

    /**
     * 分页查询商品；类目包含全部后代，SKU 命中时商品去重。
     */
    IPage<ProductVO> list(ProductQueryVO query, Integer pageNum, Integer pageSize);

    /**
     * 读取当前商品完整详情。
     */
    ProductVO detail(Long productId);

    /**
     * 仅更新共同资料，编码、状态、图片和库存不由此接口修改。
     */
    Product updateProduct(ProductVO query);

    /**
     * 更新商品启停状态，保留 SKU 各自状态。
     */
    Product changeStatus(ProductVO query);

    /**
     * 检查全部 SKU 零库存后逻辑删除主档，返回新商品版本。
     */
    Long delete(ProductRequests.VersionedId query);
}
