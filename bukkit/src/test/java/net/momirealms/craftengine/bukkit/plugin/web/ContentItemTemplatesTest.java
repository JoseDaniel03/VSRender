package net.momirealms.craftengine.bukkit.plugin.web;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ContentItemTemplatesTest {
    private static final String ITEM_ID = "studio:prueba";

    private static String generated(String type, Map<String,Object> options, int stack) throws Exception {
        Method generator = ContentWebServer.class.getDeclaredMethod(
                "itemSource", String.class, String.class, String.class, String.class, Map.class, int.class);
        generator.setAccessible(true);
        String material = type.equals("taladro") ? "iron_pickaxe" : "stone";
        return (String) generator.invoke(null, ITEM_ID, "Prueba", material, type, options, stack);
    }

    @Test
    void glowingBlockUsesNativeLuminanceAndSelfLoot() throws Exception {
        String yaml=generated("bloque_luz",Map.of("texture","studio:block/lampara",
                "block_sound","glass","luminance",12),64);
        assertTrue(yaml.contains("    behavior:\n      type: block_item\n      block: studio:prueba"));
        assertTrue(yaml.contains("blocks:\n  studio:prueba:"));
        assertTrue(yaml.contains("        luminance: 12\n"));
        assertTrue(yaml.contains("template: default:loot_table/self"));
        assertTrue(yaml.contains("      texture: studio:block/lampara\n"));
    }

    @Test
    void orientedBlockDefinesFourFacingVariantsAndRotations() throws Exception {
        String yaml=generated("bloque_orientable",Map.of("texture","studio:block/patron"),64);
        assertTrue(yaml.contains("parent: minecraft:block/template_glazed_terracotta"));
        assertTrue(yaml.contains("        facing:\n          type: 4-direction"));
        for (String dir : new String[]{"north","south","east","west"}) {
            assertTrue(yaml.contains("        facing="+dir+":\n          appearance: "+dir));
        }
        assertTrue(yaml.contains("            y: 270\n"));
        assertTrue(yaml.contains("            y: 180\n"));
        assertTrue(yaml.contains("            y: 90\n"));
    }

    @Test
    void drillUsesBuiltInRangeMiningLimitedToThreeBlocks() throws Exception {
        String yaml=generated("taladro",Map.of("max_damage",500,"mining_depth",3),1);
        assertTrue(yaml.contains("      max_damage: 500\n"));
        assertTrue(yaml.contains("      - type: range_mining_item\n"));
        for (int z=1;z<=3;z++) assertTrue(yaml.contains("          - 0,0,"+z+"\n"));
        assertFalse(yaml.contains("          - 0,0,4\n"));
    }

    @Test
    void seatAndDisplayUseExistingFurnitureSystems() throws Exception {
        String seat=generated("asiento",Map.of("texture","studio:item/silla"),1);
        assertTrue(seat.contains("      type: furniture_item\n"));
        assertTrue(seat.contains("                seats:\n                  - 0,0,-0.1 0 true\n"));

        String showcase=generated("expositor",Map.of("texture","studio:item/vitrina"),1);
        assertTrue(showcase.contains("          - type: display_item_furniture\n"));
        assertTrue(showcase.contains("              - type: shulker\n"));
        assertTrue(showcase.contains("                interaction_entity: true\n"));
        assertTrue(showcase.contains("template: default:loot_table/furniture"));
    }
}
