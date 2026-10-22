package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.MoverType")
public interface MoverTypeProxy {
    MoverTypeProxy INSTANCE = ASMProxyFactory.create(MoverTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.entity.MoverType");
    Object SELF = INSTANCE.getSelf();

    @FieldGetter(name = "SELF", isStatic = true)
    Object getSelf();
}
