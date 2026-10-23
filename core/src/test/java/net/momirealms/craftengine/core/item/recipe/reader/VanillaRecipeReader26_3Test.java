package net.momirealms.craftengine.core.item.recipe.reader;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaRecipeReader26_3Test {
    private final VanillaRecipeReader26_3 reader = new VanillaRecipeReader26_3();

    @Test
    void readsExplicitVanillaCookingTime() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("cookingtime", 200);
        assertEquals(200, reader.cookingTime(recipe));
    }

    @Test
    void rejectsMissingCookingTimeRatherThanSilentlyDefaulting() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> reader.cookingTime(new JsonObject()));
        assertTrue(exception.getMessage().contains("cookingtime"));
    }

    @Test
    void rejectsNonpositiveCookingTime() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("cookingtime", 0);
        assertThrows(IllegalArgumentException.class, () -> reader.cookingTime(recipe));
        recipe.addProperty("cookingtime", -10);
        assertThrows(IllegalArgumentException.class, () -> reader.cookingTime(recipe));
    }

    @Test
    void olderReadersPreserveHistoricalDefault() {
        assertEquals(200, new VanillaRecipeReader26_1().cookingTime(new JsonObject()));
    }
}
