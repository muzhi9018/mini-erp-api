package com.muzhi.minierp.mapper.sales;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.sales.Quotation;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * <p>
 * 正式报价单，保存客户、开单方及计价结果作为历史记录 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public interface QuotationMapper extends BaseMapper<Quotation> {

    /**
     * 在保存事务中获取台北编号日的取号锁。
     */
    int lockDailySequence(@Param("numberDate") LocalDate numberDate);

    /**
     * 取号锁取得后查询已发最大流水，包含已逻辑删除的记录。
     */
    Integer selectMaxDailySequence(@Param("numberDate") LocalDate numberDate);
}
