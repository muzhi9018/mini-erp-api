package com.muzhi.minierp.component.vault;

import com.muzhi.minierp.enums.VaultEnum;
import com.muzhi.minierp.model.vault.VaultProtocol;
import java.util.*;

/**
 * <p>
 * {@code VaultProtocolValidator}：Controller 调用的纯入参校验，不查询数据库
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-10-15
 */
public final class VaultProtocolValidator {
    private VaultProtocolValidator() { }

    public static void box(VaultProtocol.Box box, boolean item) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(box == null || !Integer.valueOf(1).equals(box.formatVersion())
                || !VaultProtocol.ALGORITHM.equals(box.algorithm()));
        VaultCodec.decode(box.iv(), 12, 12);
        VaultCodec.decode(box.ciphertext(), item ? 17 : 48, item ? 16400 : 48);
    }

    public static void slot(VaultProtocol.Slot slot) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(slot == null);
        VaultCodec.uuid(slot.slotId());
        VaultCodec.uuid(slot.keyId());
        VaultEnum.SlotType type = VaultEnum.ofCode(VaultEnum.SlotType.class, slot.slotType());
        VaultProtocolValidator.box(slot.wrappedVmk(), false);
        if (type == VaultEnum.SlotType.NORMAL) {
            VaultEnum.Error.INVALID_REQUEST.rejectIf(!VaultProtocol.KDF_PROFILE.equals(slot.kdfProfile()) || slot.recoverySalt() != null);
            VaultCodec.decode(slot.kdfSalt(), 16, 16);
            VaultCodec.decode(slot.wrapSalt(), 32, 32);
        } else {
            VaultEnum.Error.INVALID_REQUEST.rejectIf(slot.kdfProfile() != null || slot.kdfSalt() != null || slot.wrapSalt() != null);
            VaultCodec.decode(slot.recoverySalt(), 32, 32);
        }
    }

    public static void slots(List<VaultProtocol.Slot> slots) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(slots == null || slots.size() != 2);
        Set<String> types = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (VaultProtocol.Slot slot : slots) {
            VaultProtocolValidator.slot(slot);
            VaultEnum.Error.INVALID_REQUEST.rejectIf(!types.add(slot.slotType()) || !ids.add(slot.slotId()));
        }
    }

    public static void item(VaultProtocol.Item item) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(item == null);
        VaultCodec.uuid(item.itemId());
        VaultCodec.version(item.contentVersion());
        VaultCodec.version(item.keyEpoch());
        VaultProtocolValidator.box(item.wrappedDek(), false);
        VaultProtocolValidator.box(item.encryptedItem(), true);
    }

    /**
     * 校验原子变更的格式和动作字段约束，不解析加密内容。
     */
    public static void itemBatch(VaultProtocol.ItemBatch batch) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(batch == null);
        VaultCodec.uuid(batch.idempotencyKey());
        VaultCodec.version(batch.generation());
        VaultCodec.version(batch.keyEpoch());
        VaultCodec.version(batch.expectedCollectionRevision());
        int maximumChanges = 100;
        VaultEnum.Error.INVALID_REQUEST.rejectIf(batch.changes() == null || batch.changes().isEmpty()
                || batch.changes().size() > maximumChanges);
        Set<String> seen = new HashSet<>();
        for (VaultProtocol.ItemChange change : batch.changes()) {
            VaultEnum.Error.INVALID_REQUEST.rejectIf(change == null);
            VaultCodec.uuid(change.itemId());
            VaultEnum.Error.INVALID_REQUEST.rejectIf(!seen.add(change.itemId()));
            VaultEnum.ItemChangeAction action = VaultEnum.ofCode(VaultEnum.ItemChangeAction.class, change.action());
            if (action == VaultEnum.ItemChangeAction.CREATE) {
                VaultEnum.Error.INVALID_REQUEST.rejectIf(change.expectedRowVersion() != null
                        || !Long.valueOf(1).equals(change.contentVersion()));
            } else {
                VaultCodec.version(change.expectedRowVersion());
            }
            if (action == VaultEnum.ItemChangeAction.DELETE) {
                VaultEnum.Error.INVALID_REQUEST.rejectIf(change.contentVersion() != null
                        || change.wrappedDek() != null || change.encryptedItem() != null);
            } else {
                VaultCodec.version(change.contentVersion());
                VaultProtocolValidator.box(change.wrappedDek(), false);
                VaultProtocolValidator.box(change.encryptedItem(), true);
            }
        }
    }

    public static void credential(VaultProtocol.Credential credential, boolean registration) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(credential == null || credential.response() == null
                || !VaultProtocol.CREDENTIAL_TYPE.equals(credential.type()) || !Objects.equals(credential.id(), credential.rawId()));
        VaultCodec.decode(credential.rawId(), 1, 1024);
        VaultProtocol.CredentialResponse response = credential.response();
        VaultCodec.decode(response.clientDataJSON(), 1, 8192);
        if (registration) {
            VaultCodec.decode(response.attestationObject(), 1, 16384);
        } else {
            VaultCodec.decode(response.authenticatorData(), 37, 16384);
            VaultCodec.decode(response.signature(), 1, 4096);
            if (response.userHandle() != null) {
                VaultCodec.decode(response.userHandle(), 32, 32);
            }
        }
    }

    public static void rotationStart(VaultProtocol.RotationStart start, boolean restore) {
        VaultEnum.Error.INVALID_REQUEST.rejectIf(start == null);
        VaultCodec.uuid(start.rotationId());
        VaultCodec.uuid(start.idempotencyKey());
        VaultCodec.version(start.expectedMetaRevision());
        VaultCodec.version(start.expectedCollectionRevision());
        VaultProtocolValidator.slots(start.keySlots());
        if (restore) {
            VaultCodec.uuid(start.vaultId());
            VaultEnum.Error.INVALID_REQUEST.rejectIf(start.itemIds() == null || start.itemIds().size() > 2000);
            Set<String> seen = new HashSet<>();
            for (String id : start.itemIds()) {
                VaultCodec.uuid(id);
                VaultEnum.Error.INVALID_REQUEST.rejectIf(!seen.add(id));
            }
        } else {
            VaultEnum.Error.INVALID_REQUEST.rejectIf(start.itemIds() != null || start.vaultId() != null);
        }
    }
}
