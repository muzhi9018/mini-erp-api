package com.muzhi.minierp.service.system.impl;

import com.muzhi.minierp.entity.system.Attachment;
import com.muzhi.minierp.mapper.system.AttachmentMapper;
import com.muzhi.minierp.service.system.IAttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 附件信息表，用于记录上传到对象存储中的文件信息 服务实现类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-16
 */
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment> implements IAttachmentService {

}
