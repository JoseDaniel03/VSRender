package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityAttachments")
public interface EntityAttachmentsProxy {
    EntityAttachmentsProxy INSTANCE = ASMProxyFactory.create(EntityAttachmentsProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.EntityAttachments");

    @MethodInvoker(name = "get")
    Object get(Object target,
               @Type(clazz = EntityAttachmentProxy.class) Object attachment,
               int index,
               float yaw);
}
