package com.muzhi.minierp.controller.system;

import com.muzhi.minierp.client.listener.ProgressListener;
import com.muzhi.minierp.vo.system.AttachmentVO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.muzhi.minierp.service.oss.IProgressCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.system.IAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 附件信息表，用于记录上传到对象存储中的文件信息 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-09-16
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/attachment")
public class AttachmentController {

    private final IAttachmentService attachmentService;

    private final IProgressCacheService progressCacheService;

    @PostMapping("/upload")
    public JsonResult<AttachmentVO> upload(@RequestParam("file") MultipartFile file, @RequestParam("model") String model) {
        Assert.isNull(file, "system.attachment.file-required", "请选择上传文件");
        Assert.isTrue(file.isEmpty(), "system.attachment.file-empty", "上传文件不能为空");
        AttachmentVO attachment = attachmentService.upload(file, model);
        return JsonResult.success(attachment);
    }

    /**
     * 通过 SSE 订阅当前用户的 OSS 传输进度，支持先订阅后上传和断线重连。
     */
    @GetMapping(value = "/upload/progress/{progressId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> progress(@PathVariable Long progressId) {
        ProgressListener progressListener = progressCacheService.getProgressListener(progressId);
        Assert.isNull(progressListener, "");
        SseEmitter emitter = progressListener.getSseEmitter();
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

}
