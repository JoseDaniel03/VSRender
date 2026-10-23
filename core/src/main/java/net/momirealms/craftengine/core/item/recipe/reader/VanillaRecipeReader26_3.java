package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonObject;

/**
 * Vanilla data pack recipe semantics introduced in Minecraft 26.3 (format 121.0).
 * Unlike older packs, all cooking recipes must explicitly provide cookingtime.
 */
public class VanillaRecipeReader26_3 extends VanillaRecipeReader26_1 {

    @Override
    public int cookingTime(JsonObject recipe) {
        if (!recipe.has("cookingtime") || recipe.get("cookingtime").isJsonNull()) {
            throw new IllegalArgumentException("Minecraft 26.3 cooking recipe requires cookingtime: " + recipe);
        }
        if (!recipe.get("cookingtime").isJsonPrimitive()
                || !recipe.getAsJsonPrimitive("cookingtime").isNumber()) {
            throw new IllegalArgumentException("Minecraft 26.3 cookingtime must be an integer");
        }
        final int ticks;
        try {
            ticks = Integer.parseInt(recipe.getAsJsonPrimitive("cookingtime").getAsString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Minecraft 26.3 cookingtime must be an integer", exception);
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException("Minecraft 26.3 cookingtime must be positive: " + ticks);
        }
        return ticks;
    }
}
