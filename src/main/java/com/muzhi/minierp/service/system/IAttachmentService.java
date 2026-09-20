package com.muzhi.minierp.service.system;

import com.muzhi.minierp.entity.system.Attachment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.vo.system.AttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;

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
     * 校验附件存在且未逻辑删除，忽略空 ID。
     */
    void validateAttachments(Collection<Long> attachmentIds);

    /**
     * 批量生成附件授权访问地址，重复 ID 仅签名一次；缺失附件不返回地址。
     */
    Map<Long, String> getAuthorizedUrls(Collection<Long> attachmentIds);

    /**
     * 上传文件，成功后保存并返回附件记录。
     *
     * @param file 已校验的非空文件
     * @param model 已校验的业务模块编码
     * @return 已保存的附件记录
     */
    AttachmentVO upload(MultipartFile file, String model);

}
