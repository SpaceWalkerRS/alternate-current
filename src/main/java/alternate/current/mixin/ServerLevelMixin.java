package alternate.current.mixin;

import java.util.List;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements IServerLevel {

	private WireHandler wireHandler;

	@Inject(
		method = "<init>",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$parseConfig(MinecraftServer server, Executor executor, LevelStorage storage, LevelData data, DimensionType dimension, ProfilerFiller profiler, ChunkProgressListener listener, CallbackInfo ci) {
		this.wireHandler = new WireHandler((ServerLevel)(Object)this, storage);
	}

	@Override
	public WireHandler alternate_current$getWireHandler() {
		return wireHandler;
	}
}
