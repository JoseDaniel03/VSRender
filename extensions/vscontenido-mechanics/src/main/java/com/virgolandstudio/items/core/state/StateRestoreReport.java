package com.virgolandstudio.items.core.state;

/** Result of restoring a persisted state snapshot into a possibly newer layout. */
public record StateRestoreReport(
        int restored,
        int missingInLayout,
        int incompatible,
        int defaulted
) {
    public boolean requiresRewrite() {
        return missingInLayout > 0 || incompatible > 0 || defaulted > 0;
    }
}
