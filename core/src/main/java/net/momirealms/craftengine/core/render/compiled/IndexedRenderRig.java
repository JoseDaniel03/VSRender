package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.rig.RenderHitBox;
import net.momirealms.craftengine.core.render.rig.RenderNameTagMode;
import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;
import net.momirealms.craftengine.core.render.rig.RenderPart;
import net.momirealms.craftengine.core.render.rig.RenderPartKind;
import net.momirealms.craftengine.core.render.rig.RenderPartRotationPolicy;
import net.momirealms.craftengine.core.render.rig.RenderQuat;
import net.momirealms.craftengine.core.render.rig.RenderRig;
import net.momirealms.craftengine.core.render.rig.RenderRigType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dense, parent-before-child runtime form of {@link RenderRig}.
 * String lookups exist only at the boundary; the hot path uses integer part indices.
 */
public final class IndexedRenderRig {
    private final String providerId;
    private final String rigId;
    private final RenderRigType type;
    private final String[] partIds;
    private final RenderPartKind[] kinds;
    private final int[] parentIndices;
    private final Map<String, Integer> indexById;
    private final int[] attachmentIndices;
    private final int[] hitBoxIndices;
    private final int[] nameTagIndices;
    private final int[] mountIndices;
    private final boolean[] hasHitBox;
    private final float[] anchorX, anchorY, anchorZ;
    private final RenderNameTagMode[] nameTagModes;
    private final RenderMountPolicy[] mountPolicies;
    private final float[] hitBoxCenterX, hitBoxCenterY, hitBoxCenterZ;
    private final float[] hitBoxMinX, hitBoxMinY, hitBoxMinZ;
    private final float[] hitBoxMaxX, hitBoxMaxY, hitBoxMaxZ;
    private final boolean[] ownerHeadRotation;
    private final boolean hasOwnerHeadRotation;

    private final float[] restTx;
    private final float[] restTy;
    private final float[] restTz;
    private final float[] restSx;
    private final float[] restSy;
    private final float[] restSz;
    private final float[] restRx;
    private final float[] restRy;
    private final float[] restRz;
    private final float[] restQx;
    private final float[] restQy;
    private final float[] restQz;
    private final float[] restQw;

    private IndexedRenderRig(RenderRig rig, List<RenderPart> ordered) {
        providerId = rig.providerId();
        rigId = rig.rigId();
        type = rig.type();
        int size = ordered.size();
        partIds = new String[size];
        kinds = new RenderPartKind[size];
        parentIndices = new int[size];
        hasHitBox = new boolean[size];
        anchorX = new float[size]; anchorY = new float[size]; anchorZ = new float[size];
        nameTagModes = new RenderNameTagMode[size];
        mountPolicies = new RenderMountPolicy[size];
        hitBoxCenterX = new float[size]; hitBoxCenterY = new float[size]; hitBoxCenterZ = new float[size];
        hitBoxMinX = new float[size]; hitBoxMinY = new float[size]; hitBoxMinZ = new float[size];
        hitBoxMaxX = new float[size]; hitBoxMaxY = new float[size]; hitBoxMaxZ = new float[size];
        ownerHeadRotation = new boolean[size];
        restTx = new float[size];
        restTy = new float[size];
        restTz = new float[size];
        restSx = new float[size];
        restSy = new float[size];
        restSz = new float[size];
        restRx = new float[size];
        restRy = new float[size];
        restRz = new float[size];
        restQx = new float[size];
        restQy = new float[size];
        restQz = new float[size];
        restQw = new float[size];

        LinkedHashMap<String, Integer> indices = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String id = ordered.get(i).id();
            partIds[i] = id;
            indices.put(id, i);
        }

        ArrayList<Integer> attachments = new ArrayList<>();
        ArrayList<Integer> hitBoxes = new ArrayList<>();
        ArrayList<Integer> nameTags = new ArrayList<>();
        ArrayList<Integer> mounts = new ArrayList<>();
        boolean anyOwnerHeadRotation = false;
        boolean[] inheritedOwnerHeadRotation = new boolean[size];
        for (int i = 0; i < size; i++) {
            RenderPart part = ordered.get(i);
            kinds[i] = part.kind();
            parentIndices[i] = part.parentId() == null ? -1 : requireParentIndex(indices, part, i);
            if (part.attachmentPoint()) attachments.add(i);
            anchorX[i] = part.anchorOffset().x();
            anchorY[i] = part.anchorOffset().y();
            anchorZ[i] = part.anchorOffset().z();
            nameTagModes[i] = part.nameTagMode();
            mountPolicies[i] = part.mountPolicy();
            if (part.hasNameTag()) nameTags.add(i);
            if (part.mountable()) mounts.add(i);
            RenderHitBox hitBox = part.hitBox();
            if (hitBox != null) {
                hasHitBox[i] = true;
                hitBoxes.add(i);
                hitBoxCenterX[i] = hitBox.centerOffset().x();
                hitBoxCenterY[i] = hitBox.centerOffset().y();
                hitBoxCenterZ[i] = hitBox.centerOffset().z();
                hitBoxMinX[i] = hitBox.minX();
                hitBoxMinY[i] = hitBox.minY();
                hitBoxMinZ[i] = hitBox.minZ();
                hitBoxMaxX[i] = hitBox.maxX();
                hitBoxMaxY[i] = hitBox.maxY();
                hitBoxMaxZ[i] = hitBox.maxZ();
            }

            int parent = parentIndices[i];
            RenderPartRotationPolicy rotationPolicy = part.rotationPolicy();
            boolean inherited = parent >= 0 && inheritedOwnerHeadRotation[parent];
            ownerHeadRotation[i] = inherited || rotationPolicy.ownerHead();
            inheritedOwnerHeadRotation[i] = inherited || rotationPolicy.includeDescendants();
            anyOwnerHeadRotation |= ownerHeadRotation[i];

            RenderBonePose rest = part.restPose();
            restTx[i] = rest.translation().x();
            restTy[i] = rest.translation().y();
            restTz[i] = rest.translation().z();
            restSx[i] = rest.scale().x();
            restSy[i] = rest.scale().y();
            restSz[i] = rest.scale().z();
            restRx[i] = rest.rotation().x();
            restRy[i] = rest.rotation().y();
            restRz[i] = rest.rotation().z();
            RenderQuat q = RenderQuat.fromEulerZYXDegrees(rest.rotation());
            restQx[i] = q.x();
            restQy[i] = q.y();
            restQz[i] = q.z();
            restQw[i] = q.w();
        }
        indexById = Collections.unmodifiableMap(indices);
        attachmentIndices = attachments.stream().mapToInt(Integer::intValue).toArray();
        hitBoxIndices = hitBoxes.stream().mapToInt(Integer::intValue).toArray();
        nameTagIndices = nameTags.stream().mapToInt(Integer::intValue).toArray();
        mountIndices = mounts.stream().mapToInt(Integer::intValue).toArray();
        hasOwnerHeadRotation = anyOwnerHeadRotation;
    }

    public static IndexedRenderRig compile(RenderRig rig) {
        Objects.requireNonNull(rig, "rig");
        ArrayList<RenderPart> ordered = new ArrayList<>(rig.parts().size());
        for (RenderPart root : rig.roots()) appendDepthFirst(rig, root, ordered);
        if (ordered.size() != rig.parts().size()) {
            throw new IllegalArgumentException("rig topology did not produce every part");
        }
        return new IndexedRenderRig(rig, ordered);
    }

    private static void appendDepthFirst(RenderRig rig, RenderPart part, List<RenderPart> ordered) {
        ordered.add(part);
        for (String childId : part.children()) {
            appendDepthFirst(rig, rig.part(childId).orElseThrow(), ordered);
        }
    }

    private static int requireParentIndex(Map<String, Integer> indices, RenderPart part, int childIndex) {
        Integer parentIndex = indices.get(part.parentId());
        if (parentIndex == null || parentIndex >= childIndex) {
            throw new IllegalArgumentException("compiled rig must be parent-before-child: " + part.parentId() + " -> " + part.id());
        }
        return parentIndex;
    }

    public String providerId() { return providerId; }
    public String rigId() { return rigId; }
    public RenderRigType type() { return type; }
    public int partCount() { return partIds.length; }
    public String partId(int index) { checkIndex(index); return partIds[index]; }
    public RenderPartKind kind(int index) { checkIndex(index); return kinds[index]; }
    public int parentIndex(int index) { checkIndex(index); return parentIndices[index]; }
    public int indexOf(String partId) { return indexById.getOrDefault(partId, -1); }
    public Map<String, Integer> indexById() { return indexById; }
    public int[] attachmentIndices() { return attachmentIndices.clone(); }
    public int[] hitBoxIndices() { return hitBoxIndices.clone(); }
    public int[] nameTagIndices() { return nameTagIndices.clone(); }
    public int[] mountIndices() { return mountIndices.clone(); }
    public float anchorX(int index) { checkIndex(index); return anchorX[index]; }
    public float anchorY(int index) { checkIndex(index); return anchorY[index]; }
    public float anchorZ(int index) { checkIndex(index); return anchorZ[index]; }
    public RenderNameTagMode nameTagMode(int index) { checkIndex(index); return nameTagModes[index]; }
    public RenderMountPolicy mountPolicy(int index) { checkIndex(index); return mountPolicies[index]; }
    public boolean hasHitBox(int index) { checkIndex(index); return hasHitBox[index]; }
    public float hitBoxCenterX(int index) { checkHitBox(index); return hitBoxCenterX[index]; }
    public float hitBoxCenterY(int index) { checkHitBox(index); return hitBoxCenterY[index]; }
    public float hitBoxCenterZ(int index) { checkHitBox(index); return hitBoxCenterZ[index]; }
    public float hitBoxMinX(int index) { checkHitBox(index); return hitBoxMinX[index]; }
    public float hitBoxMinY(int index) { checkHitBox(index); return hitBoxMinY[index]; }
    public float hitBoxMinZ(int index) { checkHitBox(index); return hitBoxMinZ[index]; }
    public float hitBoxMaxX(int index) { checkHitBox(index); return hitBoxMaxX[index]; }
    public float hitBoxMaxY(int index) { checkHitBox(index); return hitBoxMaxY[index]; }
    public float hitBoxMaxZ(int index) { checkHitBox(index); return hitBoxMaxZ[index]; }
    public boolean ownerHeadRotation(int index) { checkIndex(index); return ownerHeadRotation[index]; }
    public boolean hasOwnerHeadRotation() { return hasOwnerHeadRotation; }

    float restTx(int i) { return restTx[i]; }
    float restTy(int i) { return restTy[i]; }
    float restTz(int i) { return restTz[i]; }
    float restSx(int i) { return restSx[i]; }
    float restSy(int i) { return restSy[i]; }
    float restSz(int i) { return restSz[i]; }
    float restRx(int i) { return restRx[i]; }
    float restRy(int i) { return restRy[i]; }
    float restRz(int i) { return restRz[i]; }
    float restQx(int i) { return restQx[i]; }
    float restQy(int i) { return restQy[i]; }
    float restQz(int i) { return restQz[i]; }
    float restQw(int i) { return restQw[i]; }

    private void checkHitBox(int index) {
        checkIndex(index);
        if (!hasHitBox[index]) throw new IllegalStateException("part has no hitbox: " + partIds[index]);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= partIds.length) throw new IndexOutOfBoundsException(index);
    }
}
