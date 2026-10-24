package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.runtime.ExecutionBudget;
import com.virgolandstudio.items.core.runtime.ExecutionContext;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class BehaviorEngine {
    private static final Map<Class<?>, Object> GLOBAL_CONTEXT_SERVICES = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> contextServices = new ConcurrentHashMap<>();

    /** Registers a service for one engine instance. */
    public <T> BehaviorEngine contextService(Class<T> type, T service) {
        contextServices.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(service, "service"));
        return this;
    }

    /**
     * Registers a process-local Items runtime service for engines created by platform coordinators.
     * This is intended for singleton platform bridges installed during plugin bootstrap.
     */
    public static <T> void globalContextService(Class<T> type, T service) {
        GLOBAL_CONTEXT_SERVICES.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(service, "service"));
    }

    /** Removes a global service only when it is still the expected instance. */
    public static <T> void clearGlobalContextService(Class<T> type, T expectedService) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(expectedService, "expectedService");
        GLOBAL_CONTEXT_SERVICES.remove(type, expectedService);
    }

    public ExecutionContext execute(CompiledItemDefinition item, Trigger trigger, ExecutionBudget budget) {
        var context = new ExecutionContext(item, trigger, budget);
        execute(context);
        return context;
    }

    public ExecutionContext execute(ItemInstance instance, Trigger trigger, ExecutionBudget budget) {
        var context = new ExecutionContext(instance.definition(), instance, trigger, budget);
        execute(context);
        return context;
    }

    public ActionResult execute(ExecutionContext context) {
        prepare(context);
        ItemInstance instance = context.instance().orElse(null);
        if (instance == null) return executeLocked(context);
        synchronized (instance) {
            return executeLocked(context);
        }
    }

    @SuppressWarnings("try")
    private ActionResult executeLocked(ExecutionContext context) {
        var list = context.item().behaviors().get(context.trigger());
        if (list == null || list.isEmpty()) return ActionResult.CONTINUE;

        try (var ignored = context.budget().enterNestedTrigger()) {
            for (CompiledBehavior behavior : list) {
                if (!testConditionsLocked(context, behavior.conditions())) continue;
                ActionResult result = executeActionsLocked(context, behavior.actions());
                if (result != ActionResult.CONTINUE) return result;
            }
            return ActionResult.CONTINUE;
        }
    }

    public boolean testConditions(ExecutionContext context, java.util.List<CompiledCondition> conditions) {
        prepare(context);
        ItemInstance instance = context.instance().orElse(null);
        if (instance == null) return testConditionsLocked(context, conditions);
        synchronized (instance) {
            return testConditionsLocked(context, conditions);
        }
    }

    private boolean testConditionsLocked(ExecutionContext context, java.util.List<CompiledCondition> conditions) {
        for (CompiledCondition condition : conditions) {
            context.budget().consumeOperation();
            if (!condition.test(context)) return false;
        }
        return true;
    }

    public ActionResult executeActions(ExecutionContext context, java.util.List<CompiledAction> actions) {
        prepare(context);
        ItemInstance instance = context.instance().orElse(null);
        if (instance == null) return executeActionsLocked(context, actions);
        synchronized (instance) {
            return executeActionsLocked(context, actions);
        }
    }

    private ActionResult executeActionsLocked(ExecutionContext context, java.util.List<CompiledAction> actions) {
        for (CompiledAction action : actions) {
            context.budget().consumeOperation();
            ActionResult result = action.execute(context);
            if (result != ActionResult.CONTINUE) return result;
        }
        return ActionResult.CONTINUE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void prepare(ExecutionContext context) {
        for (Map.Entry<Class<?>, Object> entry : GLOBAL_CONTEXT_SERVICES.entrySet()) {
            context.service((Class) entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, Object> entry : contextServices.entrySet()) {
            context.service((Class) entry.getKey(), entry.getValue());
        }
    }
}
