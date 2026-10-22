package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderAnimationSample;
import net.momirealms.craftengine.core.render.animation.RenderBonePose;
import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves local rest + animation poses through a rig hierarchy into instance-space poses. */
public final class RenderRigPoseResolver {
    private RenderRigPoseResolver() {
    }

    public static Map<String, RenderResolvedPose> resolve(RenderRig rig, Map<String, RenderAnimationSample> animationSamples) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(animationSamples, "animationSamples");
        LinkedHashMap<String, RenderResolvedPose> resolved = new LinkedHashMap<>();
        for (RenderPart root : rig.roots()) resolvePart(rig, root, null, animationSamples, resolved);
        return Collections.unmodifiableMap(resolved);
    }

    private static void resolvePart(RenderRig rig,
                                    RenderPart part,
                                    RenderResolvedPose parent,
                                    Map<String, RenderAnimationSample> samples,
                                    LinkedHashMap<String, RenderResolvedPose> resolved) {
        RenderAnimationSample sample = samples.get(part.id());
        RenderBonePose animation = sample == null ? RenderBonePose.IDENTITY : sample.pose();
        RenderBonePose rest = part.restPose();

        RenderVec3 localTranslation = rest.translation().add(animation.translation());
        RenderVec3 localScale = rest.scale().multiply(animation.scale());
        RenderVec3 localEuler = rest.rotation().add(animation.rotation());
        RenderQuat localRotation = RenderQuat.fromEulerZYXDegrees(localEuler);

        RenderResolvedPose pose;
        if (parent == null) {
            pose = new RenderResolvedPose(localTranslation, localScale, localRotation);
        } else {
            RenderVec3 rotatedTranslation = parent.rotation().rotate(localTranslation);
            RenderVec3 worldTranslation = parent.translation().add(rotatedTranslation.multiply(parent.scale()));
            RenderVec3 worldScale = parent.scale().multiply(localScale);
            boolean globalRotation = sample != null && sample.globalRotation();
            RenderQuat worldRotation = globalRotation ? localRotation : parent.rotation().multiply(localRotation).normalized();
            pose = new RenderResolvedPose(worldTranslation, worldScale, worldRotation);
        }
        resolved.put(part.id(), pose);
        for (String childId : part.children()) {
            RenderPart child = rig.part(childId).orElseThrow();
            resolvePart(rig, child, pose, samples, resolved);
        }
    }
}
