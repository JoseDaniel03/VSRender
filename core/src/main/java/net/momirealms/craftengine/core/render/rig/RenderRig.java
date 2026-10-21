package net.momirealms.craftengine.core.render.rig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable snapshot of a render rig's logical hierarchy.
 * It owns identity and topology only; providers own the physical renderer.
 */
public final class RenderRig {
    private final String providerId;
    private final String rigId;
    private final RenderRigType type;
    private final Map<String, RenderPart> parts;
    private final List<RenderPart> roots;
    private final Set<String> animations;

    public RenderRig(String providerId,
                     String rigId,
                     RenderRigType type,
                     Collection<RenderPart> parts,
                     Collection<String> animations) {
        this.providerId = requireId(providerId, "providerId");
        this.rigId = requireId(rigId, "rigId");
        this.type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(animations, "animations");

        LinkedHashMap<String, RenderPart> partMap = new LinkedHashMap<>();
        for (RenderPart part : parts) {
            Objects.requireNonNull(part, "part");
            if (partMap.putIfAbsent(part.id(), part) != null) {
                throw new IllegalArgumentException("duplicate rig part: " + part.id());
            }
        }
        validateHierarchy(partMap);
        this.parts = Collections.unmodifiableMap(partMap);
        this.roots = partMap.values().stream().filter(RenderPart::root).toList();

        LinkedHashSet<String> animationSet = new LinkedHashSet<>();
        for (String animation : animations) {
            animationSet.add(requireId(animation, "animation"));
        }
        this.animations = Collections.unmodifiableSet(animationSet);
    }

    public String providerId() {
        return providerId;
    }

    public String rigId() {
        return rigId;
    }

    public RenderRigType type() {
        return type;
    }

    public Collection<RenderPart> parts() {
        return parts.values();
    }

    public List<RenderPart> roots() {
        return roots;
    }

    public Optional<RenderPart> part(String id) {
        return Optional.ofNullable(parts.get(id));
    }

    public Set<String> animations() {
        return animations;
    }

    /** Locator parts exposed as provider-independent named attachment points. */
    public List<RenderPart> attachmentPoints() {
        return parts.values().stream().filter(RenderPart::attachmentPoint).toList();
    }

    public boolean hasAnimation(String animationId) {
        return animations.contains(animationId);
    }

    private static void validateHierarchy(Map<String, RenderPart> parts) {
        for (RenderPart part : parts.values()) {
            if (part.parentId() != null) {
                RenderPart parent = parts.get(part.parentId());
                if (parent == null) {
                    throw new IllegalArgumentException("missing parent " + part.parentId() + " for part " + part.id());
                }
                if (!parent.children().contains(part.id())) {
                    throw new IllegalArgumentException("parent/child mismatch: " + part.parentId() + " -> " + part.id());
                }
            }
            for (String childId : part.children()) {
                RenderPart child = parts.get(childId);
                if (child == null) {
                    throw new IllegalArgumentException("missing child " + childId + " for part " + part.id());
                }
                if (!part.id().equals(child.parentId())) {
                    throw new IllegalArgumentException("parent/child mismatch: " + part.id() + " -> " + childId);
                }
            }
        }

        // Parent-chain walk also rejects cycles that are not visible from roots.
        for (RenderPart start : parts.values()) {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            RenderPart cursor = start;
            while (cursor != null) {
                if (!seen.add(cursor.id())) {
                    throw new IllegalArgumentException("cycle in rig hierarchy at part " + cursor.id());
                }
                cursor = cursor.parentId() == null ? null : parts.get(cursor.parentId());
            }
        }
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
