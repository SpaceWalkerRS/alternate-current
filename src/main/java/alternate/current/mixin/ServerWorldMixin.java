package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements IServerWorld {
	
	private final Map<WireBlock, WorldAccess> access = new HashMap<>();
	
	protected ServerWorldMixin(LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> chunkManagerProvider, Profiler profiler, boolean isClient) {
		super(levelProperties, dimensionType, chunkManagerProvider, profiler, isClient);
	}
	
	@Override
	public WorldAccess getAccess(WireBlock wireBlock) {
		WorldAccess world = access.get(wireBlock);
		
		if (world == null) {
			world = new WorldAccess(wireBlock, (ServerWorld)(Object)this);
			access.put(wireBlock, world);
		}
		
		return world;
	}
	
	@Override
	public void updateWireConnectionsAround(BlockPos pos) {
		for (Direction dir : WireHandler.Directions.ALL) {
			BlockPos side = pos.offset(dir);
			WorldChunk chunk = getWorldChunk(side);
			WireNode wire = ((IChunk)chunk).getWireNode(side);
			
			if (wire != null) {
				wire.connections.update();
			}
		}
	}
}
