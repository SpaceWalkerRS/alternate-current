package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IWorld;
import alternate.current.wire.WireHandler;

import net.minecraft.world.World;

@Mixin(World.class)
public class WorldMixin implements IWorld {

	private final WireHandler wireHandler = new WireHandler((World)(Object)this);

	@Override
	public WireHandler getWireHandler() {
		return wireHandler;
	}
}
