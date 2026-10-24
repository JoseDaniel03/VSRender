package net.momirealms.craftengine.bukkit.plugin.web;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ContentItemTemplatesTest {
    private static final String ID = "studio:prueba";

    @SuppressWarnings("unchecked")
    private static Map<String,Object> generated(String type, Map<String,Object> options, int stack) throws Exception {
        Method generator = ContentWebServer.class.getDeclaredMethod(
                "itemSource", String.class, String.class, String.class, String.class, Map.class, int.class);
        generator.setAccessible(true);
        String material = type.equals("taladro") ? "iron_pickaxe" : "stone";
        String result = (String) generator.invoke(null, ID, "Prueba", material, type, options, stack);
        return (Map<String,Object>)new Load(
                LoadSettings.builder().setAllowDuplicateKeys(false).build()).loadFromString(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Object raw) { return (Map<String,Object>)raw; }

    private static Map<String,Object> item(Map<String,Object> yaml) {
        return map(map(yaml.get("items")).get(ID));
    }

    private static Map<String,Object> block(Map<String,Object> yaml) {
        return map(map(yaml.get("blocks")).get(ID));
    }

    @Test
    void glowingBlockUsesBuiltInLuminanceAndSelfLoot() throws Exception {
        Map<String,Object> doc=generated("bloque_luz", Map.of(
                "texture","studio:block/lampara","block_sound","glass","luminance",12),64);
        assertEquals("block_item",map(item(doc).get("behavior")).get("type"));
        Map<String,Object> settings=map(block(doc).get("settings"));
        assertEquals(12,map(settings.get("overrides")).get("luminance"));
        assertEquals("default:loot_table/self",map(block(doc).get("loot")).get("template"));
        assertEquals("studio:block/lampara",map(block(doc).get("state")).get("texture"));
    }

    @Test
    void orientedBlockHasFourVanillaFacingVariants() throws Exception {
        Map<String,Object> doc=generated("bloque_orientable",Map.of(
                "texture","studio:block/patron","hardness",2.0),64);
        Map<String,Object> states=map(block(doc).get("states"));
        Map<String,Object> variants=map(states.get("variants"));
        assertEquals(4,variants.size());
        assertEquals("north", map(map(states.get("properties")).get("facing")).get("default"));
        for (String facing : List.of("north","south","east","west")) {
            assertEquals(facing,map(variants.get("facing="+facing)).get("appearance"));
        }
        assertEquals("minecraft:block/template_glazed_terracotta",
                map(map(item(doc).get("model")).get("generation")).get("parent"));
    }

    @Test
    void drillUsesNativeRangeMiningLimitedToThreeBlocks() throws Exception {
        Map<String,Object> doc=generated("taladro",Map.of("max_damage",500,"mining_depth",3),1);
        assertEquals(500,map(item(doc).get("data")).get("max_damage"));
        List<?> behavior=(List<?>)item(doc).get("behavior");
        assertEquals("range_mining_item",map(behavior.get(0)).get("type"));
        assertEquals(List.of("0,0,1","0,0,2","0,0,3"),map(behavior.get(0)).get("range"));
    }

    @Test
    void seatAndDisplayReuseNativeFurnitureBehaviors() throws Exception {
        Map<String,Object> seat=generated("asiento",Map.of("texture","studio:item/silla"),1);
        Map<String,Object> furniture=map(map(item(seat).get("behavior")).get("furniture"));
        Map<String,Object> ground=map(map(furniture.get("variants")).get("ground"));
        assertEquals(1,((List<?>)map(((List<?>)ground.get("hitboxes")).get(0)).get("seats")).size());

        Map<String,Object> showcase=generated("expositor",Map.of("texture","studio:item/vitrina"),1);
        Map<String,Object> display=map(map(item(showcase).get("behavior")).get("furniture"));
        List<?> behaviors=(List<?>) display.get("behaviors");
        assertEquals("display_item_furniture",map(behaviors.get(0)).get("type"));
        Map<String,Object> hitbox=map(((List<?>)map(map(display.get("variants")).get("ground")).get("hitboxes")).get(0));
        assertEquals("shulker",hitbox.get("type"));
        assertEquals(true,hitbox.get("interaction_entity"));
    }
}
