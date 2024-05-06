package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ILogger;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldStorage;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements IServerWorld {

	private WireHandler wireHandler;

	@Inject(
		method = "<init>",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$parseConfig(MinecraftServer server, WorldStorage storage, String name, int dimension, WorldSettings settings, Profiler profiler, ILogger logger, CallbackInfo ci) {
		this.wireHandler = new WireHandler((ServerWorld)(Object)this, storage);
	}

	@Override
	public WireHandler alternate_current$getWireHandler() {
		return wireHandler;
	}
}
