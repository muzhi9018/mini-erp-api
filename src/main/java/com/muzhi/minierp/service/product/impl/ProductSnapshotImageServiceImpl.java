package com.muzhi.minierp.service.product.impl;

import com.muzhi.minierp.entity.product.ProductSnapshotImage;
import com.muzhi.minierp.mapper.product.ProductSnapshotImageMapper;
import com.muzhi.minierp.service.product.IProductSnapshotImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品快照图片明细，完整保留当时的全部图片、主图标记和顺序，仅新增和查询，不随来源图片删除 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@Service
public class ProductSnapshotImageServiceImpl extends ServiceImpl<ProductSnapshotImageMapper, ProductSnapshotImage> implements IProductSnapshotImageService {

}
