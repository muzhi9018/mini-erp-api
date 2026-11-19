package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSnapshotSpec;
import com.muzhi.minierp.mapper.product.ProductSnapshotSpecMapper;
import com.muzhi.minierp.service.product.IProductSnapshotSpecService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品快照规格明细，完整复制当时的名称、选中值和单位，仅新增和查询，不动态读取来源主档 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSnapshotSpecServiceImpl extends ServiceImpl<ProductSnapshotSpecMapper, ProductSnapshotSpec> implements IProductSnapshotSpecService {

}
