package com.muzhi.minierp.service.vault.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.*;
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
    private final VaultAccess access;

    private final VaultIdempotency idempotency;

    private final VaultCipherStore cipher;

    private final VaultControlMapper control;

    private final VaultInfoMapper vaults;

    private final VaultItemMapper items;

    private final VaultKeySlotMapper slots;

    private final VaultSnapshotStore snapshots;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultMetaVO meta(VaultSession session) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = control.lockVault(account.getId());
        if (vault != null) {
            return cipher.meta(vault);
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
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(
                session,
                VaultEnum.Operation.INITIALIZE,
                setup.idempotencyKey(),
                setup,
                VaultMetaVO.class,
                () -> this.createVault(session, setup)
        );
    }

    private VaultMetaVO createVault(VaultSession session, VaultProtocol.Setup setup) {
        VaultInfo vault = control.lockVault(session.accountId());
        if (vault == null) {
            vault = new VaultInfo();
            vault.setId(IdWorker.getId());
            vault.setAccountId(session.accountId());
            vault.setVaultId(VaultCodec.uuid(setup.vaultId()));
            vault.setMetaRevision(1L);
            vault.setCollectionRevision(1L);
            vault.setStatus(VaultEnum.VaultStatus.EMPTY.getCode());
            access.create(vault, session.ownerId());
            vaults.insert(vault);
        }
        VaultEnum.Error.ALREADY_INITIALIZED.rejectIf(vault.getActiveGeneration() != null
                || !VaultEnum.VaultStatus.EMPTY.getCode().equals(vault.getStatus()));
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getVaultId().equals(VaultCodec.uuid(setup.vaultId())));
        long generation = cipher.nextGeneration(vault);
        cipher.addGeneration(vault, generation, 1L, VaultEnum.GenerationStatus.ACTIVE, session.ownerId());
        cipher.addSlots(vault, generation, setup.keySlots(), session.ownerId());
        vault.setActiveGeneration(generation);
        vault.setStatus(VaultEnum.VaultStatus.ACTIVE.getCode());
        access.touch(vault, session.ownerId());
        vaults.updateById(vault);
        access.audit(session, vault.getId(), VaultEnum.AuditAction.INITIALIZE);
        return cipher.meta(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemPageVO items(VaultSession session, String cursor, int pageSize) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = access.lockVault(session);
        VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        UUID last = null;
        if (cursor != null) {
            byte[] decoded = VaultCodec.decode(cursor, 1, 256);
            String[] parts = new String(decoded, StandardCharsets.UTF_8).split(":", -1);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(parts.length != 5);
            boolean stale = !parts[0].equals(session.ownerId().toString()) || !parts[1].equals(vault.getVaultId().toString())
                    || !parts[2].equals(vault.getActiveGeneration().toString()) || !parts[3].equals(vault.getCollectionRevision().toString());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(stale);
            last = VaultCodec.uuid(parts[4]);
        }
        LambdaQueryWrapper<VaultItem> query = Wrappers.lambdaQuery();
        query.eq(VaultItem::getVaultId, vault.getId());
        query.eq(VaultItem::getGeneration, vault.getActiveGeneration());
        if (last != null) {
            query.gt(VaultItem::getItemId, last);
        }
        query.orderByAsc(VaultItem::getItemId);
        query.last("LIMIT " + (pageSize + 1));
        List<VaultItem> found = items.selectList(query);
        boolean more = found.size() > pageSize;
        List<VaultItem> page = more ? found.subList(0, pageSize) : found;
        VaultItemPageVO result = new VaultItemPageVO();
        result.setGeneration(vault.getActiveGeneration());
        result.setCollectionRevision(vault.getCollectionRevision());
        List<VaultProtocol.Item> pageItems = page.stream().map(cipher::view).toList();
        result.setItems(pageItems);
        String next = null;
        if (more) {
            String value = session.ownerId() + ":" + vault.getVaultId() + ":" + vault.getActiveGeneration() + ":"
                    + vault.getCollectionRevision() + ":" + page.getLast().getItemId();
            next = VaultCodec.encode(value.getBytes(StandardCharsets.UTF_8));
        }
        result.setNextCursor(next);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO save(VaultSession session, VaultProtocol.ItemWrite request, boolean create) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultEnum.Operation operation = create ? VaultEnum.Operation.CREATE_ITEM : VaultEnum.Operation.UPDATE_ITEM;
        return idempotency.execute(
                session,
                operation,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> this.saveItem(session, request, create)
        );
    }

    private VaultItemResultVO saveItem(VaultSession session, VaultProtocol.ItemWrite request, boolean create) {
        VaultInfo vault = access.lockVault(session);
        cipher.writable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultGeneration generation = cipher.generation(vault, request.generation());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!generation.getKeyEpoch().equals(request.keyEpoch()));
        VaultItem item;
        if (create) {
            List<VaultItem> current = cipher.items(vault, request.generation());
            int maximumItems = 2000;
            VaultEnum.Error.INVALID_REQUEST.rejectIf(current.size() >= maximumItems);
            VaultProtocol.Item value = new VaultProtocol.Item(request.itemId(), 1L, 1L, request.keyEpoch(), request.wrappedDek(), request.encryptedItem());
            item = cipher.newItem(vault, request.generation(), value, session.ownerId());
            items.insert(item);
        } else {
            item = this.item(vault, request.itemId());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(request.expectedRowVersion())
                    || request.contentVersion() != item.getContentVersion() + 1);
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(item.getRowVersion() >= VaultProtocol.MAX_VERSION);
            item.setContentVersion(request.contentVersion());
            item.setRowVersion(item.getRowVersion() + 1);
            cipher.content(item, request.wrappedDek(), request.encryptedItem());
            access.touch(item, session.ownerId());
            items.updateById(item);
        }
        this.bump(vault, session.ownerId());
        access.audit(session, vault.getId(), create ? VaultEnum.AuditAction.ITEM_CREATE : VaultEnum.AuditAction.ITEM_UPDATE);
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
        VaultItem item = items.selectOne(query);
        VaultEnum.Error.NOT_FOUND.rejectIf(item == null);
        return item;
    }

    private void bump(VaultInfo vault, Long actor) {
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getCollectionRevision() >= VaultProtocol.MAX_VERSION);
        vault.setCollectionRevision(vault.getCollectionRevision() + 1);
        access.touch(vault, actor);
        vaults.updateById(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultItemResultVO delete(VaultSession session, VaultProtocol.ItemWrite request) {
        VaultContexts.requireMfa(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(
                session,
                VaultEnum.Operation.DELETE_ITEM,
                request.idempotencyKey(),
                request,
                VaultItemResultVO.class,
                () -> this.deleteItem(session, request)
        );
    }

    private VaultItemResultVO deleteItem(VaultSession session, VaultProtocol.ItemWrite request) {
        VaultInfo vault = access.lockVault(session);
        cipher.writable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!Objects.equals(vault.getActiveGeneration(), request.generation()));
        VaultItem item = this.item(vault, request.itemId());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!item.getRowVersion().equals(request.expectedRowVersion()) || item.getRowVersion() >= VaultProtocol.MAX_VERSION);
        OffsetDateTime now = OffsetDateTime.now();
        LambdaUpdateWrapper<VaultItem> update = Wrappers.lambdaUpdate();
        update.eq(VaultItem::getId, item.getId());
        update.set(VaultItem::getIsDeleted, true);
        update.set(VaultItem::getDeleteUser, session.ownerId());
        update.set(VaultItem::getGmtDeleted, now);
        update.set(VaultItem::getUpdateUser, session.ownerId());
        update.set(VaultItem::getGmtModified, now);
        update.set(VaultItem::getRowVersion, item.getRowVersion() + 1);
        int changed = items.update(null, update);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(changed != 1);
        this.bump(vault, session.ownerId());
        access.audit(session, vault.getId(), VaultEnum.AuditAction.ITEM_DELETE);
        VaultItemResultVO result = new VaultItemResultVO();
        result.setDeleted(true);
        result.setCollectionRevision(vault.getCollectionRevision());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultMetaVO changeMaster(VaultSession session, VaultProtocol.MasterChange request) {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        return idempotency.execute(
                session,
                VaultEnum.Operation.CHANGE_MASTER,
                request.idempotencyKey(),
                request,
                VaultMetaVO.class,
                () -> this.replaceMaster(session, account, request)
        );
    }

    private VaultMetaVO replaceMaster(VaultSession session, VaultAccount account, VaultProtocol.MasterChange request) {
        VaultInfo vault = access.lockVault(session);
        cipher.writable(vault);
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(!vault.getMetaRevision().equals(request.expectedMetaRevision()));
        List<VaultKeySlot> active = cipher.slots(vault, vault.getActiveGeneration());
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
        slots.update(null, update);
        cipher.addSlots(vault, vault.getActiveGeneration(), List.of(request.normalSlot()), session.ownerId());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(vault.getMetaRevision() >= VaultProtocol.MAX_VERSION);
        vault.setMetaRevision(vault.getMetaRevision() + 1);
        this.bump(vault, session.ownerId());
        access.revoke(account, session.ownerId());
        access.audit(session, vault.getId(), VaultEnum.AuditAction.CHANGE_MASTER);
        return cipher.meta(vault);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultSnapshot snapshot(VaultSession session) throws IOException {
        VaultContexts.requireRecent(session);
        VaultAccount account = access.lock(session);
        access.dataAccess(account);
        VaultInfo vault = access.lockVault(session);
        VaultEnum.Error.NOT_FOUND.rejectIf(vault.getActiveGeneration() == null);
        VaultSnapshot snapshot = snapshots.prepare(vault, session);
        boolean retained = false;
        try {
            access.audit(session, vault.getId(), VaultEnum.AuditAction.BACKUP_DOWNLOAD);
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
