package alternate.current.MAX_PERFORMANCE.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IWorld;
import net.minecraft.world.World;

@Mixin(World.class)
public class WorldMixin implements IWorld {
	
}
