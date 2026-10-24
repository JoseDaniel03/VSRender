package com.virgolandstudio.items.core.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StateBinaryCodec {
    private static final int FORMAT_VERSION = 1;

    private StateBinaryCodec() {}

    public static byte[] encode(StateContainer state) {
        try {
            var buffer = new ByteArrayOutputStream(64);
            var out = new DataOutputStream(buffer);
            out.writeByte(FORMAT_VERSION);
            var snapshot = state.persistentSnapshot();
            out.writeShort(snapshot.size());
            for (StateSlot slot : state.layout().slots()) {
                if (!slot.persistent()) continue;
                Object value = snapshot.get(slot.key());
                out.writeUTF(slot.key());
                out.writeByte(slot.type().ordinal());
                out.writeBoolean(value != null);
                if (value == null) continue;
                switch (slot.type()) {
                    case INTEGER -> out.writeLong(((Number) value).longValue());
                    case DOUBLE -> out.writeDouble(((Number) value).doubleValue());
                    case BOOLEAN -> out.writeBoolean((Boolean) value);
                    case STRING -> out.writeUTF((String) value);
                }
            }
            out.flush();
            return buffer.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Unexpected in-memory state encoding failure", impossible);
        }
    }

    public static Map<String, Object> decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return Map.of();
        try {
            var in = new DataInputStream(new ByteArrayInputStream(bytes));
            int version = in.readUnsignedByte();
            if (version != FORMAT_VERSION) throw new IllegalArgumentException("Unsupported state format version: " + version);
            int count = in.readUnsignedShort();
            var values = new LinkedHashMap<String, Object>(count);
            for (int i = 0; i < count; i++) {
                String key = in.readUTF();
                int ordinal = in.readUnsignedByte();
                if (ordinal < 0 || ordinal >= com.virgolandstudio.items.api.state.StateType.values().length) {
                    throw new IllegalArgumentException("Unknown state type ordinal: " + ordinal);
                }
                var type = com.virgolandstudio.items.api.state.StateType.values()[ordinal];
                boolean present = in.readBoolean();
                Object value = null;
                if (present) {
                    value = switch (type) {
                        case INTEGER -> in.readLong();
                        case DOUBLE -> in.readDouble();
                        case BOOLEAN -> in.readBoolean();
                        case STRING -> in.readUTF();
                    };
                }
                values.put(key, value);
            }
            return java.util.Collections.unmodifiableMap(values);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid persistent item state payload", ex);
        }
    }
}
