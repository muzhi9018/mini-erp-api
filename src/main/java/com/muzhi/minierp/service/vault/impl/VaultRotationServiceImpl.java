package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.VaultContexts;
import com.muzhi.minierp.service.vault.*;
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
    private final VaultAccess access;

    private final VaultIdempotency idempotency;

    private final VaultCipherStore cipher;

    private final VaultControlMapper control;

    private final VaultInfoMapper vaults;

    private final VaultGenerationMapper generations;

    private final VaultRotationMapper rotations;

    private final VaultRotationExpectedItemMapper expectedItems;

    private final VaultRotationBatchMapper batches;

    private final VaultItemMapper itemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> start(VaultSession session, VaultProtocol.RotationStart request, VaultEnum.RotationMode mode) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultEnum.Operation operation = mode == VaultEnum.RotationMode.ROTATION ? VaultEnum.Operation.ROTATION_START : VaultEnum.Operation.RESTORE_START;
        return idempotency.execute(session, operation, request.idempotencyKey(), request,
                () -> this.startTask(session, request, mode));
    }

    private Map<String, Object> startTask(VaultSession session, VaultProtocol.RotationStart request, VaultEnum.RotationMode mode) {
        VaultInfo vault = control.lockVault(session.accountId());
        if (vault == null) {
            VaultEnum.Error.NOT_FOUND.rejectIf(mode != VaultEnum.RotationMode.RESTORE);
            vault = new VaultInfo();
            vault.setId(IdWorker.getId());
            vault.setVaultId(VaultCodec.uuid(request.vaultId()));
            vault.setAccountId(session.accountId());
            vault.setStatus(VaultEnum.VaultStatus.EMPTY.getCode());
            vault.setMetaRevision(1L);
            vault.setCollectionRevision(1L);
            access.create(vault, session.ownerId());
            vaults.insert(vault);
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getMetaRevision().equals(request.expectedMetaRevision())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        LambdaQueryWrapper<VaultRotation> open = Wrappers.lambdaQuery();
        open.eq(VaultRotation::getVaultId, vault.getId());
        open.eq(VaultRotation::getStatus, VaultEnum.RotationStatus.UPLOADING.getCode());
        VaultRotation existing = rotations.selectOne(open);
        if (existing != null && !existing.getExpiresAt().isAfter(control.databaseNow())) {
            existing = control.lockRotation(vault.getId(), existing.getRotationId());
            this.finish(vault, existing, VaultEnum.RotationStatus.EXPIRED, null);
            existing = null;
        }
        VaultEnum.Error.WRITE_LOCKED.rejectIf(existing != null);
        List<VaultItem> source = vault.getActiveGeneration() == null ? List.of() : cipher.items(vault, vault.getActiveGeneration());
        if (mode == VaultEnum.RotationMode.ROTATION) {
            VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        } else {
            VaultEnum.Error.ALREADY_INITIALIZED.rejectIf(!source.isEmpty());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getVaultId().equals(VaultCodec.uuid(request.vaultId())));
        }
        VaultGeneration sourceGeneration = vault.getActiveGeneration() == null ? null : cipher.generation(vault, vault.getActiveGeneration());
        long epoch = sourceGeneration == null ? 1 : sourceGeneration.getKeyEpoch() + 1;
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(epoch > VaultProtocol.MAX_VERSION);
        long target = cipher.nextGeneration(vault);
        cipher.addGeneration(vault, target, epoch, VaultEnum.GenerationStatus.STAGING, session.ownerId());
        cipher.addSlots(vault, target, request.keySlots(), session.ownerId());
        OffsetDateTime now = control.databaseNow();
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
        task.setExpectedItemCount(mode == VaultEnum.RotationMode.ROTATION ? source.size() : request.itemIds().size());
        task.setGmtCreate(now);
        task.setExpiresAt(now.plusMinutes(30));
        access.create(task, session.ownerId());
        rotations.insert(task);
        if (mode == VaultEnum.RotationMode.ROTATION) {
            for (VaultItem item : source) {
                this.expected(task, item.getItemId(), item.getContentVersion(), item.getRowVersion(), item.getContentVersion(), session.ownerId());
            }
        } else {
            for (String itemId : request.itemIds()) {
                this.expected(task, VaultCodec.uuid(itemId), null, null, 1L, session.ownerId());
            }
            vault.setStatus(VaultEnum.VaultStatus.RESTORING.getCode());
            access.touch(vault, session.ownerId());
            vaults.updateById(vault);
        }
        access.audit(session, vault.getId(), mode == VaultEnum.RotationMode.ROTATION ? VaultEnum.AuditAction.ROTATION_START : VaultEnum.AuditAction.RESTORE_START);
        return this.summary(vault, task);
    }

    private void expected(VaultRotation task, UUID itemId, Long sourceContent, Long sourceRow, Long targetContent, Long actor) {
        VaultRotationExpectedItem item = new VaultRotationExpectedItem();
        item.setRotationId(task.getId());
        item.setItemId(itemId);
        item.setSourceContentVersion(sourceContent);
        item.setSourceRowVersion(sourceRow);
        item.setTargetContentVersion(targetContent);
        access.create(item, actor);
        expectedItems.insert(item);
    }

    private List<VaultRotationExpectedItem> expected(VaultRotation task) {
        LambdaQueryWrapper<VaultRotationExpectedItem> query = Wrappers.lambdaQuery();
        query.eq(VaultRotationExpectedItem::getRotationId, task.getId());
        query.orderByAsc(VaultRotationExpectedItem::getItemId);
        return expectedItems.selectList(query);
    }

    private VaultRotation task(VaultInfo vault, String id) {
        VaultRotation task = control.lockRotation(vault.getId(), VaultCodec.uuid(id));
        VaultEnum.Error.NOT_FOUND.rejectIf(task == null);
        return task;
    }

    private void usable(VaultSession session, VaultRotation task, boolean binding) {
        VaultEnum.Error.ROTATION_EXPIRED.rejectIf(!VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())
                || !task.getExpiresAt().isAfter(control.databaseNow()));
        VaultEnum.Error.SESSION_REVOKED.rejectIf(task.getStartedAuthEpoch() != session.authEpoch());
        VaultEnum.Error.MFA_REQUIRED.rejectIf(binding && !task.getSessionBindingId().equals(session.bindingId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> status(VaultSession session, String rotationId) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = access.lockVault(session);
        VaultRotation task = this.task(vault, rotationId);
        if (VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus()) && !task.getExpiresAt().isAfter(control.databaseNow())) {
            this.finish(vault, task, VaultEnum.RotationStatus.EXPIRED, null);
        }
        return this.view(vault, task);
    }

    private Map<String, Object> summary(VaultInfo vault, VaultRotation task) {
        VaultGeneration generation = cipher.generation(vault, task.getTargetGeneration());
        return Map.of("rotationId", task.getRotationId().toString(), "status", task.getStatus(),
                "generation", task.getTargetGeneration(), "keyEpoch", generation.getKeyEpoch(),
                "expectedCollectionRevision", task.getExpectedCollectionRevision(), "expectedItemCount", task.getExpectedItemCount(),
                "expiresAt", task.getExpiresAt().toString());
    }

    private Map<String, Object> view(VaultInfo vault, VaultRotation task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rotationId", task.getRotationId().toString());
        result.put("mode", task.getMode());
        result.put("status", task.getStatus());
        result.put("generation", task.getTargetGeneration());
        result.put("expiresAt", task.getExpiresAt());
        result.put("expectedCollectionRevision", task.getExpectedCollectionRevision());
        List<VaultRotationExpectedItem> expected = this.expected(task);
        result.put("expectedItems", expected.stream().map(item -> Map.of("itemId", item.getItemId().toString(),
                "contentVersion", item.getTargetContentVersion())).toList());
        List<VaultKeySlot> newSlots = cipher.slots(vault, task.getTargetGeneration());
        result.put("keySlots", newSlots.stream().map(cipher::view).toList());
        VaultGeneration generation = cipher.generation(vault, task.getTargetGeneration());
        result.put("keyEpoch", generation.getKeyEpoch());
        LambdaQueryWrapper<VaultRotationBatch> query = Wrappers.lambdaQuery();
        query.eq(VaultRotationBatch::getRotationId, task.getId());
        List<VaultRotationBatch> received = batches.selectList(query);
        result.put("receivedBatchIds", received.stream().map(batch -> batch.getBatchId().toString()).toList());
        result.put("commitReceipt", task.getCommitReceipt() == null ? null : VaultCodec.receipt(task.getCommitReceipt()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batch(VaultSession session, VaultProtocol.RotationBatch request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = access.lockVault(session);
        VaultRotation task = this.task(vault, request.rotationId());
        this.usable(session, task, true);
        UUID batchId = VaultCodec.uuid(request.batchId());
        byte[] digest = VaultCodec.digest(request);
        LambdaQueryWrapper<VaultRotationBatch> receiptQuery = Wrappers.lambdaQuery();
        receiptQuery.eq(VaultRotationBatch::getRotationId, task.getId());
        receiptQuery.eq(VaultRotationBatch::getBatchId, batchId);
        VaultRotationBatch previous = batches.selectOne(receiptQuery);
        if (previous != null) {
            VaultEnum.Error.IDEMPOTENCY_CONFLICT.rejectIf(!MessageDigest.isEqual(previous.getRequestDigest(), digest));
            return Map.of("batchId", batchId.toString(), "accepted", previous.getItemCount());
        }
        List<VaultRotationExpectedItem> expected = this.expected(task);
        Map<UUID, Long> versions = new HashMap<>();
        for (VaultRotationExpectedItem item : expected) {
            versions.put(item.getItemId(), item.getTargetContentVersion());
        }
        VaultGeneration target = cipher.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        for (VaultProtocol.Item value : request.items()) {
            UUID itemId = VaultCodec.uuid(value.itemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(versions.get(itemId), value.contentVersion())
                    || !target.getKeyEpoch().equals(value.keyEpoch()));
            VaultItem item = cipher.newItem(vault, target.getGeneration(), value, session.ownerId());
            itemMapper.insert(item);
        }
        VaultRotationBatch receipt = new VaultRotationBatch();
        receipt.setId(IdWorker.getId());
        receipt.setRotationId(task.getId());
        receipt.setBatchId(batchId);
        receipt.setRequestDigest(digest);
        receipt.setItemCount(request.items().size());
        access.create(receipt, session.ownerId());
        batches.insert(receipt);
        return Map.of("batchId", batchId.toString(), "accepted", request.items().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> items(VaultSession session, String rotationId, String cursor, int pageSize) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = access.lockVault(session);
        VaultRotation task = this.task(vault, rotationId);
        this.usable(session, task, true);
        String scope = session.ownerId() + ":" + task.getRotationId() + ":" + task.getTargetGeneration() + ":";
        UUID last = null;
        if (cursor != null) {
            byte[] decoded = VaultCodec.decode(cursor, 1, 256);
            String value = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!value.startsWith(scope));
            last = VaultCodec.uuid(value.substring(scope.length()));
        }
        LambdaQueryWrapper<VaultItem> query = Wrappers.lambdaQuery();
        query.eq(VaultItem::getVaultId, vault.getId());
        query.eq(VaultItem::getGeneration, task.getTargetGeneration());
        if (last != null) {
            query.gt(VaultItem::getItemId, last);
        }
        query.orderByAsc(VaultItem::getItemId);
        query.last("LIMIT " + (pageSize + 1));
        List<VaultItem> found = itemMapper.selectList(query);
        boolean more = found.size() > pageSize;
        List<VaultItem> page = more ? found.subList(0, pageSize) : found;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rotationId", rotationId);
        result.put("generation", task.getTargetGeneration());
        result.put("items", page.stream().map(cipher::view).toList());
        String next = more ? VaultCodec.encode((scope + page.getLast().getItemId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)) : null;
        result.put("nextCursor", next);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> commit(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(session, VaultEnum.Operation.ROTATION_COMMIT, request.idempotencyKey(), request,
                () -> this.commitTask(session, account, request));
    }

    private Map<String, Object> commitTask(VaultSession session, VaultAccount account, VaultProtocol.RotationCommand request) {
        VaultInfo vault = access.lockVault(session);
        VaultRotation task = this.task(vault, request.rotationId());
        if (VaultEnum.RotationStatus.COMMITTED.getCode().equals(task.getStatus())) {
            return VaultCodec.receipt(task.getCommitReceipt());
        }
        this.usable(session, task, true);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), task.getSourceGeneration())
                || !vault.getMetaRevision().equals(task.getExpectedMetaRevision())
                || !vault.getCollectionRevision().equals(task.getExpectedCollectionRevision())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        List<VaultRotationExpectedItem> expected = this.expected(task);
        List<VaultItem> staged = cipher.items(vault, task.getTargetGeneration());
        Map<UUID, VaultItem> actual = new HashMap<>();
        for (VaultItem item : staged) {
            actual.put(item.getItemId(), item);
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(expected.size() != task.getExpectedItemCount() || actual.size() != expected.size());
        Map<UUID, VaultItem> source = new HashMap<>();
        if (task.getSourceGeneration() != null) {
            List<VaultItem> old = cipher.items(vault, task.getSourceGeneration());
            for (VaultItem item : old) {
                source.put(item.getItemId(), item);
            }
        }
        boolean restore = VaultEnum.RotationMode.RESTORE.getCode().equals(task.getMode());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(restore ? !source.isEmpty() : source.size() != expected.size());
        VaultGeneration target = cipher.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        for (VaultRotationExpectedItem item : expected) {
            VaultItem value = actual.get(item.getItemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(value == null || !value.getContentVersion().equals(item.getTargetContentVersion())
                    || !value.getKeyEpoch().equals(target.getKeyEpoch()) || value.getRowVersion() != 1L);
            if (!restore) {
                VaultItem old = source.get(item.getItemId());
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(old == null || !old.getContentVersion().equals(item.getSourceContentVersion())
                        || !old.getRowVersion().equals(item.getSourceRowVersion()));
            }
        }
        List<VaultKeySlot> newSlots = cipher.slots(vault, target.getGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(newSlots.size() != 2);
        byte[] supplied = VaultCodec.decode(request.verifiedManifestDigest(), 32, 32);
        byte[] computed = cipher.manifest(session, vault, task);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!MessageDigest.isEqual(supplied, computed));
        OffsetDateTime now = control.databaseNow();
        if (task.getSourceGeneration() != null) {
            VaultGeneration old = cipher.generation(vault, task.getSourceGeneration());
            old.setStatus(VaultEnum.GenerationStatus.RETIRED.getCode());
            old.setRetiredAt(now);
            access.touch(old, session.ownerId());
            generations.updateById(old);
        }
        target.setStatus(VaultEnum.GenerationStatus.ACTIVE.getCode());
        access.touch(target, session.ownerId());
        generations.updateById(target);
        vault.setActiveGeneration(target.getGeneration());
        vault.setStatus(VaultEnum.VaultStatus.ACTIVE.getCode());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getMetaRevision() >= VaultProtocol.MAX_VERSION || vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
        vault.setMetaRevision(vault.getMetaRevision() + 1);
        vault.setCollectionRevision(vault.getCollectionRevision() + 1);
        access.touch(vault, session.ownerId());
        vaults.updateById(vault);
        Map<String, Object> receipt = Map.of("rotationId", task.getRotationId().toString(), "generation", target.getGeneration(),
                "keyEpoch", target.getKeyEpoch(), "metaRevision", vault.getMetaRevision(), "collectionRevision", vault.getCollectionRevision());
        task.setStatus(VaultEnum.RotationStatus.COMMITTED.getCode());
        task.setFinishedAt(now);
        task.setVerifiedManifestDigest(computed);
        task.setCommitReceipt(VaultCodec.json(receipt));
        access.touch(task, session.ownerId());
        rotations.updateById(task);
        access.revoke(account, session.ownerId());
        access.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_COMMIT);
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resume(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(session, VaultEnum.Operation.ROTATION_RESUME, request.idempotencyKey(), request, () -> {
            VaultInfo vault = access.lockVault(session);
            VaultRotation task = this.task(vault, request.rotationId());
            this.usable(session, task, false);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!task.getExpectedCollectionRevision().equals(request.expectedCollectionRevision()));
            task.setSessionBindingId(session.bindingId());
            access.touch(task, session.ownerId());
            rotations.updateById(task);
            access.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_RESUME);
            return this.summary(vault, task);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(VaultSession session, VaultProtocol.RotationCommand request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(session, VaultEnum.Operation.ROTATION_CANCEL, request.idempotencyKey(), request, () -> {
            VaultInfo vault = access.lockVault(session);
            VaultRotation task = this.task(vault, request.rotationId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(VaultEnum.RotationStatus.COMMITTED.getCode().equals(task.getStatus()));
            if (VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())) {
                this.finish(vault, task, VaultEnum.RotationStatus.CANCELLED, session.ownerId());
                access.audit(session, vault.getId(), VaultEnum.AuditAction.ROTATION_CANCEL);
            }
            return Map.of("rotationId", task.getRotationId().toString(), "status", task.getStatus());
        });
    }

    private void finish(VaultInfo vault, VaultRotation task, VaultEnum.RotationStatus status, Long actor) {
        if (!VaultEnum.RotationStatus.UPLOADING.getCode().equals(task.getStatus())) {
            return;
        }
        OffsetDateTime now = control.databaseNow();
        task.setStatus(status.getCode());
        task.setFinishedAt(now);
        access.touch(task, actor);
        rotations.updateById(task);
        VaultGeneration target = cipher.generation(vault, task.getTargetGeneration());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.STAGING.getCode().equals(target.getStatus()));
        target.setStatus(VaultEnum.GenerationStatus.ABORTED.getCode());
        target.setRetiredAt(now);
        access.touch(target, actor);
        generations.updateById(target);
        if (VaultEnum.RotationMode.RESTORE.getCode().equals(task.getMode())) {
            vault.setStatus(task.getSourceGeneration() == null ? VaultEnum.VaultStatus.EMPTY.getCode() : VaultEnum.VaultStatus.ACTIVE.getCode());
            access.touch(vault, actor);
            vaults.updateById(vault);
        }
        control.purgeGenerationItems(vault.getId(), task.getTargetGeneration());
        control.purgeGenerationSlots(vault.getId(), task.getTargetGeneration());
        if (status == VaultEnum.RotationStatus.EXPIRED) {
            access.systemAudit(vault.getAccountId(), vault.getId(), VaultEnum.AuditAction.ROTATION_EXPIRE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expire(Long ownerId, String rotationId) {
        VaultAccount account = control.lockAccount(ownerId);
        if (account == null) {
            return;
        }
        VaultInfo vault = control.lockVault(account.getId());
        if (vault == null) {
            return;
        }
        VaultRotation task = this.task(vault, rotationId);
        if (!task.getExpiresAt().isAfter(control.databaseNow())) {
            this.finish(vault, task, VaultEnum.RotationStatus.EXPIRED, null);
        }
    }
}
