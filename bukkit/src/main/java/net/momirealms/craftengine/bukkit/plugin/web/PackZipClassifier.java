package net.momirealms.craftengine.bukkit.plugin.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Identify a complete pack without extracting it onto disk. */
final class PackZipClassifier {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_ENTRY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 16 * 1024 * 1024;
    private PackZipClassifier() {}

    static boolean isCompletePack(byte[] archive) {
        boolean found = false;
        int files = 0, total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (++files > MAX_ENTRIES)
                    throw new IllegalArgumentException("El ZIP supera el limite de 256 entradas");
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (name.equals("pack.yml") || name.matches("[a-z0-9][a-z0-9_.-]{0,63}/pack\\.yml")) found = true;
                byte[] content = zip.readNBytes(MAX_ENTRY_BYTES + 1);
                if (content.length > MAX_ENTRY_BYTES)
                    throw new IllegalArgumentException("Archivo de mas de 2 MB dentro del ZIP");
                total += content.length;
                if (total > MAX_TOTAL_BYTES)
                    throw new IllegalArgumentException("ZIP extraido de mas de 16 MB");
            }
        } catch (IOException invalid) {
            throw new IllegalArgumentException("El ZIP esta dañado o es incompatible", invalid);
        }
        return found;
    }
}
