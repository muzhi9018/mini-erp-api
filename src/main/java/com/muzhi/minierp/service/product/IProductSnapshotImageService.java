package com.muzhi.minierp.service.product;

import com.muzhi.minierp.entity.product.ProductSnapshotImage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品快照图片明细，完整保留当时的全部图片、主图标记和顺序，仅新增和查询，不随来源图片删除 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSnapshotImageService extends IService<ProductSnapshotImage> {

}
