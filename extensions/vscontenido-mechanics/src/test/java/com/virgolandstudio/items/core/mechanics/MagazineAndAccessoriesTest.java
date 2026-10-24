package com.virgolandstudio.items.core.mechanics;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.accessory.*;
import com.virgolandstudio.items.api.ammo.*;
import com.virgolandstudio.items.core.accessory.*;
import com.virgolandstudio.items.core.ammo.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class MagazineAndAccessoriesTest {
    @Test void magazinesHavePersistentPortableState() {
        var registry = new MagazineRegistry();
        var magazine = registry.create(NamespacedId.parse("faro12:magazine"), "faro12:shell", 6, 2);
        assertEquals(4, magazine.addRounds(7));
        assertEquals(6, magazine.rounds());
        var decoded = MagazineBinaryCodec.decode(MagazineBinaryCodec.encode(magazine));
        assertEquals(magazine.id(), decoded.id());
        assertEquals(magazine.rounds(), decoded.rounds());
        assertEquals("faro12:shell", decoded.ammoType());
    }
    @Test void embeddedAmmoSerializesWithoutBukkit() {
        var spec = new CompiledAmmoSystem("faro12:shell", AmmoContainerKind.INTERNAL_MAGAZINE,
            AmmoStorageMode.COUNT, 6, false, FeedMechanismKind.MANUAL_CHAMBER, 1, ReloadMechanismKind.TOP_OFF);
        var state = AmmoContainerState.count(6, 3);
        var decoded = AmmoContainerBinaryCodec.decode(AmmoContainerBinaryCodec.encode(state), spec);
        assertEquals(3.0, decoded.available());
        assertFalse(decoded.dirty());
    }
    @Test void attachmentModifiersApplyInDefinedOrder() {
        var modifiers = new AccessoryModifierSet(List.of(
            new AccessoryModifierDefinition("recoil.pitch", AccessoryModifierOperation.ADD, 2.0),
            new AccessoryModifierDefinition("recoil.pitch", AccessoryModifierOperation.SET, 4.0),
            new AccessoryModifierDefinition("recoil.pitch", AccessoryModifierOperation.MULTIPLY, 0.5)));
        assertEquals(3.0, modifiers.number("recoil.pitch", 99));
        assertEquals(99.0, modifiers.number("unmodified", 99));
    }
    @Test void registryAndLoadoutAreRendererIndependent() {
        var ids=NamespacedId.parse("faro12:stabilizer");
        var inventory=NamespacedId.parse("faro12:stabilizer_item");
        var definition=new AccessoryDefinition(1, ids, inventory, "Stabilizer", "BARREL",
            Set.of("shotgun"), Set.of("weapon"), List.of(), Map.of());
        var registry=new AccessoryRegistry();
        registry.publish(definition);
        assertEquals(definition, registry.findByItemDefinitionId(inventory).orElseThrow());
        assertTrue(new CompiledAttachmentSlots(List.of("barrel")).has("BARREL"));
        var loadout=new AttachmentLoadout();
        loadout.install("barrel", new InstalledAccessory(ids, inventory, UUID.randomUUID()));
        assertEquals(1, AttachmentLoadoutCodec.decode(AttachmentLoadoutCodec.encode(loadout)).size());
    }
}
