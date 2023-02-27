package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.wire.WireHandler;

import net.minecraft.server.world.ServerWorld;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements IServerWorld {

	private final WireHandler wireHandler = new WireHandler((ServerWorld)(Object)this);

	@Override
	public WireHandler getWireHandler() {
		return wireHandler;
	}
}
