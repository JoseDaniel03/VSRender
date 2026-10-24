package com.virgolandstudio.items.core.runtime;

@SuppressWarnings("serial")
public final class BudgetExceededException extends RuntimeException {
    public BudgetExceededException(String message) {
        super(message);
    }
}
