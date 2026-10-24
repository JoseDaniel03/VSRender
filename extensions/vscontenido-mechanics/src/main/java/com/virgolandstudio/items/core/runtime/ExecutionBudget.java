package com.virgolandstudio.items.core.runtime;

public final class ExecutionBudget {
    private final int maxOperations;
    private final int maxDepth;
    private int operations;
    private int depth;

    public ExecutionBudget(int maxOperations, int maxDepth) {
        if (maxOperations < 1 || maxDepth < 1) throw new IllegalArgumentException("Budget limits must be positive");
        this.maxOperations = maxOperations;
        this.maxDepth = maxDepth;
    }

    public void consumeOperation() {
        if (++operations > maxOperations) {
            throw new BudgetExceededException("Operation budget exceeded: " + maxOperations);
        }
    }

    public Scope enterNestedTrigger() {
        if (++depth > maxDepth) {
            depth--;
            throw new BudgetExceededException("Trigger depth exceeded: " + maxDepth);
        }
        return new Scope(this);
    }

    public int operations() { return operations; }
    public int depth() { return depth; }

    private void leave() { depth--; }

    public static final class Scope implements AutoCloseable {
        private ExecutionBudget owner;
        private Scope(ExecutionBudget owner) { this.owner = owner; }
        @Override public void close() {
            if (owner != null) {
                owner.leave();
                owner = null;
            }
        }
    }
}
