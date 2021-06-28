package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IWorld;

import net.minecraft.world.World;

@Mixin(World.class)
public class WorldMixin implements IWorld {
	
}
