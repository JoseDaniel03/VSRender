package net.momirealms.craftengine.bukkit.plugin.web;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class PackZipClassifierTest {
    @Test void routesFullPackWithMoreThan48Files() throws Exception {
        assertTrue(PackZipClassifier.isCompletePack(zip("faro12/pack.yml", 90)));
    }
    @Test void routesRootPack() throws Exception {
        assertTrue(PackZipClassifier.isCompletePack(zip("pack.yml", 2)));
    }
    @Test void preservesSingleItemImports() throws Exception {
        assertFalse(PackZipClassifier.isCompletePack(zip("configuration/items/item.yml", 2)));
    }
    @Test void rejectsOver256Entries() {
        assertThrows(IllegalArgumentException.class, () -> PackZipClassifier.isCompletePack(zip("faro12/pack.yml", 256)));
    }
    private static byte[] zip(String manifest, int n) throws IOException {
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output)){
            zip.putNextEntry(new ZipEntry(manifest));
            zip.write("namespace: faro12".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            for(int i=0;i<n;i++){
                zip.putNextEntry(new ZipEntry("faro12/resourcepack/assets/faro12/models/item/"+i+".json"));
                zip.write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
