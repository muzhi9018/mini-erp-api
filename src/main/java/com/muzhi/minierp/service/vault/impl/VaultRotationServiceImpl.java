package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.muzhi.minierp.component.vault.VaultAccess;
import com.muzhi.minierp.component.vault.VaultCipherStore;
import com.muzhi.minierp.component.vault.VaultCodec;
import com.muzhi.minierp.component.vault.VaultIdempotency;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.VaultContexts;
import com.muzhi.minierp.service.vault.*;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.vault.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * <p>
 * {@code VaultRotationServiceImpl}：轮换的归属、暂存批次、完整集合与数据库提交事务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultRotationServiceImpl implements IVaultRotationService {

    private final VaultAccess vaultAccess;

    private final VaultIdempotency vaultIdempotency;

    private final VaultCipherStore vaultCipherStore;

    private final VaultControlMapper vaultControlMapper;

    private final VaultInfoMapper vaultInfoMapper;

    private final VaultGenerationMapper vaultGenerationMapper;

    private final VaultRotationMapper vaultRotationMapper;

    private final VaultRotationExpectedItemMapper vaultRotationExpectedItemMapper;

    private final VaultRotationBatchMapper vaultRotationBatchMapper;

    private final VaultItemMapper vaultItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationVO start(VaultSession session, VaultProtocol.RotationStart request, VaultEnum.RotationMode mode) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultEnum.Operation operation = mode == VaultEnum.RotationMode.ROTATION ? VaultEnum.Operation.ROTATION_START : VaultEnum.Operation.RESTORE_START;
        return vaultIdempotency.execute(
                session,
                operation,
                request.idempotencyKey(),
                request,
                VaultRotationVO.class,
                () -> this.startTask(session, request, mode)
        );
    }

    private VaultRotationVO startTask(VaultSession session, VaultProtocol.RotationStart request, VaultEnum.RotationMode mode) {
        VaultInfo vault = vaultControlMapper.lockVault(session.accountId());
        if (vault == null) {
            VaultEnum.Error.NOT_FOUND.rejectIf(mode != VaultEnum.RotationMode.RESTORE);
            vault = new VaultInfo();
            vault.setId(IdWorker.getId());
            vault.setVaultId(VaultCodec.uuid(request.vaultId()));
            vault.setAccountId(session.accountId());
            vault.setStatus(VaultEnum.VaultStatus.EMPTY.getCode());
            vault.setMetaRevision(1L);
            vault.setCollectionRevision(1L);
            vaultAccess.create(vault, session.ownerId());
            vaultInfoMapper.insert(vault);
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getMetaRevision().equals(request.expectedMetaRevision())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        LambdaQueryWrapper<VaultRotation> open = Wrappers.lambdaQuery();
        open.eq(VaultRotation::getVaultId, vault.getId());
        open.eq(VaultRotation::getStatus, VaultEnum.RotationStatus.UPLOADING.getCode());
        VaultRotation existing = vaultRotationMapper.selectOne(open);
        if (existing != null && !existing.getExpiresAt().isAfter(vaultControlMapper.databaseNow())) {
            existing = vaultControlMapper.lockRotation(vault.getId(), existing.getRotationId());
            this.finish(vault, existing, VaultEnum.RotationStatus.EXPIRED, null);
            existing = null;
        }
        VaultEnum.Error.WRITE_LOCKED.rejectIf(existing != null);
        OffsetDateTime now = vaultControlMapper.databaseNow();
        OffsetDateTime before = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);

        if (mode == VaultEnum.RotationMode.ROTATION) {
            VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        } else {
            long sourceCount = vault.getActiveGeneration() == null ? 0
                    : vaultItemMapper.retainedCount(vault.getId(), vault.getActiveGeneration(), before);
            VaultEnum.Error.ALREADY_INITIALIZED.rejectIf(sourceCount != 0);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getVaultId().equals(VaultCodec.uuid(request.vaultId())));
        }
        VaultGeneration sourceGeneration = vault.getActiveGeneration() == null ? null : vaultCipherStore.generation(vault, vault.getActiveGeneration());
        long epoch = sourceGeneration == null ? 1 : sourceGeneration.getKeyEpoch() + 1;
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(epoch > VaultProtocol.MAX_VERSION);
        long target = vaultCipherStore.nextGeneration(vault);
        vaultCipherStore.addGeneration(vault, target, epoch, VaultEnum.GenerationStatus.STAGING, session.ownerId());
        vaultCipherStore.addSlots(vault, target, request.keySlots(), session.ownerId());
        VaultRotation task = new VaultRotation();
        task.setId(IdWorker.getId());
        task.setRotationId(VaultCodec.uuid(request.rotationId()));
        task.setVaultId(vault.getId());
        task.setMode(mode.getCode());
        task.setStatus(VaultEnum.RotationStatus.UPLOADING.getCode());
        task.setSourceGeneration(vault.getActiveGeneration());
        task.setTargetGeneration(target);
        task.setExpectedMetaRevision(vault.getMetaRevision());
        task.setExpectedCollectionRevision(vault.getCollectionRevision());
        task.setStartedAuthEpoch(session.authEpoch());
        task.setSessionBindingId(session.bindingId());
        task.setExpectedItemCount(mode == VaultEnum.RotationMode.ROTATION ? 0 : request.itemIds().size());
        task.setGmtCreate(now);
        task.setExpiresAt(now.plusMinutes(30));
        vaultAccess.create(task, session.ownerId());
        vaultRotationMapper.insert(task);
        if (mode == VaultEnum.RotationMode.ROTATION) {
            int count = vaultItemMapper.freezeSource(
                    vault.getId(),
                    vault.getActiveGeneration(),
                    task.getId(),
                    before,
                    now,
                    session.ownerId()
            );
            task.setExpectedItemCount(count);
            vaultRotationMapper.updateById(task);
        } else {
            for (String itemId : request.itemIds()) {
                this.expected(task, VaultCodec.uuid(itemId), null, null, 1L, session.ownerId());
            }
            vault.setStatus(VaultEnum.VaultStatus.RESTORING.getCode());
            vaultAccess.touch(vault, session.ownerId());
            vaultInfoMapper.updateById(vault);
        }
        vaultAccess.audit(session, vault.getId(), mode == VaultEnum.RotationMode.ROTATION ? VaultEnum.AuditAction.ROTATION_START : VaultEnum.AuditAction.RESTORE_START);
        return this.summary(vault, task);
    }

    private void expected(VaultRotation task, UUID itemId, Long sourceContent, Long sourceRow, Long targetContent, Long actor) {
        VaultRotationExpectedItem item = new VaultRotationExpectedItem();
        item.setRotationId(task.getId());
        item.setItemId(itemId);
        item.setSourceContentVersion(sourceContent);
        item.setSourceRowVersion(sourceRow);
        item.setTargetContentVersion(targetContent);
        vaultAccess.create(item, actor);
        vaultRotationExpectedItemMapper.insert(item);
    }

    private List<VaultRotationExpectedItem> expected(VaultRotation task) {
        LambdaQueryWrapper<VaultRotationExpectedItem> query = Wrappers.lambdaQuery();
        query.eq(VaultRotationExpectedItem::getRotationId, task.getId());
        query.orderByAsc(VaultRotationExpectedItem::getItemId);
        return vaultRotationExpectedItemMapper.selectList(query);
    }

    private VaultRotation task(VaultInfo vault, String id) {
        VaultRotation task = vaultControlMapper.lockRotation(vault.getId(), VaultCodec.uuid(id));
        VaultEnum.Error.NOT_FOUND.rejectIf(task == null);
        return task;
    }

    private void usable(VaultSession session, VaultRotation task, boolean binding) {
        VaultEnum.Error.ROTATION_EXPIRED.rejectIf(!VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())
                || !task.getExpiresAt().isAfter(vaultControlMapper.databaseNow()));
        VaultEnum.Error.SESSION_REVOKED.rejectIf(task.getStartedAuthEpoch() != session.authEpoch());
        VaultEnum.Error.MFA_REQUIRED.rejectIf(binding && !task.getSessionBindingId().equals(session.bindingId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationStatusVO status(VaultSession session, String rotationId) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultRotation task = this.task(vault, rotationId);
        if (VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus()) && !task.getExpiresAt().isAfter(vaultControlMapper.databaseNow())) {
            this.finish(vault, task, VaultEnum.RotationStatus.EXPIRED, null);
        }
        return this.view(vault, task);
    }

    private VaultRotationVO summary(VaultInfo vault, VaultRotation task) {
        VaultGeneration generation = vaultCipherStore.generation(vault, task.getTargetGeneration());
        VaultRotationVO result = BeanConvertUtils.convert(task, VaultRotationVO.class);
        result.setKeyEpoch(generation.getKeyEpoch());
        result.setExpiresAt(task.getExpiresAt().toString());
        return result;
    }

    private VaultRotationStatusVO view(VaultInfo vault, VaultRotation task) {
        VaultRotationStatusVO result = BeanConvertUtils.convert(task, VaultRotationStatusVO.class);
        result.setExpiresAt(task.getExpiresAt().toString());
        List<VaultRotationExpectedItem> expected = this.expected(task);
        List<VaultRotationStatusVO.ExpectedItemVO> expectedViews = BeanConvertUtils.mapList(expected, VaultRotationStatusVO.ExpectedItemVO.class);
        result.setExpectedItems(expectedViews);
        List<VaultKeySlot> newSlots = vaultCipherStore.slots(vault, task.getTargetGeneration());
        List<VaultProtocol.Slot> keySlots = newSlots.stream().map(vaultCipherStore::view).toList();
        result.setKeySlots(keySlots);
        VaultGeneration generation = vaultCipherStore.generation(vault, task.getTargetGeneration());
        result.setKeyEpoch(generation.getKeyEpoch());
        LambdaQueryWrapper<VaultRotationBatch> query = Wrappers.lambdaQuery();
        query.eq(VaultRotationBatch::getRotationId, task.getId());
        List<VaultRotationBatch> received = vaultRotationBatchMapper.selectList(query);
        List<String> receivedBatchIds = received.stream().map(batch -> batch.getBatchId().toString()).toList();
        result.setReceivedBatchIds(receivedBatchIds);
        if (task.getCommitReceipt() != null) {
            VaultRotationReceiptVO receipt = VaultCodec.receipt(task.getCommitReceipt(), VaultRotationReceiptVO.class);
            result.setCommitReceipt(receipt);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationBatchVO batch(VaultSession session, VaultProtocol.RotationBatch request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultRotation task = this.task(vault, request.rotationId());
        this.usable(session, task, true);
        UUID batchId = VaultCodec.uuid(request.batchId());
        byte[] digest = VaultCodec.digest(request);
        LambdaQueryWrapper<VaultRotationBatch> receiptQuery = Wrappers.lambdaQuery();
        receiptQuery.eq(VaultRotationBatch::getRotationId, task.getId());
        receiptQuery.eq(VaultRotationBatch::getBatchId, batchId);
        VaultRotationBatch previous = vaultRotationBatchMapper.selectOne(receiptQuery);
        if (previous != null) {
            VaultEnum.Error.IDEMPOTENCY_CONFLICT.rejectIf(!MessageDigest.isEqual(previous.getRequestDigest(), digest));
            VaultRotationBatchVO result = new VaultRotationBatchVO();
            result.setBatchId(batchId.toString());
            result.setAccepted(previous.getItemCount());
            return result;
        }
        List<VaultRotationExpectedItem> expected = this.expected(task);
        Map<UUID, Long> versions = new HashMap<>();
        for (VaultRotationExpectedItem item : expected) {
            versions.put(item.getItemId(), item.getTargetContentVersion());
        }
        VaultGeneration target = vaultCipherStore.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        for (VaultProtocol.Item value : request.items()) {
            UUID itemId = VaultCodec.uuid(value.itemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(versions.get(itemId), value.contentVersion())
                    || !target.getKeyEpoch().equals(value.keyEpoch()));
            VaultItem item = vaultCipherStore.newItem(vault, target.getGeneration(), value, session.ownerId());
            if (VaultEnum.RotationMode.ROTATION.getCode().equals(task.getMode())) {
                VaultItem source = vaultItemMapper.lockItem(vault.getId(), task.getSourceGeneration(), itemId);
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(source == null);
                // 删除状态来自冻结源行，客户端无法改写或延长期限。
                item.setIsDeleted(source.getIsDeleted());
                item.setGmtDeleted(source.getGmtDeleted());
                item.setDeleteUser(source.getDeleteUser());
            }
            vaultItemMapper.insert(item);
        }
        VaultRotationBatch receipt = new VaultRotationBatch();
        receipt.setId(IdWorker.getId());
        receipt.setRotationId(task.getId());
        receipt.setBatchId(batchId);
        receipt.setRequestDigest(digest);
        receipt.setItemCount(request.items().size());
        vaultAccess.create(receipt, session.ownerId());
        vaultRotationBatchMapper.insert(receipt);
        VaultRotationBatchVO result = new VaultRotationBatchVO();
        result.setBatchId(batchId.toString());
        result.setAccepted(request.items().size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemPageVO<VaultItemVO> items(VaultSession session, String rotationId, VaultEnum.ItemScope scope, String cursor, int pageSize) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultRotation task = this.task(vault, rotationId);
        this.usable(session, task, true);
        Long generation = scope == VaultEnum.ItemScope.SOURCE ? task.getSourceGeneration() : task.getTargetGeneration();
        VaultEnum.Error.NOT_FOUND.rejectIf(generation == null);
        String prefix = session.ownerId() + ":" + vault.getVaultId() + ":" + task.getRotationId() + ":"
                + generation + ":" + scope.getCode() + ":";
        UUID last = null;
        if (cursor != null) {
            byte[] decoded = VaultCodec.decode(cursor, 1, 512);
            String value = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!value.startsWith(prefix));
            last = VaultCodec.uuid(value.substring(prefix.length()));
        }
        List<VaultItem> found = vaultItemMapper.frozenItems(
                vault.getId(),
                generation,
                task.getId(),
                last,
                pageSize + 1,
                false
        );
        boolean more = found.size() > pageSize;
        List<VaultItem> page = more ? found.subList(0, pageSize) : found;
        VaultItemPageVO<VaultItemVO> result = new VaultItemPageVO<>();
        result.setRotationId(rotationId);
        result.setGeneration(generation);
        result.setScope(scope.getCode());
        OffsetDateTime serverTime = vaultControlMapper.databaseNow();
        result.setServerTime(serverTime);
        List<VaultItemVO> pageItems = page.stream().map(vaultCipherStore::lifecycleView).toList();
        result.setItems(pageItems);
        String next = more ? VaultCodec.encode((prefix + page.getLast().getItemId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)) : null;
        result.setNextCursor(next);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationReceiptVO commit(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.ROTATION_COMMIT,
                request.idempotencyKey(),
                request,
                VaultRotationReceiptVO.class,
                () -> this.commitTask(session, account, request)
        );
    }

    private VaultRotationReceiptVO commitTask(VaultSession session, VaultAccount account, VaultProtocol.RotationCommand request) {
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultRotation task = this.task(vault, request.rotationId());
        if (VaultEnum.RotationStatus.COMMITTED.getCode().equals(task.getStatus())) {
            return VaultCodec.receipt(task.getCommitReceipt(), VaultRotationReceiptVO.class);
        }
        this.usable(session, task, true);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), task.getSourceGeneration())
                || !vault.getMetaRevision().equals(task.getExpectedMetaRevision())
                || !vault.getCollectionRevision().equals(task.getExpectedCollectionRevision())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        List<VaultRotationExpectedItem> expected = this.expected(task);
        List<VaultItem> staged = vaultItemMapper.generationMetadata(vault.getId(), task.getTargetGeneration());
        Map<UUID, VaultItem> actual = new HashMap<>();
        for (VaultItem item : staged) {
            actual.put(item.getItemId(), item);
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(expected.size() != task.getExpectedItemCount() || actual.size() != expected.size());
        Map<UUID, VaultItem> source = new HashMap<>();
        boolean restore = VaultEnum.RotationMode.RESTORE.getCode().equals(task.getMode());
        if (task.getSourceGeneration() != null) {
            if (restore) {
                OffsetDateTime sourceNow = vaultControlMapper.databaseNow();
                OffsetDateTime sourceBefore = sourceNow.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
                long sourceCount = vaultItemMapper.retainedCount(vault.getId(), task.getSourceGeneration(), sourceBefore);
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(sourceCount != 0);
            } else {
                List<VaultItem> old = vaultItemMapper.frozenItems(
                        vault.getId(),
                        task.getSourceGeneration(),
                        task.getId(),
                        null,
                        null,
                        true
                );
                for (VaultItem item : old) {
                    source.put(item.getItemId(), item);
                }
            }
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!restore && source.size() != expected.size());
        VaultGeneration target = vaultCipherStore.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        for (VaultRotationExpectedItem item : expected) {
            VaultItem value = actual.get(item.getItemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(value == null || !value.getContentVersion().equals(item.getTargetContentVersion())
                    || !value.getKeyEpoch().equals(target.getKeyEpoch()) || value.getRowVersion() != 1L);
            if (!restore) {
                VaultItem old = source.get(item.getItemId());
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(old == null || !old.getContentVersion().equals(item.getSourceContentVersion())
                        || !old.getRowVersion().equals(item.getSourceRowVersion())
                        || !Objects.equals(old.getIsDeleted(), value.getIsDeleted())
                        || !Objects.equals(old.getGmtDeleted(), value.getGmtDeleted())
                        || !Objects.equals(old.getDeleteUser(), value.getDeleteUser()));
            }
        }
        List<VaultKeySlot> newSlots = vaultCipherStore.slots(vault, target.getGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(newSlots.size() != 2);
        byte[] supplied = VaultCodec.decode(request.verifiedManifestDigest(), 32, 32);
        byte[] computed = vaultCipherStore.manifest(session, vault, task);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!MessageDigest.isEqual(supplied, computed));
        OffsetDateTime now = vaultControlMapper.databaseNow();
        if (task.getSourceGeneration() != null) {
            VaultGeneration old = vaultCipherStore.generation(vault, task.getSourceGeneration());
            old.setStatus(VaultEnum.GenerationStatus.RETIRED.getCode());
            old.setRetiredAt(now);
            vaultAccess.touch(old, session.ownerId());
            vaultGenerationMapper.updateById(old);
        }
        target.setStatus(VaultEnum.GenerationStatus.ACTIVE.getCode());
        vaultAccess.touch(target, session.ownerId());
        vaultGenerationMapper.updateById(target);
        vault.setActiveGeneration(target.getGeneration());
        vault.setStatus(VaultEnum.VaultStatus.ACTIVE.getCode());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getMetaRevision() >= VaultProtocol.MAX_VERSION || vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
        vault.setMetaRevision(vault.getMetaRevision() + 1);
        vault.setCollectionRevision(vault.getCollectionRevision() + 1);
        vaultAccess.touch(vault, session.ownerId());
        vaultInfoMapper.updateById(vault);
        VaultRotationReceiptVO receipt = new VaultRotationReceiptVO();
        receipt.setRotationId(task.getRotationId().toString());
        receipt.setGeneration(target.getGeneration());
        receipt.setKeyEpoch(target.getKeyEpoch());
        receipt.setMetaRevision(vault.getMetaRevision());
        receipt.setCollectionRevision(vault.getCollectionRevision());
        task.setStatus(VaultEnum.RotationStatus.COMMITTED.getCode());
        task.setFinishedAt(now);
        task.setVerifiedManifestDigest(computed);
        task.setCommitReceipt(VaultCodec.json(receipt));
        vaultAccess.touch(task, session.ownerId());
        vaultRotationMapper.updateById(task);
        // 摘要与完整冻结清单验证完毕后，才移除轮换期间到期的源/目标行。
        OffsetDateTime before = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        int purged = vaultItemMapper.purgeExpired(vault.getId(), before, null);
        if (purged > 0) {
            vaultAccess.systemAudit(vault.getAccountId(), vault.getId(), VaultEnum.AuditAction.ITEM_PURGE);
        }
        vaultAccess.revoke(account, session.ownerId());
        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_COMMIT);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationVO resume(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.ROTATION_RESUME,
                request.idempotencyKey(),
                request,
                VaultRotationVO.class,
                () -> {
                    VaultInfo vault = vaultAccess.lockVault(session);
                    VaultRotation task = this.task(vault, request.rotationId());
                    this.usable(session, task, false);
                    VaultEnum.Error.VERSION_CONFLICT.rejectIf(!task.getExpectedCollectionRevision().equals(request.expectedCollectionRevision()));
                    task.setSessionBindingId(session.bindingId());
                    vaultAccess.touch(task, session.ownerId());
                    vaultRotationMapper.updateById(task);
                    vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_RESUME);
                    return this.summary(vault, task);
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultRotationVO cancel(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.ROTATION_CANCEL,
                request.idempotencyKey(),
                request,
                VaultRotationVO.class,
                () -> {
                    VaultInfo vault = vaultAccess.lockVault(session);
                    VaultRotation task = this.task(vault, request.rotationId());
                    VaultEnum.Error.VERSION_CONFLICT.rejectIf(VaultEnum.RotationStatus.COMMITTED.getCode().equals(task.getStatus()));
                    if (VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())) {
                        this.finish(vault, task, VaultEnum.RotationStatus.CANCELLED, session.ownerId());
                        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_CANCEL);
                    }
                    VaultRotationVO result = new VaultRotationVO();
                    result.setRotationId(task.getRotationId().toString());
                    result.setStatus(task.getStatus());
                    return result;
                }
        );
    }

    private void finish(VaultInfo vault, VaultRotation task, VaultEnum.RotationStatus status, Long actor) {
        if (!VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())) {
            return;
        }
        OffsetDateTime now = vaultControlMapper.databaseNow();
        task.setStatus(status.getCode());
        task.setFinishedAt(now);
        vaultAccess.touch(task, actor);
        vaultRotationMapper.updateById(task);
        VaultGeneration target = vaultCipherStore.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        target.setStatus(VaultEnum.GenerationStatus.ABORTED.getCode());
        target.setRetiredAt(now);
        vaultAccess.touch(target, actor);
        vaultGenerationMapper.updateById(target);
        if (VaultEnum.RotationMode.RESTORE.getCode().equals(task.getMode())) {
            vault.setStatus(task.getSourceGeneration() == null ? VaultEnum.VaultStatus.EMPTY.getCode() : VaultEnum.VaultStatus.ACTIVE.getCode());
            vaultAccess.touch(vault, actor);
            vaultInfoMapper.updateById(vault);
        }
        vaultControlMapper.purgeGenerationItems(vault.getId(), task.getTargetGeneration());
        vaultControlMapper.purgeGenerationSlots(vault.getId(), task.getTargetGeneration());
        OffsetDateTime before = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        int purged = vaultItemMapper.purgeExpired(vault.getId(), before, null);
        if (purged > 0) {
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
            vault.setCollectionRevision(vault.getCollectionRevision() + 1);
            vaultAccess.touch(vault, actor);
            vaultInfoMapper.updateById(vault);
            vaultAccess.systemAudit(vault.getAccountId(), vault.getId(), VaultEnum.AuditAction.ITEM_PURGE);
        }
        if (status == VaultEnum.RotationStatus.EXPIRED) {
            vaultAccess.systemAudit(vault.getAccountId(), vault.getId(), VaultEnum.AuditAction.ROTATION_EXPIRE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expire(Long ownerId, String rotationId) {
        VaultAccount account = vaultControlMapper.lockAccount(ownerId);
        if (account == null) {
            return;
        }
        VaultInfo vault = vaultControlMapper.lockVault(account.getId());
        if (vault == null) {
            return;
        }
        VaultRotation task = this.task(vault, rotationId);
        if (!task.getExpiresAt().isAfter(vaultControlMapper.databaseNow())) {
            this.finish(vault, task, VaultEnum.RotationStatus.EXPIRED, null);
        }
    }
}
