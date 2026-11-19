package com.muzhi.minierp.service.product;

import com.muzhi.minierp.entity.product.ProductSnapshotSpec;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品快照规格明细，完整复制当时的名称、选中值和单位，仅新增和查询，不动态读取来源主档 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSnapshotSpecService extends IService<ProductSnapshotSpec> {

}
