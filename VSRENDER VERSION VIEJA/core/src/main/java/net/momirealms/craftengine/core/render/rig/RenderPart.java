package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable provider-independent description of one rig part/bone.
 * Runtime renderer objects must not leak into this contract.
 */
public final class RenderPart {
    private final String id;
    private final String parentId;
    private final List<String> children;
    private final RenderPartKind kind;
    private final RenderBonePose restPose;
    private final RenderPartRotationPolicy rotationPolicy;
    private final RenderHitBox hitBox;
    private final RenderVec3 anchorOffset;
    private final RenderNameTagMode nameTagMode;
    private final RenderMountPolicy mountPolicy;

    public RenderPart(String id, String parentId, List<String> children) {
        this(id, parentId, children, RenderPartKind.GROUP, RenderBonePose.IDENTITY, RenderPartRotationPolicy.NONE, null, RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind) {
        this(id, parentId, children, kind, RenderBonePose.IDENTITY, RenderPartRotationPolicy.NONE, null, RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind, RenderBonePose restPose) {
        this(id, parentId, children, kind, restPose, RenderPartRotationPolicy.NONE, null, RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind,
                      RenderBonePose restPose, RenderPartRotationPolicy rotationPolicy) {
        this(id, parentId, children, kind, restPose, rotationPolicy, null, RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind,
                      RenderBonePose restPose, RenderPartRotationPolicy rotationPolicy, RenderHitBox hitBox) {
        this(id, parentId, children, kind, restPose, rotationPolicy, hitBox, RenderVec3.ZERO, RenderNameTagMode.NONE, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind,
                      RenderBonePose restPose, RenderPartRotationPolicy rotationPolicy, RenderHitBox hitBox,
                      RenderVec3 anchorOffset, RenderNameTagMode nameTagMode) {
        this(id, parentId, children, kind, restPose, rotationPolicy, hitBox, anchorOffset, nameTagMode, RenderMountPolicy.DISABLED);
    }

    public RenderPart(String id, String parentId, List<String> children, RenderPartKind kind,
                      RenderBonePose restPose, RenderPartRotationPolicy rotationPolicy, RenderHitBox hitBox,
                      RenderVec3 anchorOffset, RenderNameTagMode nameTagMode, RenderMountPolicy mountPolicy) {
        this.id = requireId(id, "id");
        this.parentId = parentId == null ? null : requireId(parentId, "parentId");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.restPose = Objects.requireNonNull(restPose, "restPose");
        this.rotationPolicy = Objects.requireNonNull(rotationPolicy, "rotationPolicy");
        this.hitBox = hitBox;
        this.anchorOffset = Objects.requireNonNull(anchorOffset, "anchorOffset");
        this.nameTagMode = Objects.requireNonNull(nameTagMode, "nameTagMode");
        this.mountPolicy = Objects.requireNonNull(mountPolicy, "mountPolicy");
        if (this.id.equals(this.parentId)) {
            throw new IllegalArgumentException("part cannot be its own parent: " + id);
        }
        Objects.requireNonNull(children, "children");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String child : children) {
            String childId = requireId(child, "child");
            if (this.id.equals(childId)) {
                throw new IllegalArgumentException("part cannot contain itself as child: " + id);
            }
            if (!unique.add(childId)) {
                throw new IllegalArgumentException("duplicate child " + childId + " in part " + id);
            }
        }
        this.children = List.copyOf(unique);
    }

    public String id() {
        return id;
    }

    public String parentId() {
        return parentId;
    }

    public List<String> children() {
        return children;
    }

    public RenderPartKind kind() {
        return kind;
    }

    public RenderBonePose restPose() {
        return restPose;
    }

    public RenderPartRotationPolicy rotationPolicy() {
        return rotationPolicy;
    }

    public RenderHitBox hitBox() {
        return hitBox;
    }

    public boolean hasHitBox() {
        return hitBox != null;
    }

    /** Generic provider anchor point (BetterModel group hitBoxPoint). */
    public RenderVec3 anchorOffset() {
        return anchorOffset;
    }

    public RenderNameTagMode nameTagMode() {
        return nameTagMode;
    }

    public boolean hasNameTag() {
        return nameTagMode != RenderNameTagMode.NONE;
    }

    public RenderMountPolicy mountPolicy() {
        return mountPolicy;
    }

    public boolean mountable() {
        return mountPolicy.canMount();
    }

    public boolean attachmentPoint() {
        return kind == RenderPartKind.LOCATOR;
    }

    public boolean root() {
        return parentId == null;
    }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
