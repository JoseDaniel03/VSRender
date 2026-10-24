package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ActionConfig;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.behavior.ConditionConfig;
import com.virgolandstudio.items.api.behavior.ValueConfig;
import com.virgolandstudio.items.core.compile.ValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BehaviorCompiler {
    private final Map<String, ValueProviderType> valueProviders = new LinkedHashMap<>();
    private final Map<String, ConditionType> conditions = new LinkedHashMap<>();
    private final Map<String, ActionType> actions = new LinkedHashMap<>();

    public void registerValue(String id, ValueProviderType type) { valueProviders.put(id, type); }
    public void registerCondition(String id, ConditionType type) { conditions.put(id, type); }
    public void registerAction(String id, ActionType type) { actions.put(id, type); }

    public CompiledValue compileValue(ValueConfig config) {
        var type = valueProviders.get(config.type());
        if (type == null) throw new ValidationException(List.of("Unknown value provider: " + config.type()));
        return type.compile(config);
    }

    public CompiledCondition compileCondition(ConditionConfig config) {
        var type = conditions.get(config.type());
        if (type == null) throw new ValidationException(List.of("Unknown condition: " + config.type()));
        return type.compile(config, this);
    }

    public CompiledAction compileAction(ActionConfig config) {
        var type = actions.get(config.type());
        if (type == null) throw new ValidationException(List.of("Unknown action: " + config.type()));
        return type.compile(config, this);
    }

    public CompiledBehavior compile(BehaviorDefinition definition) {
        var compiledConditions = new ArrayList<CompiledCondition>(definition.conditions().size());
        for (var condition : definition.conditions()) compiledConditions.add(compileCondition(condition));
        var compiledActions = new ArrayList<CompiledAction>(definition.actions().size());
        for (var action : definition.actions()) compiledActions.add(compileAction(action));
        return new CompiledBehavior(definition.trigger(), compiledConditions, compiledActions);
    }

    public Map<String, ValueProviderType> valueTypes() { return Map.copyOf(valueProviders); }
    public Map<String, ConditionType> conditionTypes() { return Map.copyOf(conditions); }
    public Map<String, ActionType> actionTypes() { return Map.copyOf(actions); }
}
