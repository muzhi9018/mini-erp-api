package com.muzhi.minierp.vo.website;

import com.muzhi.minierp.entity.website.WebsiteProductMediaItem;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 官网商品媒体明细及图片授权地址。
 *
 * @author Mr.Muzhi
 * @since 2026-09-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WebsiteProductMediaItem.class)
public class WebsiteProductMediaItemVO extends WebsiteProductMediaItem {

    @Serial
    private static final long serialVersionUID = -6990257989311565481L;
    
    /**
     * 图片授权访问地址，仅用于查询返回。
     */
    private String imageUrl;
}
