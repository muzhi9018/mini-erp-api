package com.muzhi.minierp.vo.sales;

/**
 * 从原报价复制时选择是否清除客户资料。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
public record QuotationCopyRequest(Long id, Boolean clearCustomer) {
}
