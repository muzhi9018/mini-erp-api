package com.muzhi.minierp.vo.sales;

/**
 * 报价或模板的业务 ID 与期望版本。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationVersionedId(Long id, Long expectedVersion) {
}
