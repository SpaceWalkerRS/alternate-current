package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WorldAccess;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.SaveHandler;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.level.LevelProperties;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements IServerWorld {
	
	private final Map<WireBlock, WorldAccess> access = new HashMap<>();
	
	protected ServerWorldMixin(SaveHandler handler, LevelProperties properties, Dimension dim, Profiler profiler, boolean isClient) {
		super(handler, properties, dim, profiler, isClient);
	}
	
	@Override
	public WorldAccess getAccess(WireBlock wireBlock) {
		return access.computeIfAbsent(wireBlock, key -> new WorldAccess(wireBlock, (ServerWorld)(Object)this));
	}
}
