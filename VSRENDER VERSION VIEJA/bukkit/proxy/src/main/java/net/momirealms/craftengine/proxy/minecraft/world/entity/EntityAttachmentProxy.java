package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityAttachment")
public interface EntityAttachmentProxy {
    EntityAttachmentProxy INSTANCE = ASMProxyFactory.create(EntityAttachmentProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.EntityAttachment");
    Object PASSENGER = INSTANCE.getPassenger();

    @FieldGetter(name = "PASSENGER", isStatic = true)
    Object getPassenger();
}
