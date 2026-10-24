package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;
import com.virgolandstudio.items.core.instance.ItemInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class ExecutionContext {
    private final CompiledItemDefinition item;
    private final ItemInstance instance;
    private final Trigger trigger;
    private final ExecutionBudget budget;
    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, Object> lazy = new HashMap<>();
    private final Map<Class<?>, Object> services = new HashMap<>();

    public ExecutionContext(CompiledItemDefinition item, Trigger trigger, ExecutionBudget budget) {
        this(item, null, trigger, budget);
    }

    public ExecutionContext(CompiledItemDefinition item, ItemInstance instance, Trigger trigger, ExecutionBudget budget) {
        this.item = Objects.requireNonNull(item, "item");
        if (instance != null && instance.definition() != item) {
            throw new IllegalArgumentException("ItemInstance definition does not match execution definition");
        }
        this.instance = instance;
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public CompiledItemDefinition item() { return item; }
    public Optional<ItemInstance> instance() { return Optional.ofNullable(instance); }
    public ItemInstance requireInstance() {
        if (instance == null) throw new IllegalStateException("This behavior requires an ItemInstance");
        return instance;
    }
    public Trigger trigger() { return trigger; }
    public ExecutionBudget budget() { return budget; }

    public void put(String key, Object value) { variables.put(key, value); }
    public Object get(String key) { return variables.get(key); }

    public <T> ExecutionContext service(Class<T> type, T service) {
        services.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(service, "service"));
        return this;
    }

    public <T> Optional<T> service(Class<T> type) {
        Object service = services.get(type);
        return service == null ? Optional.empty() : Optional.of(type.cast(service));
    }

    public <T> T requireService(Class<T> type) {
        return service(type).orElseThrow(() -> new IllegalStateException("Missing execution service: " + type.getName()));
    }

    @SuppressWarnings("unchecked")
    public <T> T lazy(String key, Supplier<T> supplier) {
        if (lazy.containsKey(key)) return (T) lazy.get(key);
        T value = supplier.get();
        lazy.put(key, value);
        return value;
    }
}
