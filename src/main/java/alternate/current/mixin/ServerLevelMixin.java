package alternate.current.mixin;

import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.wire.WireHandler;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements IServerLevel {

	private final WireHandler alternateCurrent$wireHandler = new WireHandler((ServerLevel)(Object)this);

	@Unique
	@Override
	public WireHandler alternateCurrent$getWireHandler() {
		return this.alternateCurrent$wireHandler;
	}
}
