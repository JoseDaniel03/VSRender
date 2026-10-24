package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.NamespacedId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Compact persistence codec for magazine instances stored inside ItemStack PDC payloads. */
public final class MagazineBinaryCodec {
    private static final int VERSION = 1;
    private static final int MAX_TEXT_BYTES = 4096;

    private MagazineBinaryCodec() {}

    public static byte[] encode(MagazineInstance magazine) {
        try {
            var bytes = new ByteArrayOutputStream(96);
            try (var out = new DataOutputStream(bytes)) {
                out.writeByte(VERSION);
                out.writeLong(magazine.id().getMostSignificantBits());
                out.writeLong(magazine.id().getLeastSignificantBits());
                writeString(out, magazine.definitionId().toString());
                writeString(out, magazine.ammoType());
                out.writeInt(magazine.capacity());
                out.writeInt(magazine.rounds());
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("Unexpected in-memory magazine serialization failure", impossible);
        }
    }

    public static Snapshot decode(byte[] payload) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("magazine payload is empty");
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int version = in.readUnsignedByte();
            if (version != VERSION) throw new IllegalArgumentException("Unsupported magazine payload version: " + version);
            UUID id = new UUID(in.readLong(), in.readLong());
            NamespacedId definitionId = NamespacedId.parse(readString(in));
            String ammoType = readString(in);
            int capacity = in.readInt();
            int rounds = in.readInt();
            if (capacity < 1 || rounds < 0 || rounds > capacity) throw new IllegalArgumentException("Invalid magazine payload values");
            return new Snapshot(id, definitionId, ammoType, capacity, rounds);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid magazine payload", exception);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_TEXT_BYTES) throw new IllegalArgumentException("Magazine text field is too long");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        if (length > MAX_TEXT_BYTES) throw new IllegalArgumentException("Magazine text field is too long");
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new IOException("Truncated magazine string");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record Snapshot(UUID id, NamespacedId definitionId, String ammoType, int capacity, int rounds) {}
}
