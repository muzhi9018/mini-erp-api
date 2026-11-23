package com.muzhi.minierp.vo.sales;

import com.muzhi.minierp.entity.sales.Quotation;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * 报价的客户资料。
 *
 * @author Mr.Muzhi
 * @since 2026-11-22
 */
@Data
@AutoMapper(target = Quotation.class, convertGenerate = false)
public class QuotationCustomerVO {

    /**
     * 客户名称。
     */
    private String customerName;

    /**
     * 联系人。
     */
    private String contactName;

    /**
     * 联系电话。
     */
    private String contactPhone;

    /**
     * 收货地址。
     */
    private String shippingAddress;
}
