package com.virgolandstudio.items.core.compile;

import java.util.List;

@SuppressWarnings("serial")
public final class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
