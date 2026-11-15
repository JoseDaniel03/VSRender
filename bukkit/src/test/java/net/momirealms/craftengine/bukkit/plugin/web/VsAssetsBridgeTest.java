package net.momirealms.craftengine.bukkit.plugin.web;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VsAssetsBridgeTest {
    @Test
    void acceptsProjectIdsAndPortableSourcePathsOnly() {
        assertTrue(VsAssetsBridge.validId("virgoland-1234"));
        assertTrue(VsAssetsBridge.validId("project_2"));
        for (String id : new String[]{"", ".", "../secrets", "Demo", "project space", "a/b"}) {
            assertFalse(VsAssetsBridge.validId(id), id);
        }
        assertTrue(VsAssetsBridge.validAssetPath("assets/models/escopeta.bbmodel"));
        assertTrue(VsAssetsBridge.validAssetPath("assets/textures/item/arma.png"));
        assertTrue(VsAssetsBridge.validAssetPath("assets/models/weapons/faro.bbmodel"));
    }

    @Test
    void refusesTraversalUrlsAndInvalidAssetPath() {
        for (String path : new String[]{
                "../other/file", "/etc/shadow", "assets/../../secret.txt",
                "other/file.bbmodel", "assets//dup", "assets/models/../secret",
                "assets/models/a\\b", "assets/models/file%2Fsecret",
                "assets/models/url?x=y", "assets/models/file#fragment",
                "assets/models/evil:port", "assets/models/file\u0000.txt"
        }) {
            assertFalse(VsAssetsBridge.validAssetPath(path), path);
        }
    }
}
