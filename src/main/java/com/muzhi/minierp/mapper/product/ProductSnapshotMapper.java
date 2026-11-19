package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductSnapshot;

/**
 * <p>
 * SKU 商品版本快照，同一 SKU 与商品版本全表唯一，仅新增和查询，不随来源主档删除 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductSnapshotMapper extends BaseMapper<ProductSnapshot> {

}
