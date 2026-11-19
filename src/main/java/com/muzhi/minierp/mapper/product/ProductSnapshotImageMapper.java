package com.muzhi.minierp.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.product.ProductSnapshotImage;

/**
 * <p>
 * 商品快照图片明细，完整保留当时的全部图片、主图标记和顺序，仅新增和查询，不随来源图片删除 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface ProductSnapshotImageMapper extends BaseMapper<ProductSnapshotImage> {

}
