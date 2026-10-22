package net.momirealms.craftengine.core.render.rig;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Provider-independent selection of rig/model parts.
 * Providers translate child traversal semantics to their own bone/part APIs.
 */
public final class RenderPartSelector {
    public enum Mode { ALL, NONE, NAMED }

    private static final RenderPartSelector ALL = new RenderPartSelector(Mode.ALL, Set.of(), true);
    private static final RenderPartSelector NONE = new RenderPartSelector(Mode.NONE, Set.of(), false);

    private final Mode mode;
    private final Set<String> names;
    private final boolean includeChildren;

    private RenderPartSelector(Mode mode, Set<String> names, boolean includeChildren) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.names = Set.copyOf(names);
        this.includeChildren = includeChildren;
    }

    public static RenderPartSelector all() {
        return ALL;
    }

    public static RenderPartSelector none() {
        return NONE;
    }

    public static RenderPartSelector named(Collection<String> names, boolean includeChildren) {
        Objects.requireNonNull(names, "names");
        var copy = new LinkedHashSet<String>();
        for (String name : names) copy.add(Objects.requireNonNull(name, "part name"));
        if (copy.isEmpty()) return NONE;
        return new RenderPartSelector(Mode.NAMED, copy, includeChildren);
    }

    public Mode mode() {
        return mode;
    }

    public Set<String> names() {
        return names;
    }

    public boolean includeChildren() {
        return includeChildren;
    }
}
