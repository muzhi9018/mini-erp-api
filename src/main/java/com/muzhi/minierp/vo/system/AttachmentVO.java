package com.muzhi.minierp.vo.system;

import com.muzhi.minierp.entity.system.Attachment;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <p>
 * {@code AttachmentVO}: 附件信息表 试图对象
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/20 16:34
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMappers({@AutoMapper(target = Attachment.class)})
public class AttachmentVO extends Attachment {

    @Serial
    private static final long serialVersionUID = -8233378952249977168L;

    /**
     * 生成预览地址
     */
    private String previewUrl;

}
