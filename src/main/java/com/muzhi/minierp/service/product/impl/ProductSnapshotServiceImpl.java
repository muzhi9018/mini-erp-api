package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.muzhi.minierp.mapper.product.ProductSnapshotMapper;
import com.muzhi.minierp.service.product.IProductSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * SKU 商品版本快照，同一 SKU 与商品版本全表唯一，仅新增和查询，不随来源主档删除 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSnapshotServiceImpl extends ServiceImpl<ProductSnapshotMapper, ProductSnapshot> implements IProductSnapshotService {

}
