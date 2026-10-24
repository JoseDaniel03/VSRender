package com.virgolandstudio.items.core.ranged;

import java.util.Map;
import java.util.Objects;

/** Compiled O(1) exact/group/default material resolution table. Exact rules always win over groups. */
public final class CompiledImpactResponse {
    private final double blockPenetrationPower;
    private final int maxBlockPenetrations;
    private final int maxRicochets;
    private final CompiledMaterialImpact defaultResponse;
    private final Map<String, CompiledMaterialImpact> exactRules;
    private final Map<String, GroupMatch> expandedGroupRules;

    public CompiledImpactResponse(
            double blockPenetrationPower,
            int maxBlockPenetrations,
            CompiledMaterialImpact defaultResponse,
            Map<String, CompiledMaterialImpact> exactRules
    ) {
        this(blockPenetrationPower, maxBlockPenetrations, 0, defaultResponse, exactRules, Map.of());
    }

    public CompiledImpactResponse(
            double blockPenetrationPower,
            int maxBlockPenetrations,
            CompiledMaterialImpact defaultResponse,
            Map<String, CompiledMaterialImpact> exactRules,
            Map<String, GroupMatch> expandedGroupRules
    ) {
        this(blockPenetrationPower, maxBlockPenetrations, 0, defaultResponse, exactRules, expandedGroupRules);
    }

    public CompiledImpactResponse(
            double blockPenetrationPower,
            int maxBlockPenetrations,
            int maxRicochets,
            CompiledMaterialImpact defaultResponse,
            Map<String, CompiledMaterialImpact> exactRules,
            Map<String, GroupMatch> expandedGroupRules
    ) {
        if (blockPenetrationPower < 0.0 || !Double.isFinite(blockPenetrationPower)) {
            throw new IllegalArgumentException("blockPenetrationPower must be finite and >= 0");
        }
        if (maxBlockPenetrations < 0 || maxBlockPenetrations > 16) {
            throw new IllegalArgumentException("maxBlockPenetrations must be between 0 and 16");
        }
        if (maxRicochets < 0 || maxRicochets > 16) {
            throw new IllegalArgumentException("maxRicochets must be between 0 and 16");
        }
        this.blockPenetrationPower = blockPenetrationPower;
        this.maxBlockPenetrations = maxBlockPenetrations;
        this.maxRicochets = maxRicochets;
        this.defaultResponse = Objects.requireNonNull(defaultResponse, "defaultResponse");
        this.exactRules = Map.copyOf(Objects.requireNonNull(exactRules, "exactRules"));
        this.expandedGroupRules = Map.copyOf(Objects.requireNonNull(expandedGroupRules, "expandedGroupRules"));
    }

    public double blockPenetrationPower() { return blockPenetrationPower; }
    public int maxBlockPenetrations() { return maxBlockPenetrations; }
    public int maxRicochets() { return maxRicochets; }
    public CompiledMaterialImpact defaultResponse() { return defaultResponse; }
    public Map<String, CompiledMaterialImpact> exactRules() { return exactRules; }
    public Map<String, GroupMatch> expandedGroupRules() { return expandedGroupRules; }

    public CompiledMaterialImpact resolve(String materialId) { return resolveDetailed(materialId).impact(); }

    public ResolvedMaterialImpact resolveDetailed(String materialId) {
        String normalized = MaterialGroupRegistry.normalizeMaterialId(materialId == null || materialId.isBlank() ? "minecraft:air" : materialId);
        CompiledMaterialImpact exact = exactRules.get(normalized);
        if (exact != null) return new ResolvedMaterialImpact(exact, ResolvedMaterialImpact.RuleSource.EXACT, "");
        GroupMatch group = expandedGroupRules.get(normalized);
        if (group != null) return new ResolvedMaterialImpact(group.impact(), ResolvedMaterialImpact.RuleSource.GROUP, group.groupId());
        return new ResolvedMaterialImpact(defaultResponse, ResolvedMaterialImpact.RuleSource.DEFAULT, "");
    }

    public record GroupMatch(String groupId, CompiledMaterialImpact impact) {
        public GroupMatch {
            groupId = MaterialGroupRegistry.normalizeGroupId(groupId);
            Objects.requireNonNull(impact, "impact");
        }
    }
}
