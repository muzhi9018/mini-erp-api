package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.muzhi.minierp.component.vault.*;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.mapper.vault.*;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.security.vault.VaultContexts;
import com.muzhi.minierp.service.vault.*;
import com.muzhi.minierp.vo.vault.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * <p>
 * {@code VaultDataServiceImpl}：密文数据的归属、并发和逻辑删除事务
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultDataServiceImpl implements IVaultDataService {

    private final VaultAccess vaultAccess;

    private final VaultIdempotency vaultIdempotency;

    private final VaultCipherStore vaultCipherStore;

    private final VaultControlMapper vaultControlMapper;

    private final VaultInfoMapper vaultInfoMapper;

    private final VaultItemMapper vaultItemMapper;

    private final VaultKeySlotMapper vaultKeySlotMapper;

    private final VaultRotationMapper vaultRotationMapper;

    private final VaultSnapshotStore vaultSnapshotStore;

    private final VaultOperationAuthorization vaultOperationAuthorization;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultOperationOptionsVO deleteItemOptions(VaultSession session, VaultProtocol.ItemWrite request) {
        VaultAccount account = this.lockOperationAccount(session);
        VaultInfo vault = this.lockOperationVault(session);
        this.validateOperationTarget(vault, request.generation(), request.itemId(), request.expectedRowVersion(), false);
        return vaultOperationAuthorization.options(session, account, vault, VaultEnum.Operation.DELETE_ITEM, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultOperationOptionsVO purgeItemOptions(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultAccount account = this.lockOperationAccount(session);
        VaultInfo vault = this.lockOperationVault(session);
        this.validateOperationTarget(vault, request.generation(), request.itemId(), request.expectedRowVersion(), true);
        return vaultOperationAuthorization.options(session, account, vault, VaultEnum.Operation.PURGE_ITEM, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultOperationOptionsVO deleteGroupOptions(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultAccount account = this.lockOperationAccount(session);
        VaultInfo vault = this.lockOperationVault(session);
        this.validateOperationTarget(vault, request.generation(), request.itemId(), request.expectedRowVersion(), false);
        return vaultOperationAuthorization.options(session, account, vault, VaultEnum.Operation.DELETE_GROUP, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultOperationOptionsVO batchItemsOptions(VaultSession session, VaultProtocol.ItemBatch request) {
        VaultAccount account = this.lockOperationAccount(session);
        VaultInfo vault = this.lockOperationVault(session);
        VaultEnum.Error.INVALID_REQUEST.rejectIf(!VaultProtocolValidator.hasDeletion(request));
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        VaultGeneration generation = vaultCipherStore.generation(vault, request.generation());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!generation.getKeyEpoch().equals(request.keyEpoch()));
        for (VaultProtocol.ItemChange change : request.changes()) {
            VaultEnum.ItemChangeAction action = VaultEnum.ofCode(VaultEnum.ItemChangeAction.class, change.action());
            if (action == VaultEnum.ItemChangeAction.CREATE) {
                UUID itemId = VaultCodec.uuid(change.itemId());
                boolean occupied = vaultControlMapper.itemIdExists(vault.getId(), request.generation(), itemId);
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(occupied);
            } else {
                this.validateOperationTarget(vault, request.generation(), change.itemId(), change.expectedRowVersion(), false);
            }
        }
        return vaultOperationAuthorization.options(session, account, vault, VaultEnum.Operation.BATCH_ITEMS, request);
    }

    /**
     * 操作挑战与实际删除保持相同的账户、库持锁顺序。
     */
    private VaultAccount lockOperationAccount(VaultSession session) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return account;
    }

    private VaultInfo lockOperationVault(VaultSession session) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        return vault;
    }

    private void validateOperationTarget(VaultInfo vault, Long generation, String itemId, Long rowVersion, boolean deleted) {
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), generation));
        VaultItem item = this.lockItem(vault, itemId);
        if (deleted) {
            VaultEnum.Error.ITEM_NOT_DELETED.rejectIf(!Boolean.TRUE.equals(item.getIsDeleted()));
        } else {
            VaultEnum.Error.ITEM_ALREADY_DELETED.rejectIf(Boolean.TRUE.equals(item.getIsDeleted()));
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(rowVersion));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultMetaVO meta(VaultSession session) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultControlMapper.lockVault(account.getId());
        if (vault != null) {
            return vaultCipherStore.meta(vault);
        }
        VaultMetaVO result = new VaultMetaVO();
        result.setInitialized(false);
        result.setMetaRevision(1L);
        result.setCollectionRevision(1L);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultMetaVO initialize(VaultSession session, VaultProtocol.Setup setup) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.INITIALIZE,
                setup.idempotencyKey(),
                setup,
                VaultMetaVO.class,
                () -> this.createVault(session, setup)
        );
    }

    private VaultMetaVO createVault(VaultSession session, VaultProtocol.Setup setup) {
        VaultInfo vault = vaultControlMapper.lockVault(session.accountId());
        if (vault == null) {
            vault = new VaultInfo();
            vault.setId(IdWorker.getId());
            vault.setAccountId(session.accountId());
            vault.setVaultId(VaultCodec.uuid(setup.vaultId()));
            vault.setMetaRevision(1L);
            vault.setCollectionRevision(1L);
            vault.setStatus(VaultEnum.VaultStatus.EMPTY.getCode());
            vaultAccess.create(vault, session.ownerId());
            vaultInfoMapper.insert(vault);
        }
        VaultEnum.Error.ALREADY_INITIALIZED.rejectIf(vault.getActiveGeneration() != null
                || !VaultEnum.VaultStatus.EMPTY.getCode().equals(vault.getStatus()));
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getVaultId().equals(VaultCodec.uuid(setup.vaultId())));
        long generation = vaultCipherStore.nextGeneration(vault);
        vaultCipherStore.addGeneration(vault, generation, 1L, VaultEnum.GenerationStatus.ACTIVE, session.ownerId());
        vaultCipherStore.addSlots(vault, generation, setup.keySlots(), session.ownerId());
        vault.setActiveGeneration(generation);
        vault.setStatus(VaultEnum.VaultStatus.ACTIVE.getCode());
        vaultAccess.touch(vault, session.ownerId());
        vaultInfoMapper.updateById(vault);
        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.INITIALIZE);
        return vaultCipherStore.meta(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemPageVO<VaultProtocol.Item> items(VaultSession session, String cursor, int pageSize) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        UUID last = null;
        if (cursor != null) {
            byte[] decoded = VaultCodec.decode(cursor, 1, 512);
            String[] parts = new String(decoded, StandardCharsets.UTF_8).split(":", -1);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(parts.length != 6);
            boolean stale = !parts[0].equals(session.ownerId().toString()) || !parts[1].equals(vault.getVaultId().toString())
                    || !parts[2].equals(vault.getActiveGeneration().toString()) || !parts[3].equals(vault.getCollectionRevision().toString()) || !"ACTIVE".equals(parts[4]);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(stale);
            last = VaultCodec.uuid(parts[5]);
        }
        LambdaQueryWrapper<VaultItem> query = Wrappers.lambdaQuery();
        query.eq(VaultItem::getVaultId, vault.getId());
        query.eq(VaultItem::getGeneration, vault.getActiveGeneration());
        if (last != null) {
            query.gt(VaultItem::getItemId, last);
        }
        query.orderByAsc(VaultItem::getItemId);
        query.last("LIMIT " + (pageSize + 1));
        List<VaultItem> found = vaultItemMapper.selectList(query);
        boolean more = found.size() > pageSize;
        List<VaultItem> page = more ? found.subList(0, pageSize) : found;
        VaultItemPageVO<VaultProtocol.Item> result = new VaultItemPageVO<>();
        result.setGeneration(vault.getActiveGeneration());
        OffsetDateTime serverTime = vaultControlMapper.databaseNow();
        result.setServerTime(serverTime);
        result.setCollectionRevision(vault.getCollectionRevision());
        List<VaultProtocol.Item> pageItems = page.stream().map(vaultCipherStore::view).toList();
        result.setItems(pageItems);
        String next = null;
        if (more) {
            String value = session.ownerId() + ":" + vault.getVaultId() + ":" + vault.getActiveGeneration() + ":"
                    + vault.getCollectionRevision() + ":ACTIVE:" + page.getLast().getItemId();
            next = VaultCodec.encode(value.getBytes(StandardCharsets.UTF_8));
        }
        result.setNextCursor(next);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO save(VaultSession session, VaultProtocol.ItemWrite request, boolean create) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultEnum.Operation operation = create ? VaultEnum.Operation.CREATE_ITEM : VaultEnum.Operation.UPDATE_ITEM;
        return vaultIdempotency.execute(
                session,
                operation,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> this.saveItem(session, request, create)
        );
    }

    /**
     * 检查库状态及进行中的轮换，写入检查沿用当前业务事务持有的库行锁。
     */
    private void validateWritable(VaultInfo vault) {
        LambdaQueryWrapper<VaultRotation> query = Wrappers.lambdaQuery();
        query.eq(VaultRotation::getVaultId, vault.getId());
        query.eq(VaultRotation::getStatus, VaultEnum.RotationStatus.UPLOADING.getCode());
        boolean locked = vaultRotationMapper.exists(query);
        VaultEnum.Error.WRITE_LOCKED.rejectIf(locked || !VaultEnum.VaultStatus.ACTIVE.getCode().equals(vault.getStatus()));
    }

    private VaultItemResultVO saveItem(VaultSession session, VaultProtocol.ItemWrite request, boolean create) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultGeneration generation = vaultCipherStore.generation(vault, request.generation());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!generation.getKeyEpoch().equals(request.keyEpoch()));
        VaultItem item;
        if (create) {
            LambdaQueryWrapper<VaultItem> countQuery = Wrappers.lambdaQuery();
            countQuery.eq(VaultItem::getVaultId, vault.getId());
            countQuery.eq(VaultItem::getGeneration, request.generation());
            Long currentCount = vaultItemMapper.selectCount(countQuery);
            int maximumItems = 2000;
            VaultEnum.Error.ITEM_LIMIT_REACHED.rejectIf(currentCount >= maximumItems);
            VaultProtocol.Item value = new VaultProtocol.Item(request.itemId(), 1L, 1L, request.keyEpoch(), request.wrappedDek(), request.encryptedItem());
            item = vaultCipherStore.newItem(vault, request.generation(), value, session.ownerId());
            vaultItemMapper.insert(item);
        } else {
            item = this.item(vault, request.itemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(request.expectedRowVersion())
                    || request.contentVersion() != item.getContentVersion() + 1);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(item.getRowVersion() >= VaultProtocol.MAX_VERSION);
            item.setContentVersion(request.contentVersion());
            item.setRowVersion(item.getRowVersion() + 1);
            vaultCipherStore.content(item, request.wrappedDek(), request.encryptedItem());
            vaultAccess.touch(item, session.ownerId());
            vaultItemMapper.updateById(item);
        }
        this.bump(vault, session.ownerId());
        vaultAccess.audit(session, vault.getId(), create ? VaultEnum.AuditAction.ITEM_CREATE : VaultEnum.AuditAction.ITEM_UPDATE);
        VaultItemResultVO result = new VaultItemResultVO();
        result.setItemId(item.getItemId().toString());
        result.setRowVersion(item.getRowVersion());
        result.setContentVersion(item.getContentVersion());
        result.setCollectionRevision(vault.getCollectionRevision());
        return result;
    }

    private VaultItem item(VaultInfo vault, String itemId) {
        LambdaQueryWrapper<VaultItem> query = Wrappers.lambdaQuery();
        query.eq(VaultItem::getVaultId, vault.getId());
        query.eq(VaultItem::getGeneration, vault.getActiveGeneration());
        query.eq(VaultItem::getItemId, VaultCodec.uuid(itemId));
        VaultItem item = vaultItemMapper.selectOne(query);
        VaultEnum.Error.NOT_FOUND.rejectIf(item == null);
        return item;
    }

    private void bump(VaultInfo vault, Long actor) {
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
        vault.setCollectionRevision(vault.getCollectionRevision() + 1);
        vaultAccess.touch(vault, actor);
        vaultInfoMapper.updateById(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO delete(VaultSession session, VaultProtocol.ItemWrite request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.DELETE_ITEM,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> {
                    VaultInfo vault = vaultAccess.lockVault(session);
                    vaultOperationAuthorization.verify(session, account, vault, VaultEnum.Operation.DELETE_ITEM,
                            request, request.passkeyAuthorization());
                    return this.deleteItem(session, request);
                }
        );
    }

    private VaultItemResultVO deleteItem(VaultSession session, VaultProtocol.ItemWrite request) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultItem item = this.lockItem(vault, request.itemId());
        VaultEnum.Error.ITEM_ALREADY_DELETED.rejectIf(Boolean.TRUE.equals(item.getIsDeleted()));
        this.validateRowVersion(item, request.expectedRowVersion());
        OffsetDateTime now = vaultControlMapper.databaseNow();
        int changed = vaultItemMapper.markDeleted(item, session.ownerId(), now);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(changed != 1);
        this.bump(vault, session.ownerId());
        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.ITEM_DELETE);
        VaultItemResultVO result = this.itemReceipt(item, vault, now);
        result.setRowVersion(item.getRowVersion() + 1);
        result.setDeleted(true);
        result.setDeletedAt(now);
        OffsetDateTime purgeAt = now.plusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        result.setPurgeAt(purgeAt);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO deleteGroup(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.DELETE_GROUP,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> {
                    VaultInfo vault = vaultAccess.lockVault(session);
                    vaultOperationAuthorization.verify(session, account, vault, VaultEnum.Operation.DELETE_GROUP,
                            request, request.passkeyAuthorization());
                    return this.deleteGroupItem(session, request);
                }
        );
    }

    /**
     * 账号组不进入保留集合；类型在密文内，客户端必须按解密后的类型选择此操作。
     */
    private VaultItemResultVO deleteGroupItem(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultItem item = this.lockItem(vault, request.itemId());
        VaultEnum.Error.ITEM_ALREADY_DELETED.rejectIf(Boolean.TRUE.equals(item.getIsDeleted()));
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(request.expectedRowVersion()));
        OffsetDateTime now = vaultControlMapper.databaseNow();
        int changed = vaultItemMapper.deleteActiveItem(item);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(changed != 1);
        vaultItemMapper.purgeHistoricalCopies(vault.getId(), item.getItemId());
        this.bump(vault, session.ownerId());
        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.ITEM_DELETE);
        VaultItemResultVO result = new VaultItemResultVO();
        result.setItemId(request.itemId());
        result.setPermanentlyDeleted(true);
        result.setCollectionRevision(vault.getCollectionRevision());
        result.setPurgedAt(now);
        result.setServerTime(now);
        return result;
    }

    private VaultItem lockItem(VaultInfo vault, String itemId) {
        UUID id = VaultCodec.uuid(itemId);
        VaultItem item = vaultItemMapper.lockItem(vault.getId(), vault.getActiveGeneration(), id);
        VaultEnum.Error.NOT_FOUND.rejectIf(item == null);
        return item;
    }

    private void validateRowVersion(VaultItem item, Long expectedRowVersion) {
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(expectedRowVersion)
                || item.getRowVersion() >= VaultProtocol.MAX_VERSION);
    }

    private VaultItemResultVO itemReceipt(VaultItem item, VaultInfo vault, OffsetDateTime now) {
        VaultItemResultVO result = new VaultItemResultVO();
        result.setItemId(item.getItemId().toString());
        result.setRowVersion(item.getRowVersion());
        result.setContentVersion(item.getContentVersion());
        result.setCollectionRevision(vault.getCollectionRevision());
        result.setServerTime(now);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO restoreItem(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.RESTORE_ITEM,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> this.changeDeletedItem(session, request, false)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO purgeItem(VaultSession session, VaultProtocol.ItemLifecycle request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.PURGE_ITEM,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> {
                    VaultInfo vault = vaultAccess.lockVault(session);
                    vaultOperationAuthorization.verify(session, account, vault, VaultEnum.Operation.PURGE_ITEM,
                            request, request.passkeyAuthorization());
                    return this.changeDeletedItem(session, request, true);
                }
        );
    }

    /**
     * 恢复和永久删除共用持锁顺序；幂等成功重放在读取当前状态之前返回。
     */
    private VaultItemResultVO changeDeletedItem(VaultSession session, VaultProtocol.ItemLifecycle request, boolean purge) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultItem item = this.lockItem(vault, request.itemId());
        VaultEnum.Error.ITEM_NOT_DELETED.rejectIf(!Boolean.TRUE.equals(item.getIsDeleted()));
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(request.expectedRowVersion()));
        OffsetDateTime now = vaultControlMapper.databaseNow();
        OffsetDateTime before = now.minusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        int changed;
        if (purge) {
            changed = vaultItemMapper.purgeItem(item);
            vaultItemMapper.purgeHistoricalCopies(vault.getId(), item.getItemId());
        } else {
            VaultEnum.Error.ITEM_EXPIRED.rejectIf(!item.getGmtDeleted().isAfter(before));
            this.validateRowVersion(item, request.expectedRowVersion());
            LambdaQueryWrapper<VaultItem> countQuery = Wrappers.lambdaQuery();
            countQuery.eq(VaultItem::getVaultId, vault.getId());
            countQuery.eq(VaultItem::getGeneration, vault.getActiveGeneration());
            Long currentCount = vaultItemMapper.selectCount(countQuery);
            int maximumItems = 2000;
            VaultEnum.Error.ITEM_LIMIT_REACHED.rejectIf(currentCount >= maximumItems);
            changed = vaultItemMapper.restoreItem(item, session.ownerId(), now, before);
        }
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(changed != 1);
        this.bump(vault, session.ownerId());
        VaultEnum.AuditAction action = purge ? VaultEnum.AuditAction.ITEM_PURGE : VaultEnum.AuditAction.ITEM_RESTORE;
        vaultAccess.audit(session, vault.getId(), action);
        VaultItemResultVO result = this.itemReceipt(item, vault, now);
        if (purge) {
            result.setRowVersion(null);
            result.setContentVersion(null);
            result.setPermanentlyDeleted(true);
            result.setPurgedAt(now);
        } else {
            result.setRowVersion(item.getRowVersion() + 1);
            result.setDeleted(false);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemVO deletedDetail(VaultSession session, String itemId) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultItem item = this.lockItem(vault, itemId);
        VaultEnum.Error.ITEM_NOT_DELETED.rejectIf(!Boolean.TRUE.equals(item.getIsDeleted()));
        OffsetDateTime now = vaultControlMapper.databaseNow();
        OffsetDateTime purgeAt = item.getGmtDeleted().plusHours(VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L);
        VaultEnum.Error.ITEM_EXPIRED.rejectIf(!now.isBefore(purgeAt));
        VaultItemVO result = vaultCipherStore.lifecycleView(item);
        result.setGeneration(vault.getActiveGeneration());
        result.setCollectionRevision(vault.getCollectionRevision());
        result.setServerTime(now);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemPageVO<VaultItemVO> deletedItems(VaultSession session, String cursor, int pageSize) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        OffsetDateTime now = vaultControlMapper.databaseNow();
        long retentionHours = VaultProtocol.DELETED_ITEM_RETENTION_DAYS * 24L;
        OffsetDateTime firstTime = now;
        OffsetDateTime before = now.minusHours(retentionHours);
        OffsetDateTime earliest = vaultItemMapper.earliestDeletedAt(vault.getId(), vault.getActiveGeneration(), before);
        OffsetDateTime boundary = earliest == null ? null : earliest.plusHours(retentionHours);
        OffsetDateTime lastDeleted = null;
        UUID lastId = null;
        String scope = "DELETED|" + session.ownerId() + "|" + vault.getVaultId() + "|" + vault.getActiveGeneration()
                + "|" + vault.getCollectionRevision() + "|";
        if (cursor != null) {
            byte[] decoded = VaultCodec.decode(cursor, 1, 512);
            String value = new String(decoded, StandardCharsets.UTF_8);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!value.startsWith(scope));
            String[] parts = value.substring(scope.length()).split("\\|", -1);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(parts.length != 4);
            try {
                firstTime = OffsetDateTime.parse(parts[0]);
                boundary = OffsetDateTime.parse(parts[1]);
                lastDeleted = OffsetDateTime.parse(parts[2]);
            } catch (DateTimeParseException failure) {
                throw VaultEnum.Error.INVALID_REQUEST.exception();
            }
            // 游标并非授权凭据，必须从数据库重新核实原集合的到期边界。
            OffsetDateTime originalBefore = firstTime.minusHours(retentionHours);
            OffsetDateTime originalEarliest = vaultItemMapper.earliestDeletedAt(vault.getId(), vault.getActiveGeneration(), originalBefore);
            OffsetDateTime expectedBoundary = originalEarliest == null ? null : originalEarliest.plusHours(retentionHours);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(firstTime.isAfter(now) || expectedBoundary == null
                    || !boundary.isEqual(expectedBoundary) || !now.isBefore(boundary));
            lastId = VaultCodec.uuid(parts[3]);
        }
        List<VaultItem> found = vaultItemMapper.deletedItems(
                vault.getId(),
                vault.getActiveGeneration(),
                before,
                firstTime,
                lastDeleted,
                lastId,
                pageSize + 1
        );
        boolean more = found.size() > pageSize;
        List<VaultItem> page = more ? found.subList(0, pageSize) : found;
        VaultItemPageVO<VaultItemVO> result = new VaultItemPageVO<>();
        result.setGeneration(vault.getActiveGeneration());
        result.setCollectionRevision(vault.getCollectionRevision());
        result.setServerTime(now);
        List<VaultItemVO> views = page.stream().map(vaultCipherStore::lifecycleView).toList();
        result.setItems(views);
        if (more) {
            VaultItem last = page.getLast();
            String next = scope + firstTime + "|" + boundary + "|" + last.getGmtDeleted() + "|" + last.getItemId();
            byte[] nextBytes = next.getBytes(StandardCharsets.UTF_8);
            String nextCursor = VaultCodec.encode(nextBytes);
            result.setNextCursor(nextCursor);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemBatchResultVO batch(VaultSession session, VaultProtocol.ItemBatch request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.BATCH_ITEMS,
                request.idempotencyKey(),
                request,
                VaultItemBatchResultVO.class,
                () -> {
                    if (VaultProtocolValidator.hasDeletion(request)) {
                        VaultInfo vault = vaultAccess.lockVault(session);
                        vaultOperationAuthorization.verify(session, account, vault, VaultEnum.Operation.BATCH_ITEMS,
                                request, request.passkeyAuthorization());
                    }
                    return this.applyBatch(session, request);
                }
        );
    }

    private VaultItemBatchResultVO applyBatch(VaultSession session, VaultProtocol.ItemBatch request) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation())
                || !vault.getCollectionRevision().equals(request.expectedCollectionRevision()));
        VaultGeneration generation = vaultCipherStore.generation(vault, request.generation());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!generation.getKeyEpoch().equals(request.keyEpoch()));
        for (VaultProtocol.ItemChange change : request.changes()) {
            VaultEnum.ItemChangeAction action = VaultEnum.ofCode(VaultEnum.ItemChangeAction.class, change.action());
            if (action == VaultEnum.ItemChangeAction.CREATE) {
                UUID itemId = VaultCodec.uuid(change.itemId());
                boolean occupied = vaultControlMapper.itemIdExists(vault.getId(), request.generation(), itemId);
                VaultEnum.Error.VERSION_CONFLICT.rejectIf(occupied);
            }
        }
        Map<String, VaultItemResultVO> receipts = new HashMap<>();
        // 先解除删除条目的容量占用，使满库时也能原子替换；回执仍按请求顺序返回。
        List<VaultProtocol.ItemChange> ordered = new ArrayList<>(request.changes());
        ordered.sort(Comparator.comparing(change -> {
            VaultEnum.ItemChangeAction action = VaultEnum.ofCode(VaultEnum.ItemChangeAction.class, change.action());
            return action == VaultEnum.ItemChangeAction.DELETE || action == VaultEnum.ItemChangeAction.DELETE_GROUP ? 0 : 1;
        }));
        for (VaultProtocol.ItemChange change : ordered) {
            VaultEnum.ItemChangeAction action = VaultEnum.ofCode(VaultEnum.ItemChangeAction.class, change.action());
            VaultItemResultVO receipt;
            if (action == VaultEnum.ItemChangeAction.DELETE_GROUP) {
                VaultProtocol.ItemLifecycle group = new VaultProtocol.ItemLifecycle(change.itemId(), request.generation(),
                        change.expectedRowVersion(), request.idempotencyKey());
                receipt = this.deleteGroupItem(session, group);
            } else {
                VaultProtocol.ItemWrite write = new VaultProtocol.ItemWrite(change.itemId(), request.generation(),
                        request.keyEpoch(), change.expectedRowVersion(), change.contentVersion(),
                        change.wrappedDek(), change.encryptedItem(), request.idempotencyKey());
                if (action == VaultEnum.ItemChangeAction.DELETE) {
                    receipt = this.deleteItem(session, write);
                } else {
                    receipt = this.saveItem(session, write, action == VaultEnum.ItemChangeAction.CREATE);
                }
            }
            receipts.put(change.itemId(), receipt);
        }
        VaultInfo updated = vaultAccess.lockVault(session);
        List<VaultItemResultVO> results = new ArrayList<>();
        for (VaultProtocol.ItemChange change : request.changes()) {
            VaultItemResultVO receipt = receipts.get(change.itemId());
            receipt.setCollectionRevision(updated.getCollectionRevision());
            results.add(receipt);
        }
        VaultItemBatchResultVO result = new VaultItemBatchResultVO();
        result.setGeneration(request.generation());
        result.setCollectionRevision(updated.getCollectionRevision());
        result.setResults(results);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultMetaVO changeMaster(VaultSession session, VaultProtocol.MasterChange request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        return vaultIdempotency.execute(
                session,
                VaultEnum.Operation.CHANGE_MASTER,
                request.idempotencyKey(),
                request,
                VaultMetaVO.class,
                () -> this.replaceMaster(session, account, request)
        );
    }

    private VaultMetaVO replaceMaster(VaultSession session, VaultAccount account, VaultProtocol.MasterChange request) {
        VaultInfo vault = vaultAccess.lockVault(session);
        this.validateWritable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getMetaRevision().equals(request.expectedMetaRevision()));
        List<VaultKeySlot> active = vaultCipherStore.slots(vault, vault.getActiveGeneration());
        VaultKeySlot normal = active.stream().filter(slot -> VaultEnum.SlotType.NORMAL.getCode().equals(slot.getSlotType())).findFirst().orElse(null);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(normal == null || active.size() != 2
                || !normal.getKeyId().equals(VaultCodec.uuid(request.normalSlot().keyId())));
        OffsetDateTime now = OffsetDateTime.now();
        LambdaUpdateWrapper<VaultKeySlot> update = Wrappers.lambdaUpdate();
        update.eq(VaultKeySlot::getId, normal.getId());
        update.set(VaultKeySlot::getIsDeleted, true);
        update.set(VaultKeySlot::getDeleteUser, session.ownerId());
        update.set(VaultKeySlot::getGmtDeleted, now);
        update.set(VaultKeySlot::getUpdateUser, session.ownerId());
        update.set(VaultKeySlot::getGmtModified, now);
        vaultKeySlotMapper.update(null, update);
        vaultCipherStore.addSlots(vault, vault.getActiveGeneration(), List.of(request.normalSlot()), session.ownerId());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getMetaRevision() >= VaultProtocol.MAX_VERSION);
        vault.setMetaRevision(vault.getMetaRevision() + 1);
        this.bump(vault, session.ownerId());
        vaultAccess.revoke(account, session.ownerId());
        vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.CHANGE_MASTER);
        return vaultCipherStore.meta(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSnapshot snapshot(VaultSession session) throws IOException {
        VaultContexts.requireRecent(session);
        VaultAccount account = vaultAccess.lock(session);
        vaultAccess.dataAccess(account);
        VaultInfo vault = vaultAccess.lockVault(session);
        VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        VaultSnapshot snapshot = vaultSnapshotStore.prepare(vault, session);
        boolean retained = false;
        try {
            vaultAccess.audit(session, vault.getId(), VaultEnum.AuditAction.BACKUP_DOWNLOAD);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try {
                                    snapshot.close();
                                } catch (IOException failure) {
                                    // 快照只含密文；删除失败不输出临时路径或原始异常。
                                    log.warn("密码库临时密文快照清理失败");
                                }
                            }
                        }
                    });
            retained = true;
            return snapshot;
        } finally {
            if (!retained) {
                snapshot.close();
            }
        }
    }
}
