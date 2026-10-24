package com.virgolandstudio.items.core.reload;

import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.reload.ReloadSequenceDefinition;
import com.virgolandstudio.items.api.reload.ReloadStageDefinition;
import com.virgolandstudio.items.core.behavior.BehaviorCompiler;
import com.virgolandstudio.items.core.compile.ValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Compiles authored reload stages once; no action/condition lookup or map interpretation occurs per reload tick. */
public final class ReloadSequenceCompiler {
    private static final int MAX_STAGE_ITERATIONS = 128;

    private final BehaviorCompiler behaviors;

    public ReloadSequenceCompiler(BehaviorCompiler behaviors) {
        this.behaviors = behaviors;
    }

    public ReloadSequence compile(Object raw) {
        ReloadSequenceDefinition definition = parse(raw);
        var stages = new ArrayList<ReloadStage>(definition.stages().size());
        var ids = new HashSet<String>();
        var errors = new ArrayList<String>();

        for (ReloadStageDefinition stage : definition.stages()) {
            if (!ids.add(stage.id())) {
                errors.add("Duplicate reload stage id: " + stage.id());
                continue;
            }
            if (stage.maxIterations() > MAX_STAGE_ITERATIONS) {
                errors.add("Reload stage " + stage.id() + " maxIterations exceeds " + MAX_STAGE_ITERATIONS);
                continue;
            }
            var conditions = stage.conditions().stream().map(behaviors::compileCondition).toList();
            var actions = stage.actions().stream().map(behaviors::compileAction).toList();
            var repeatWhile = stage.repeatWhile().stream().map(behaviors::compileCondition).toList();
            stages.add(new ReloadStage(
                    stage.id(),
                    secondsToTicks(stage.durationSeconds()),
                    stage.interruptible(),
                    conditions,
                    actions,
                    repeatWhile,
                    stage.maxIterations()
            ));
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return new ReloadSequence(stages);
    }

    private static int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.round(seconds * 20.0));
    }

    private static ReloadSequenceDefinition parse(Object raw) {
        if (raw instanceof ReloadSequenceDefinition sequence) return sequence;
        if (!(raw instanceof List<?> list)) {
            throw new ValidationException(List.of("reload.sequence must be a ReloadSequenceDefinition or list"));
        }

        var stages = new ArrayList<ReloadStageDefinition>(list.size());
        for (Object value : list) stages.add(parseStage(value));
        return new ReloadSequenceDefinition(stages);
    }

    @SuppressWarnings("unchecked")
    private static ReloadStageDefinition parseStage(Object raw) {
        if (raw instanceof ReloadStageDefinition stage) return stage;
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("reload stage must be an object");

        Object id = map.get("id");
        Object duration = map.get("duration");
        Object interruptible = map.get("interruptible");
        Object conditionsRaw = map.get("conditions");
        Object actionsRaw = map.get("actions");
        Object repeatRaw = map.get("repeatWhile");
        Object maxIterationsRaw = map.get("maxIterations");

        if (!(id instanceof String stageId)) throw new IllegalArgumentException("reload stage id must be a string");
        double seconds = duration instanceof Number number ? number.doubleValue() : 0.0;
        boolean canInterrupt = !(interruptible instanceof Boolean bool) || bool;
        int maxIterations = maxIterationsRaw instanceof Number number ? number.intValue() : 1;

        var conditions = parseConditions(conditionsRaw, "reload stage condition");
        var actions = parseActions(actionsRaw);
        var repeatWhile = parseConditions(repeatRaw, "reload repeat condition");
        return new ReloadStageDefinition(stageId, seconds, canInterrupt, conditions, actions, repeatWhile, maxIterations);
    }

    @SuppressWarnings("unchecked")
    private static List<ConditionConfig> parseConditions(Object raw, String label) {
        if (!(raw instanceof List<?> list)) return List.of();
        var conditions = new ArrayList<ConditionConfig>(list.size());
        for (Object condition : list) {
            if (condition instanceof ConditionConfig config) {
                conditions.add(config);
            } else if (condition instanceof Map<?, ?> conditionMap) {
                Object type = conditionMap.get("type");
                Object values = conditionMap.get("values");
                if (!(type instanceof String conditionType)) throw new IllegalArgumentException(label + " type must be a string");
                conditions.add(new ConditionConfig(conditionType,
                        values instanceof Map<?, ?> valueMap ? (Map<String, Object>) valueMap : Map.of()));
            } else {
                throw new IllegalArgumentException(label + " must be a ConditionConfig or object");
            }
        }
        return conditions;
    }

    @SuppressWarnings("unchecked")
    private static List<ActionConfig> parseActions(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        var actions = new ArrayList<ActionConfig>(list.size());
        for (Object action : list) {
            if (action instanceof ActionConfig config) {
                actions.add(config);
            } else if (action instanceof Map<?, ?> actionMap) {
                Object type = actionMap.get("type");
                Object values = actionMap.get("values");
                if (!(type instanceof String actionType)) throw new IllegalArgumentException("reload action type must be a string");
                actions.add(new ActionConfig(actionType,
                        values instanceof Map<?, ?> valueMap ? (Map<String, Object>) valueMap : Map.of()));
            } else {
                throw new IllegalArgumentException("reload action must be an ActionConfig or object");
            }
        }
        return actions;
    }
}
