package net.momirealms.craftengine.core.entity.furniture.data;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.render.profile.RenderProfile;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.IntArrayTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Reads a provider-independent render profile from the furniture source item. */
public final class SourceItemProfileDataSourceConfig implements FurnitureDataSourceConfig<RenderProfile> {
    private static final SourceItemProfileDataSourceConfig INSTANCE = new SourceItemProfileDataSourceConfig();

    private SourceItemProfileDataSourceConfig() {
    }

    public static @NotNull SourceItemProfileDataSourceConfig fromConfig() {
        return INSTANCE;
    }

    static @Nullable RenderProfile readModernProfile(@Nullable Tag profileTag) {
        if (!(profileTag instanceof CompoundTag profile)) return null;
        return profile(
                readUUID(profile.get("id")),
                readString(profile.get("name")),
                readModernTextures(profile.get("properties"))
        );
    }

    static @Nullable RenderProfile readLegacyProfile(@Nullable Tag profileTag) {
        if (profileTag instanceof CompoundTag profile) {
            return profile(
                    readUUID(profile.get("Id")),
                    readString(profile.get("Name")),
                    readLegacyTextures(profile.get("Properties"))
            );
        }
        if (profileTag instanceof StringTag owner) {
            return profile(null, nonEmpty(owner.value()), null);
        }
        return null;
    }

    private static @Nullable RenderProfile profile(
            @Nullable UUID id,
            @Nullable String name,
            @Nullable String rawTextures
    ) {
        UUID normalizedId = RenderProfile.UNKNOWN_ID.equals(id) ? null : id;
        String normalizedName = nonEmpty(name);
        String normalizedTextures = nonEmpty(rawTextures);
        if (normalizedId == null && normalizedName == null && normalizedTextures == null) return null;
        return new RenderProfile(normalizedId, normalizedName, normalizedTextures);
    }

    private static @Nullable String readModernTextures(@Nullable Tag propertiesTag) {
        if (!(propertiesTag instanceof ListTag properties)) return null;
        for (int i = 0; i < properties.size(); i++) {
            if (!(properties.get(i) instanceof CompoundTag property)) continue;
            if ("textures".equals(readString(property.get("name")))) {
                String value = readString(property.get("value"));
                if (value != null) return value;
            }
        }
        return null;
    }

    private static @Nullable String readLegacyTextures(@Nullable Tag propertiesTag) {
        if (!(propertiesTag instanceof CompoundTag properties)) return null;
        Tag texturesTag = properties.get("textures");
        if (!(texturesTag instanceof ListTag textures) || textures.isEmpty()) return null;
        Tag textureTag = textures.getFirst();
        return textureTag instanceof CompoundTag texture ? readString(texture.get("Value")) : null;
    }

    private static @Nullable String readString(@Nullable Tag tag) {
        return tag instanceof StringTag string ? nonEmpty(string.value()) : null;
    }

    private static @Nullable String nonEmpty(@Nullable String value) {
        if (value == null || value.isEmpty()) return null;
        return value;
    }

    private static @Nullable UUID readUUID(@Nullable Tag tag) {
        if (tag instanceof IntArrayTag array && array.size() == 4) return array.getAsUUID();
        return tag instanceof StringTag string ? parseUUID(string.value()) : null;
    }

    private static @Nullable UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @Nullable RenderProfile readProfile(@NotNull Item item) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return readModernProfile(item.getComponentAsSparrowTag(DataComponentKeys.PROFILE));
        }
        return readLegacyProfile(item.getSparrowTag("SkullOwner"));
    }

    @Override
    public @NotNull FurnitureDataResolver<RenderProfile> bind(@NotNull Furniture furniture) {
        return () -> {
            Item sourceItem = furniture.sourceItem();
            return sourceItem == null ? null : readProfile(sourceItem);
        };
    }
}
