package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.NamespacedId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Versioned Base64 codec so an entire attachment loadout fits the existing persistent STRING state type. */
public final class AttachmentLoadoutCodec {
    private static final int VERSION = 1;

    private AttachmentLoadoutCodec() {}

    public static String encode(AttachmentLoadout loadout) {
        if (loadout == null || loadout.isEmpty()) return "";
        try {
            var bytes = new ByteArrayOutputStream(128);
            var out = new DataOutputStream(bytes);
            out.writeByte(VERSION);
            var snapshot = loadout.snapshot();
            out.writeShort(snapshot.size());
            for (var entry : snapshot.entrySet()) {
                InstalledAccessory accessory = entry.getValue();
                out.writeUTF(entry.getKey());
                out.writeUTF(accessory.accessoryDefinitionId().toString());
                out.writeUTF(accessory.itemDefinitionId().toString());
                out.writeLong(accessory.itemInstanceId().getMostSignificantBits());
                out.writeLong(accessory.itemInstanceId().getLeastSignificantBits());
            }
            out.flush();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException("Unexpected in-memory attachment encoding failure", impossible);
        }
    }

    public static AttachmentLoadout decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return new AttachmentLoadout();
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            var in = new DataInputStream(new ByteArrayInputStream(bytes));
            int version = in.readUnsignedByte();
            if (version != VERSION) throw new IllegalArgumentException("Unsupported attachment loadout version: " + version);
            int count = in.readUnsignedShort();
            var values = new LinkedHashMap<String, InstalledAccessory>();
            for (int i = 0; i < count; i++) {
                String slot = in.readUTF();
                var accessoryId = NamespacedId.parse(in.readUTF());
                var itemDefinitionId = NamespacedId.parse(in.readUTF());
                var instanceId = new UUID(in.readLong(), in.readLong());
                values.put(slot, new InstalledAccessory(accessoryId, itemDefinitionId, instanceId));
            }
            return new AttachmentLoadout(values);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid attachment loadout payload", ex);
        }
    }
}
