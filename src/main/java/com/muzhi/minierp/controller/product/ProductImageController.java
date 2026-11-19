package com.muzhi.minierp.controller.product;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 商品图片，同一商品下 SKU 共用，非空图片列表恰好一张主图由应用事务保证 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/productImage")
public class ProductImageController {

}
