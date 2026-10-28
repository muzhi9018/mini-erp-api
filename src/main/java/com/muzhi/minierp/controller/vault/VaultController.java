package com.muzhi.minierp.controller.vault;

import com.muzhi.minierp.component.vault.VaultCodec;
import com.muzhi.minierp.component.vault.VaultChallengeStore;
import com.muzhi.minierp.component.vault.VaultProtocolValidator;
import com.muzhi.minierp.entity.vault.VaultAccount;
import com.muzhi.minierp.entity.vault.VaultSite;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.*;
import com.muzhi.minierp.service.vault.*;
import com.muzhi.minierp.vo.vault.*;
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
@RequestMapping("/vault/api")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultController {

    private final IVaultDataService vaultDataService;

    private final IVaultRotationService vaultRotationService;

    private final IVaultIdentityService iVaultIdentityService;

    private final IVaultSiteService vaultSiteService;

    private final VaultBrowserSession vaultBrowserSession;

    private final VaultChallengeStore challenges;


    @GetMapping("/session")
    public JsonResult<VaultSessionVO> session(HttpServletRequest request, HttpServletResponse response) {
        VaultSession session = VaultContexts.current();
        VaultAccount account = iVaultIdentityService.checkSession(session);
        VaultSessionVO result = vaultBrowserSession.view(session, request, response);
        result.setCredentialRevision(account.getCredentialRevision());
        return JsonResult.success(result);
    }

    @GetMapping("/meta")
    public JsonResult<VaultMetaVO> meta() {
        VaultMetaVO result = vaultDataService.meta(VaultContexts.current());
        return JsonResult.success(result);
    }

    @PostMapping("/initialize")
    public JsonResult<VaultMetaVO> initialize(@RequestBody VaultProtocol.Setup setup) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(setup == null);
        VaultCodec.uuid(setup.vaultId());
        VaultCodec.uuid(setup.idempotencyKey());
        VaultProtocolValidator.slots(setup.keySlots());
        VaultMetaVO result = vaultDataService.initialize(VaultContexts.current(), setup);
        return JsonResult.success(result);
    }

    @GetMapping("/items")
    public JsonResult<VaultItemPageVO<VaultProtocol.Item>> items(@RequestParam(required = false) String cursor, @RequestParam(defaultValue = "100") Integer pageSize) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(pageSize == null || pageSize < 1 || pageSize > 100);
        if (cursor != null) {
            VaultCodec.decode(cursor, 1, 512);
        }
        VaultItemPageVO<VaultProtocol.Item> result = vaultDataService.items(VaultContexts.current(), cursor, pageSize);
        return JsonResult.success(result);
    }

    /**
     * 查询未到期的已删除密文，不与正常条目混用游标。
     */
    @GetMapping("/items/deleted")
    public JsonResult<VaultItemPageVO<VaultItemVO>> deletedItems(@RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(pageSize == null || pageSize < 1 || pageSize > 100);
        if (cursor != null) {
            VaultCodec.decode(cursor, 1, 512);
        }
        VaultItemPageVO<VaultItemVO> result = vaultDataService.deletedItems(VaultContexts.current(), cursor, pageSize);
        return JsonResult.success(result);
    }

    /**
     * 打开详情时重新确认归属、删除状态与期限。
     */
    @GetMapping("/items/deleted/detail")
    public JsonResult<VaultItemVO> deletedDetail(@RequestParam String itemId) {
        VaultCodec.uuid(itemId);
        VaultItemVO result = vaultDataService.deletedDetail(VaultContexts.current(), itemId);
        return JsonResult.success(result);
    }

    /**
     * 恢复原条目，不接收客户端密文及删除时间。
     */
    @PostMapping("/items/restore")
    public JsonResult<VaultItemResultVO> restoreItem(@RequestBody VaultProtocol.ItemLifecycle value) {
        VaultProtocolValidator.itemLifecycle(value);
        VaultItemResultVO result = vaultDataService.restoreItem(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    /**
     * 永久删除已删除条目，必须提供绑定本次请求的 Passkey 证明。
     */
    @PostMapping("/items/purge")
    public JsonResult<VaultItemResultVO> purgeItem(@RequestBody VaultProtocol.ItemLifecycle value, HttpServletRequest request) {
        VaultProtocolValidator.itemLifecycle(value);
        this.limitDeletion(request);
        VaultItemResultVO result = vaultDataService.purgeItem(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    @PostMapping("/items/create")
    public JsonResult<VaultItemResultVO> create(@RequestBody VaultProtocol.ItemWrite value) {
        VaultProtocolValidator.itemWrite(value, true);
        VaultEnum.Error.INVALID_REQUEST.rejectIf(!Long.valueOf(1).equals(value.contentVersion()));
        VaultItemResultVO result = vaultDataService.save(VaultContexts.current(), value, true);
        return JsonResult.success(result);
    }

    @PostMapping("/items/update")
    public JsonResult<VaultItemResultVO> update(@RequestBody VaultProtocol.ItemWrite value) {
        VaultProtocolValidator.itemWrite(value, true);
        VaultCodec.version(value.expectedRowVersion());
        VaultItemResultVO result = vaultDataService.save(VaultContexts.current(), value, false);
        return JsonResult.success(result);
    }

    @PostMapping("/items/delete")
    public JsonResult<VaultItemResultVO> delete(@RequestBody VaultProtocol.ItemWrite value, HttpServletRequest request) {
        VaultProtocolValidator.itemWrite(value, false);
        VaultCodec.version(value.expectedRowVersion());
        this.limitDeletion(request);
        VaultItemResultVO result = vaultDataService.delete(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    /**
     * 直接删除客户端解密后确认的账号组，成员密码及 TOTP 不受影响。
     */
    @PostMapping("/items/delete-group")
    public JsonResult<VaultItemResultVO> deleteGroup(@RequestBody VaultProtocol.ItemLifecycle value, HttpServletRequest request) {
        VaultProtocolValidator.itemLifecycle(value);
        this.limitDeletion(request);
        VaultItemResultVO result = vaultDataService.deleteGroup(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    /**
     * 原子保存账号组及必要的独立条目变更，任意变更失败时整批回滚。
     */
    @PostMapping("/items/batch")
    public JsonResult<VaultItemBatchResultVO> batch(@RequestBody VaultProtocol.ItemBatch value, HttpServletRequest request) {
        VaultProtocolValidator.itemBatch(value);
        if (VaultProtocolValidator.hasDeletion(value)) {
            this.limitDeletion(request);
        }
        VaultItemBatchResultVO result = vaultDataService.batch(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    private void limitDeletion(HttpServletRequest request) {
        VaultSession session = VaultContexts.current();
        challenges.rate("credential-account", session.ownerId().toString(), 20);
        challenges.rate("credential-source", request.getRemoteAddr(), 60);
    }

    @PostMapping("/key-slots/change-master")
    public JsonResult<VaultMetaVO> changeMaster(@RequestBody VaultProtocol.MasterChange change, HttpServletRequest request, HttpServletResponse response) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(change == null);
        VaultCodec.version(change.expectedMetaRevision());
        VaultCodec.uuid(change.idempotencyKey());
        VaultProtocolValidator.slot(change.normalSlot());
        VaultEnum.Error.INVALID_REQUEST.rejectIf(!VaultEnum.SlotType.NORMAL.getCode().equals(change.normalSlot().slotType()));
        VaultMetaVO result = vaultDataService.changeMaster(VaultContexts.current(), change);
        vaultBrowserSession.clear(request, response, true);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/start")
    public JsonResult<VaultRotationVO> start(@RequestBody VaultProtocol.RotationStart value) {
        VaultProtocolValidator.rotationStart(value, false);
        VaultRotationVO result = vaultRotationService.start(VaultContexts.current(), value, VaultEnum.RotationMode.ROTATION);
        return JsonResult.success(result);
    }

    @PostMapping("/restores/start")
    public JsonResult<VaultRotationVO> restore(@RequestBody VaultProtocol.RotationStart value) {
        VaultProtocolValidator.rotationStart(value, true);
        VaultRotationVO result = vaultRotationService.start(VaultContexts.current(), value, VaultEnum.RotationMode.RESTORE);
        return JsonResult.success(result);
    }

    @GetMapping("/rotations/status")
    public JsonResult<VaultRotationStatusVO> status(@RequestParam String rotationId) {
        VaultCodec.uuid(rotationId);
        VaultRotationStatusVO result = vaultRotationService.status(VaultContexts.current(), rotationId);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/batch")
    public JsonResult<VaultRotationBatchVO> batch(@RequestBody VaultProtocol.RotationBatch value) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(value == null || value.items() == null || value.items().isEmpty() || value.items().size() > 100);
        VaultCodec.uuid(value.rotationId());
        VaultCodec.uuid(value.batchId());
        Set<String> seen = new HashSet<>();
        for (VaultProtocol.Item item : value.items()) {
            VaultProtocolValidator.item(item);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(!seen.add(item.itemId()));
        }
        VaultRotationBatchVO result = vaultRotationService.batch(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    @GetMapping("/rotations/items")
    public JsonResult<VaultItemPageVO<VaultItemVO>> stagedItems(@RequestParam String rotationId, @RequestParam(defaultValue = "STAGING") String scope, @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "100") Integer pageSize) {
        VaultCodec.uuid(rotationId);
        VaultEnum.Error.INVALID_REQUEST.rejectIf(pageSize == null || pageSize < 1 || pageSize > 100);
        if (cursor != null) {
            VaultCodec.decode(cursor, 1, 512);
        }
        VaultEnum.ItemScope itemScope = VaultEnum.ofCode(VaultEnum.ItemScope.class, scope);
        VaultItemPageVO<VaultItemVO> result = vaultRotationService.items(VaultContexts.current(), rotationId, itemScope, cursor, pageSize);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/commit")
    public JsonResult<VaultRotationReceiptVO> commit(@RequestBody VaultProtocol.RotationCommand value, HttpServletRequest request, HttpServletResponse response) {
        this.validateCommand(value, true);
        VaultCodec.decode(value.verifiedManifestDigest(), 32, 32);
        VaultRotationReceiptVO result = vaultRotationService.commit(VaultContexts.current(), value);
        vaultBrowserSession.clear(request, response, true);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/resume")
    public JsonResult<VaultRotationVO> resume(@RequestBody VaultProtocol.RotationCommand value) {
        this.validateCommand(value, true);
        VaultRotationVO result = vaultRotationService.resume(VaultContexts.current(), value);
        return JsonResult.success(result);
    }

    @PostMapping("/rotations/cancel")
    public JsonResult<VaultRotationVO> cancel(@RequestBody VaultProtocol.RotationCommand value) {
        this.validateCommand(value, false);
        VaultRotationVO result = vaultRotationService.cancel(VaultContexts.current(), value);
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
        try (VaultSnapshot snapshot = vaultDataService.snapshot(session)) {
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentLengthLong(snapshot.contentLength());
            snapshot.transferTo(response.getOutputStream());
        }
    }

    @GetMapping("/site/list")
    public JsonResult<List<VaultSite>> vaultSites() {
        List<VaultSite> list = vaultSiteService.list();
        return JsonResult.success(list);
    }
}
