package com.muzhi.minierp.component.vault;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.muzhi.minierp.entity.vault.*;
import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.*;
import com.muzhi.minierp.service.vault.IVaultControlService;
import com.muzhi.minierp.service.vault.IVaultGenerationService;
import com.muzhi.minierp.service.vault.IVaultItemService;
import com.muzhi.minierp.service.vault.IVaultKeySlotService;
import com.muzhi.minierp.util.BeanConvertUtils;
import com.muzhi.minierp.vo.vault.VaultMetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <p>
 * {@code VaultCipherStore}：密文盒持久化与规范清单，密钥和条目明文不进入服务端
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vault", name = "enabled", havingValue = "true")
public class VaultCipherStore {

    private final IVaultKeySlotService vaultKeySlotService;

    private final IVaultItemService vaultItemService;

    private final IVaultGenerationService vaultGenerationService;

    private final IVaultControlService vaultControlService;

    private final VaultAccess vaultAccess;

    public VaultGeneration generation(VaultInfo vault, long number) {
        VaultGeneration generation = vaultControlService.generation(vault.getId(), number);
        VaultEnum.Error.NOT_FOUND.rejectIf(generation == null);
        return generation;
    }

    public List<VaultKeySlot> slots(VaultInfo vault, long generation) {
        return vaultKeySlotService.listByGeneration(vault.getId(), generation);
    }

    public List<VaultItem> items(VaultInfo vault, long generation) {
        return vaultItemService.listByGeneration(vault.getId(), generation);
    }

    public long nextGeneration(VaultInfo vault) {
        Long maximum = vaultControlService.maxGeneration(vault.getId());
        VaultEnum.Error.VERSION_CONFLICT.rejectIf(maximum >= VaultProtocol.MAX_VERSION);
        return maximum + 1;
    }

    public void addGeneration(VaultInfo vault, long generation, long epoch, VaultEnum.GenerationStatus status, Long actor) {
        VaultGeneration record = new VaultGeneration();
        record.setId(IdWorker.getId());
        record.setVaultId(vault.getId());
        record.setGeneration(generation);
        record.setKeyEpoch(epoch);
        record.setStatus(status.getCode());
        vaultAccess.create(record, actor);
        vaultGenerationService.save(record);
    }

    public void addSlots(VaultInfo vault, long generation, List<VaultProtocol.Slot> values, Long actor) {
        for (VaultProtocol.Slot value : values) {
            VaultKeySlot slot = new VaultKeySlot();
            slot.setId(IdWorker.getId());
            slot.setVaultId(vault.getId());
            slot.setGeneration(generation);
            slot.setSlotId(VaultCodec.uuid(value.slotId()));
            slot.setSlotType(value.slotType());
            slot.setKeyId(VaultCodec.uuid(value.keyId()));
            slot.setFormatVersion(value.wrappedVmk().formatVersion());
            slot.setAlgorithm(value.wrappedVmk().algorithm());
            slot.setIv(VaultCodec.decode(value.wrappedVmk().iv(), 12, 12));
            slot.setCiphertext(VaultCodec.decode(value.wrappedVmk().ciphertext(), 48, 48));
            slot.setKdfProfile(value.kdfProfile());
            if (value.kdfSalt() != null) {
                slot.setKdfSalt(VaultCodec.decode(value.kdfSalt(), 16, 16));
                slot.setWrapSalt(VaultCodec.decode(value.wrapSalt(), 32, 32));
            }
            if (value.recoverySalt() != null) {
                slot.setRecoverySalt(VaultCodec.decode(value.recoverySalt(), 32, 32));
            }
            vaultAccess.create(slot, actor);
            vaultKeySlotService.save(slot);
        }
    }

    public VaultItem newItem(VaultInfo vault, long generation, VaultProtocol.Item value, Long actor) {
        VaultItem item = new VaultItem();
        item.setId(IdWorker.getId());
        item.setVaultId(vault.getId());
        item.setGeneration(generation);
        item.setItemId(VaultCodec.uuid(value.itemId()));
        item.setKeyEpoch(value.keyEpoch());
        item.setContentVersion(value.contentVersion());
        item.setRowVersion(1L);
        this.content(item, value.wrappedDek(), value.encryptedItem());
        vaultAccess.create(item, actor);
        return item;
    }

    public void content(VaultItem item, VaultProtocol.Box wrapped, VaultProtocol.Box encrypted) {
        item.setWrappedDekFormatVersion(wrapped.formatVersion());
        item.setWrappedDekAlgorithm(wrapped.algorithm());
        item.setWrappedDekIv(VaultCodec.decode(wrapped.iv(), 12, 12));
        item.setWrappedDekCiphertext(VaultCodec.decode(wrapped.ciphertext(), 48, 48));
        item.setEncryptedItemFormatVersion(encrypted.formatVersion());
        item.setEncryptedItemAlgorithm(encrypted.algorithm());
        item.setEncryptedItemIv(VaultCodec.decode(encrypted.iv(), 12, 12));
        item.setEncryptedItemCiphertext(VaultCodec.decode(encrypted.ciphertext(), 17, 16400));
    }

    public VaultProtocol.Slot view(VaultKeySlot slot) {
        VaultProtocol.Box box = new VaultProtocol.Box(slot.getFormatVersion(), slot.getAlgorithm(),
                VaultCodec.encode(slot.getIv()), VaultCodec.encode(slot.getCiphertext()));
        return new VaultProtocol.Slot(slot.getSlotId().toString(), slot.getSlotType(), slot.getKeyId().toString(),
                slot.getKdfProfile(), this.optional(slot.getKdfSalt()), this.optional(slot.getWrapSalt()), this.optional(slot.getRecoverySalt()), box);
    }

    public VaultProtocol.Item view(VaultItem item) {
        VaultProtocol.Box wrapped = new VaultProtocol.Box(item.getWrappedDekFormatVersion(), item.getWrappedDekAlgorithm(),
                VaultCodec.encode(item.getWrappedDekIv()), VaultCodec.encode(item.getWrappedDekCiphertext()));
        VaultProtocol.Box encrypted = new VaultProtocol.Box(item.getEncryptedItemFormatVersion(), item.getEncryptedItemAlgorithm(),
                VaultCodec.encode(item.getEncryptedItemIv()), VaultCodec.encode(item.getEncryptedItemCiphertext()));
        return new VaultProtocol.Item(item.getItemId().toString(), item.getContentVersion(), item.getRowVersion(), item.getKeyEpoch(), wrapped, encrypted);
    }

    private String optional(byte[] value) {
        return value == null ? null : VaultCodec.encode(value);
    }

    public VaultMetaVO meta(VaultInfo vault) {
        VaultMetaVO result = BeanConvertUtils.convert(vault, VaultMetaVO.class);
        result.setInitialized(vault.getActiveGeneration() != null);
        if (vault.getActiveGeneration() != null) {
            VaultGeneration current = this.generation(vault, vault.getActiveGeneration());
            VaultEnum.Error.VERSION_CONFLICT.rejectIf(!VaultEnum.GenerationStatus.ACTIVE.getCode().equals(current.getStatus()));
            result.setKeyEpoch(current.getKeyEpoch());
            List<VaultKeySlot> activeSlots = this.slots(vault, current.getGeneration());
            List<VaultProtocol.Slot> keySlots = activeSlots.stream().map(this::view).toList();
            result.setKeySlots(keySlots);
        }
        return result;
    }

    public byte[] manifest(VaultSession session, VaultInfo vault, VaultRotation rotation) {
        VaultGeneration generation = this.generation(vault, rotation.getTargetGeneration());
        List<Object> slotArrays = new ArrayList<>();
        for (VaultKeySlot raw : this.slots(vault, generation.getGeneration())) {
            VaultProtocol.Slot slot = this.view(raw);
            VaultProtocol.Box box = slot.wrappedVmk();
            slotArrays.add(Arrays.asList(slot.slotType(), slot.slotId(), slot.keyId(), box.formatVersion(), box.algorithm(),
                    box.iv(), box.ciphertext(), slot.kdfProfile(), slot.kdfSalt(), slot.wrapSalt(), slot.recoverySalt()));
        }
        List<Object> itemArrays = new ArrayList<>();
        for (VaultItem raw : this.items(vault, generation.getGeneration())) {
            VaultProtocol.Item item = this.view(raw);
            VaultProtocol.Box wrapped = item.wrappedDek();
            VaultProtocol.Box encrypted = item.encryptedItem();
            itemArrays.add(List.of(item.itemId(), item.contentVersion(), item.keyEpoch(), wrapped.formatVersion(), wrapped.algorithm(),
                    wrapped.iv(), wrapped.ciphertext(), encrypted.formatVersion(), encrypted.algorithm(), encrypted.iv(), encrypted.ciphertext()));
        }
        List<Object> context = List.of("mini-erp-vault", 1, "staging-manifest", session.ownerId().toString(),
                vault.getVaultId().toString(), rotation.getRotationId().toString(), generation.getGeneration(), generation.getKeyEpoch(), slotArrays, itemArrays);
        return VaultCodec.digest(context);
    }
}
