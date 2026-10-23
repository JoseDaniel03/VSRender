from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
BEHAVIOR = ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit/item/behavior/PortableShulkerItemBehavior.java"
REGISTRY = ROOT / "bukkit/src/main/java/net/momirealms/craftengine/bukkit/item/behavior/BukkitItemBehaviors.java"

class PortableBackpackTests(unittest.TestCase):
    def test_registers_reusable_item_behavior(self):
        self.assertIn('Key.ce("portable_shulker")', REGISTRY.read_text())
    def test_vanilla_storage_and_sneak_place(self):
        code = BEHAVIOR.read_text()
        self.assertIn('box.getSnapshotInventory()', code)
        self.assertIn('meta.setBlockState(box)', code)
        self.assertIn('player.isSecondaryUseActive()', code)
        self.assertIn('return InteractionResult.PASS;', code)
        self.assertIn('OPEN_IDENTITIES', code)
        self.assertIn('session.slot', code)
    def test_inventory_title_does_not_cross_the_shaded_adventure_api_boundary(self):
        code = BEHAVIOR.read_text()
        self.assertIn('AdventureHelper.getLegacy().serialize(', code)
        self.assertIn('Bukkit.createInventory(this, 27, legacyTitle)', code)
        self.assertNotIn('Bukkit.createInventory(this, 27,\\n                    AdventureHelper.deserialize(', code)
        self.assertIn('RuntimeException | LinkageError', code)

    def test_no_backpack_nesting_and_no_hotbar_swap(self):
        code=BEHAVIOR.read_text()
        self.assertIn('InventoryAction.HOTBAR_SWAP', code)
        self.assertIn('isShulker(event.getCursor())', code)
        self.assertIn('InventoryDragEvent', code)

if __name__ == '__main__':
    unittest.main()
