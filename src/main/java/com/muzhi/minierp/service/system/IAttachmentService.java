package com.muzhi.minierp.service.system;

import com.muzhi.minierp.entity.system.Attachment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 附件信息表，用于记录上传到对象存储中的文件信息 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-16
 */
public interface IAttachmentService extends IService<Attachment> {

    /**
     * 上传文件，成功后保存并返回附件记录。
     *
     * @param file 已校验的非空文件
     * @param model 已校验的业务模块编码
     * @return 已保存的附件记录
     */
    Attachment upload(MultipartFile file, String model);

}
