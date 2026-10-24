package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.ammo.AmmoStorageMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Compact PDC-friendly codec for embedded COUNT/SLOTS/RESOURCE container state. */
public final class AmmoContainerBinaryCodec {
    private static final int VERSION = 1;
    private static final int MAX_CAPACITY = 100_000;

    private AmmoContainerBinaryCodec() {}

    public static byte[] encode(AmmoContainerState state) {
        try {
            var bytes = new ByteArrayOutputStream(32 + (state.mode() == AmmoStorageMode.SLOTS ? state.capacity() : 0));
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(VERSION);
                out.writeByte(state.mode().ordinal());
                out.writeInt(state.capacity());
                if (state.mode() == AmmoStorageMode.SLOTS) {
                    out.writeInt(state.cursor());
                    for (AmmoSlotState slot : state.slotsSnapshot()) out.writeByte(slot.ordinal());
                } else {
                    out.writeDouble(state.available());
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Unexpected in-memory ammo serialization failure", impossible);
        }
    }

    public static AmmoContainerState decode(byte[] payload, CompiledAmmoSystem spec) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("ammo payload is empty");
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int version = in.readUnsignedByte();
            if (version != VERSION) throw new IllegalArgumentException("Unsupported ammo payload version: " + version);
            int modeOrdinal = in.readUnsignedByte();
            if (modeOrdinal >= AmmoStorageMode.values().length) throw new IllegalArgumentException("Invalid ammo storage mode");
            AmmoStorageMode mode = AmmoStorageMode.values()[modeOrdinal];
            int capacity = in.readInt();
            if (capacity < 1 || capacity > MAX_CAPACITY) throw new IllegalArgumentException("Invalid ammo capacity");
            if (mode != spec.storageMode() || capacity != spec.capacity()) {
                throw new IllegalArgumentException("Ammo payload does not match compiled item definition");
            }

            AmmoContainerState state;
            if (mode == AmmoStorageMode.SLOTS) {
                int cursor = in.readInt();
                var slots = new AmmoSlotState[capacity];
                for (int i = 0; i < capacity; i++) {
                    int slotOrdinal = in.readUnsignedByte();
                    if (slotOrdinal >= AmmoSlotState.values().length) throw new IllegalArgumentException("Invalid ammo slot state");
                    slots[i] = AmmoSlotState.values()[slotOrdinal];
                }
                state = AmmoContainerState.restore(spec, 0.0, slots, cursor);
            } else {
                state = AmmoContainerState.restore(spec, in.readDouble(), null, 0);
            }
            state.markClean();
            return state;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid ammo payload", exception);
        }
    }
}
