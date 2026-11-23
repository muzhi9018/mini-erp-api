package com.muzhi.minierp.service.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.entity.product.ProductSku;
import com.muzhi.minierp.entity.product.ProductSkuSpecValue;
import com.muzhi.minierp.vo.product.*;

import java.util.List;

/**
 * <p>
 * 商品 SKU 管理、选品与人工库存服务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
public interface IProductSkuService extends IService<ProductSku> {

    /**
     * 完整创建事务内保存 SKU，初始商品版本不递增。
     */
    void initialize(Long productId, List<ProductSkuVO> skus, List<ProductSpecVO> specs);

    /**
     * 批量增加实际经营组合，一次递增商品版本。
     */
    List<ProductSkuVO> create(ProductVO query);

    /**
     * 编辑 SKU 资料，数量小数位变更时校验当前库存。
     */
    ProductRequests.SkuResult updateSku(ProductSkuVO query);

    /**
     * 替换 SKU 自身的图片，递增 SKU 行版本及所属商品版本。
     */
    ProductRequests.SkuImagesResult saveImages(ProductSkuVO query);

    /**
     * 启停 SKU，不改变其他 SKU 状态。
     */
    ProductRequests.SkuResult changeStatus(ProductSkuVO query);

    /**
     * 设置当前库存绝对值，校验并递增 SKU 行版本。
     */
    ProductRequests.SkuResult setStock(ProductSkuVO query);

    /**
     * 零库存 SKU 逻辑删除，不影响共享快照。
     */
    ProductRequests.SkuResult delete(ProductRequests.VersionedId query);

    /**
     * 管理分页查询，包含内部参考进价。
     */
    IPage<ProductSkuVO> list(ProductQueryVO query, Integer pageNum, Integer pageSize);

    /**
     * 业务选品，排除停用项及内部管理字段。
     */
    IPage<ProductOptionVO> options(ProductQueryVO query, Integer pageNum, Integer pageSize);

    /**
     * 报价选品，按售价币种筛选并排除未定价项。
     */
    IPage<ProductOptionVO> quotationOptions(ProductQuotationQueryVO query, Integer pageNum, Integer pageSize);

    /**
     * 按 ID 批量读取当前选品资料，由单据服务决定业务可用性。
     */
    List<ProductOptionVO> optionsByIds(List<Long> skuIds);

    /**
     * 查询商品下全部有效 SKU 详情。
     */
    List<ProductSkuVO> listByProduct(Long productId);

    /**
     * 只生成选中候选值的组合预览，不写数据库。
     */
    List<List<ProductSkuSpecValue>> preview(ProductRequests.Preview query);
}
