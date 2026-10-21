#!/usr/bin/env python3
"""Apply VSRender's Minecraft 26.3 item compatibility patch after upstream import.

Refuse unexpected source instead of silently editing the wrong version.
Idempotent: it is safe to rerun.
"""
from pathlib import Path
from shutil import copyfile
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"core/src/main/java/net/momirealms/craftengine/core/item/processor"
ITEM=BASE/"ItemProcessors.java"
COMP=BASE/"ComponentsProcessor.java"
TEST=ROOT/"core/src/test/java/net/momirealms/craftengine/core/item/processor/ItemComponent26_3Test.java"

def replace_once(contents, old, new, label):
    if new in contents:
        return contents
    if contents.count(old)!=1:
        raise RuntimeError(f"{label}: expected one upstream anchor; got {contents.count(old)}")
    return contents.replace(old,new)

def run():
    if not ITEM.exists() or not COMP.exists():
        raise SystemExit("Import full VSRender sources before applying the 26.3 patch")
    before_i=ITEM.read_text()
    before_c=COMP.read_text()
    item_old="""                        if (DataComponentKeys.contains(componentKey)) {
                            callback.accept(ComponentsProcessor.createSingle(componentKey, value));
                        }"""
    item_new="""                        if (DataComponentKeys.contains(componentKey)) {
                            for (Key compatible : ComponentsProcessor.compatibleKeys(
                                    componentKey, dataSection.keySet(), VersionHelper.isOrAbove26_3)) {
                                callback.accept(ComponentsProcessor.createSingle(compatible, value));
                            }
                        }"""
    after_i=replace_once(before_i,item_old,item_new,"ItemProcessors")
    after_c=before_c
    substitutions=[
        ("import net.momirealms.craftengine.core.util.TagParser;\n",
         "import net.momirealms.craftengine.core.util.TagParser;\nimport net.momirealms.craftengine.core.util.VersionHelper;\n","imports"),
        ("""            } else {
                arguments.add(getProvider(id, value));
            }""",
         """            } else {
                addCompatibleProviders(arguments, id, value, keys);
            }""","constructor"),
        ("""                } else {
                    arguments.add(getProvider(id, componentValue));
                }""",
         """                } else {
                    addCompatibleProviders(arguments, id, componentValue, componentsSection.keySet());
                }""","factory"),
        ("""    public record DynamicComponentProvider(Key type, Function<ItemBuildContext, Tag> function) {
    }

    public static ComponentsProcessor createSingle""",
         """    public record DynamicComponentProvider(Key type, Function<ItemBuildContext, Tag> function) {
    }

    // In 26.3 vanilla replaced swing_animation with separate attack and interact animations.
    // Preserve explicit new keys and drop the removed map_color component on 26.3.
    public static List<Key> compatibleKeys(Key id, Set<String> configuredKeys, boolean is26_3) {
        if (!is26_3) return List.of(id);
        if (DataComponentKeys.MAP_COLOR.equals(id)) return List.of();
        if (!DataComponentKeys.SWING_ANIMATION.equals(id)) return List.of(id);

        Set<Key> explicit = new java.util.HashSet<>();
        for (String key : configuredKeys) explicit.add(Key.of(key));
        List<Key> converted = new ArrayList<>(2);
        if (!explicit.contains(DataComponentKeys.ATTACK_ANIMATION)) {
            converted.add(DataComponentKeys.ATTACK_ANIMATION);
        }
        if (!explicit.contains(DataComponentKeys.INTERACT_ANIMATION)) {
            converted.add(DataComponentKeys.INTERACT_ANIMATION);
        }
        return converted;
    }

    private static void addCompatibleProviders(List<DynamicComponentProvider> target, Key id,
                                               ConfigValue value, Set<String> configuredKeys) {
        for (Key compatible : compatibleKeys(id, configuredKeys, VersionHelper.isOrAbove26_3)) {
            target.add(getProvider(compatible, value));
        }
    }

    public static ComponentsProcessor createSingle""","helper"),
    ]
    for old,new,label in substitutions:
        after_c=replace_once(after_c,old,new,label)
    # Atomic at the logical level: only write after every anchor passed.
    if after_i!=before_i: ITEM.write_text(after_i)
    if after_c!=before_c: COMP.write_text(after_c)
    fixture=ROOT/"porting/ItemComponent26_3Test.java"
    TEST.parent.mkdir(parents=True,exist_ok=True)
    copyfile(fixture,TEST)
    print("VSRender 26.3 component compatibility applied.")

if __name__=="__main__": run()
