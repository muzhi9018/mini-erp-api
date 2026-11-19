package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductSnapshotSpec;

/**
 * <p>
 * 商品快照规格明细，完整复制当时的名称、选中值和单位，仅新增和查询，不动态读取来源主档 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductSnapshotSpecMapper extends BaseMapper<ProductSnapshotSpec> {

}
