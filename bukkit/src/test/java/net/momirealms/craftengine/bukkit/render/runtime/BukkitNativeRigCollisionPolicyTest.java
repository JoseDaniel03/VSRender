package net.momirealms.craftengine.bukkit.render.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitNativeRigCollisionPolicyTest {
    @Test
    void allowsUnrelatedEntity() {
        assertTrue(BukkitNativeRigCollisionPolicy.shouldCollide(false, false, false, false, false, false));
    }

    @Test
    void excludesOwnerPassengersSiblingsAndMountParticipantsIndependently() {
        for (int excluded = 0; excluded < 6; excluded++) {
            boolean[] flags = new boolean[6];
            flags[excluded] = true;
            assertFalse(BukkitNativeRigCollisionPolicy.shouldCollide(
                    flags[0], flags[1], flags[2], flags[3], flags[4], flags[5]
            ));
        }
    }
}