package com.muzhi.minierp.controller.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.*;
import com.muzhi.minierp.service.vault.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * {@code VaultController}：当前用户密文及轮换协议入口，不接受客户端归属与审计字段
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultController {
    private final IVaultDataService data;

    private final IVaultRotationService rotations;

    private final IVaultIdentityService identities;

    private final VaultBrowserSession browser;

    @GetMapping("/session")
    public JsonResult<Map<String, Object>> session(HttpServletRequest request, HttpServletResponse response) {
        VaultSession session = VaultContexts.current();
        var account = identities.checkSession(session);
        Map<String, Object> result = browser.view(session, request, response);
        result.put("credentialRevision", account.getCredentialRevision());
        return JsonResult.success(result);
    }

    @GetMapping("/meta")
    public JsonResult<Map<String, Object>> meta() {
        Map<String, Object> result = data.meta(VaultContexts.current());
        return JsonResult.success(result);
    }

    @PostMapping("/initialize")
    public JsonResult<Map<String, Object>> initialize(@RequestBody VaultProtocol.Setup setup) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(setup == null);
        VaultCodec.uuid(setup.vaultId());
        VaultCodec.uuid(setup.idempotencyKey());
        VaultProtocolValidator.slots(setup.keySlots());
        Map<String, Object> result = data.initialize(VaultContexts.current(), setup);
        return JsonResult.success(result);
    }

    @GetMapping("/items")
    public JsonResult<Map<String, Object>> items(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "100") Integer pageSize) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(pageSize == null || pageSize < 1 || pageSize > 100);
        if (cursor != null) {
            VaultCodec.decode(cursor, 1, 256);
        }
        Map<String, Object> result = data.items(VaultContexts.current(), cursor, pageSize);
        return JsonResult.success(result);
    }

    @PostMapping("/items/create")
    public JsonResult<Map<String, Object>> create(@RequestBody VaultProtocol.ItemWrite value) {
        this.validateItem(value, true);
        VaultEnum.Error.INVALID_REQUEST.rejectIf(!Long.valueOf(1).equals(value.contentVersion()));
        Map<String, Object> result = data.save(VaultContexts.current(), value, true);
        return JsonResult.success(result);
    }

    @PostMapping("/items/update")
    public JsonResult<Map<String, Object>> update(@RequestBody VaultProtocol.ItemWrite value) {
        this.validateItem(value, true);
        VaultCodec.version(value.expectedRowVersion());
        Map<String, Object> result = data.save(VaultContexts.current(), value, false);
        return JsonResult.success(result);
    }

    @PostMapping("/items/delete")
    public JsonResult<Map<String, Object>> delete(@RequestBody VaultProtocol.ItemWrite value) {
        this.validateItem(value, false);
        VaultCodec.version(value.expectedRowVersion());
        Map<String, Object> result = data.delete(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    private void validateItem(VaultProtocol.ItemWrite value, boolean content) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null);
        VaultCodec.uuid(value.itemId());
        VaultCodec.uuid(value.idempotencyKey());
        VaultCodec.version(value.generation());
        if (content) {
            VaultCodec.version(value.keyEpoch());
            VaultCodec.version(value.contentVersion());
            VaultProtocolValidator.box(value.wrappedDek(), false);
            VaultProtocolValidator.box(value.encryptedItem(), true);
        }
    }

    @PostMapping("/key-slots/change-master")
    public JsonResult<Map<String, Object>> changeMaster(@RequestBody VaultProtocol.MasterChange change, HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(change == null);
        VaultCodec.version(change.expectedMetaRevision());
        VaultCodec.uuid(change.idempotencyKey());
        VaultProtocolValidator.slot(change.normalSlot());
        VaultEnum.Error.INVALID_REQUEST.rejectIf(!VaultEnum.SlotType.NORMAL.getCode().equals(change.normalSlot().slotType()));
        Map<String, Object> result = data.changeMaster(VaultContexts.current(), change);
        browser.clear(request, response, true);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/start")
    public JsonResult<Map<String, Object>> start(@RequestBody VaultProtocol.RotationStart value) {
        VaultProtocolValidator.rotationStart(value, false);
        Map<String, Object> result = rotations.start(VaultContexts.current(), value, VaultEnum.RotationMode.ROTATION);
        return JsonResult.success(result);
    }

    @PostMapping("/restores/start")
    public JsonResult<Map<String, Object>> restore(@RequestBody VaultProtocol.RotationStart value) {
        VaultProtocolValidator.rotationStart(value, true);
        Map<String, Object> result = rotations.start(VaultContexts.current(), value, VaultEnum.RotationMode.RESTORE);
        return JsonResult.success(result);
    }

    @GetMapping("/rotations/status")
    public JsonResult<Map<String, Object>> status(@RequestParam String rotationId) {
        VaultCodec.uuid(rotationId);
        Map<String, Object> result = rotations.status(VaultContexts.current(), rotationId);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/batch")
    public JsonResult<Map<String, Object>> batch(@RequestBody VaultProtocol.RotationBatch value) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null || value.items() == null || value.items().isEmpty() || value.items().size() > 100);
        VaultCodec.uuid(value.rotationId());
        VaultCodec.uuid(value.batchId());
        Set<String> seen = new HashSet<>();
        for (VaultProtocol.Item item : value.items()) {
            VaultProtocolValidator.item(item);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(!seen.add(item.itemId()));
        }
        Map<String, Object> result = rotations.batch(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    @GetMapping("/rotations/items")
    public JsonResult<Map<String, Object>> stagedItems(@RequestParam String rotationId, @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "100") Integer pageSize) {
        VaultCodec.uuid(rotationId);
        VaultEnum.Error.INVALID_REQUEST.rejectIf(pageSize == null || pageSize < 1 || pageSize > 100);
        if (cursor != null) {
            VaultCodec.decode(cursor, 1, 256);
        }
        Map<String, Object> result = rotations.items(VaultContexts.current(), rotationId, cursor, pageSize);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/commit")
    public JsonResult<Map<String, Object>> commit(@RequestBody VaultProtocol.RotationCommand value, HttpServletRequest request, HttpServletResponse response) {
        this.validateCommand(value, true);
        VaultCodec.decode(value.verifiedManifestDigest(), 32, 32);
        Map<String, Object> result = rotations.commit(VaultContexts.current(), value);
        browser.clear(request, response, true);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/resume")
    public JsonResult<Map<String, Object>> resume(@RequestBody VaultProtocol.RotationCommand value) {
        this.validateCommand(value, true);
        Map<String, Object> result = rotations.resume(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/cancel")
    public JsonResult<Map<String, Object>> cancel(@RequestBody VaultProtocol.RotationCommand value) {
        this.validateCommand(value, false);
        Map<String, Object> result = rotations.cancel(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    private void validateCommand(VaultProtocol.RotationCommand value, boolean revision) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null);
        VaultCodec.uuid(value.rotationId());
        VaultCodec.uuid(value.idempotencyKey());
        if (revision) {
            VaultCodec.version(value.expectedCollectionRevision());
        }
    }

    /**
     * 流式写出完整 JsonResult 快照，文件生成事务结束后再传输，避免慢客户端下载持有业务锁。
     */
    @GetMapping("/backup-snapshot")
    public void snapshot(HttpServletResponse response) throws IOException {
        VaultSession session = VaultContexts.current();
        try (VaultSnapshot snapshot = data.snapshot(session)) {
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentLengthLong(snapshot.contentLength());
            snapshot.transferTo(response.getOutputStream());
        }
    }
}
