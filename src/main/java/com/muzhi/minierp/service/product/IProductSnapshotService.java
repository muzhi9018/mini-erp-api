package com.muzhi.minierp.service.product;

import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * SKU 商品版本快照，同一 SKU 与商品版本全表唯一，仅新增和查询，不随来源主档删除 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSnapshotService extends IService<ProductSnapshot> {

}
