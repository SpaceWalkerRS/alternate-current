package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.wire.WireHandler;

import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements IServerLevel {

	private final WireHandler wireHandler = new WireHandler((ServerLevel)(Object)this);

	@Override
	public WireHandler getWireHandler() {
		return wireHandler;
	}
}
