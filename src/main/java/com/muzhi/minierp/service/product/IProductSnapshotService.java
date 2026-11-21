package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.ProductSnapshot;
import com.muzhi.minierp.vo.product.ProductSnapshotVO;

/**
 * <p>
 * 商品快照业务通过获取或创建及读取方法处理
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSnapshotService extends IService<ProductSnapshot> {

    /**
     * 在单据保存事务中按 SKU 和当前商品版本获取或创建完整快照。
     * 调用方先完成单据写权限校验；发生需回滚的数据库冲突时重试整个单据事务。
     *
     * @param skuId 来源 SKU ID
     * @param expectedProductVersion 选品确认的商品版本
     * @param expectedSkuVersion 选品确认的 SKU 行版本
     * @return 已有或完整新建的不可变快照
     */
    ProductSnapshotVO getOrCreate(Long skuId, Long expectedProductVersion, Long expectedSkuVersion);

    /**
     * 单据服务确认当前用户可读关联单据后，读取冻结资料。
     * 此方法仅供已鉴权的单据服务调用，不可直接作为商品 HTTP 查询入口。
     */
    ProductSnapshotVO readForDocument(Long snapshotId);
}
