package alternate.current.mixin;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerChunkManager;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.redstone.WireHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements IServerWorld {
	
	private WireHandler wireHandler;
	
	@Shadow @Final private ServerChunkManager serverChunkManager;
	
	@Inject(
			method = "<init>",
			at = @At(
					value = "RETURN"
			)
	)
	private void onInitInjectAtReturn(MinecraftServer server, Executor executor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> key, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean isDebugWorld, long l, List<Spawner> spawners, boolean shouldTickTime, CallbackInfo ci) {
		this.wireHandler = new WireHandler((ServerWorld)(Object)this);
	}
	
	@Inject(method = "tick", at = @At(value = "HEAD"))
	private void tickstart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		((IWorld)this).reset();
	}
	
	@Inject(method = "tick", at = @At(value = "RETURN"))
	private void tickend(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		int count = ((IWorld)this).getCount();
		
		if (count > 0) {
			AlternateCurrentMod.LOGGER.info(count);
		}
	}
	
	@Override
	public WireHandler getWireHandler() {
		return wireHandler;
	}
	
	@Override
	public void clearWires() {
		((IServerChunkManager)serverChunkManager).clearWires();
	}
}
