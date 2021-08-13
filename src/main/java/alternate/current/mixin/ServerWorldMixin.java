package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IServerChunkManager;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements IWorld, IServerWorld {
	
	private final Map<WireBlock, WireHandler> wireHandlers = new HashMap<>();
	
	protected ServerWorldMixin(LevelProperties levelProperties, DimensionType dimensionType, BiFunction<World, Dimension, ChunkManager> chunkManagerProvider, Profiler profiler, boolean isClient) {
		super(levelProperties, dimensionType, chunkManagerProvider, profiler, isClient);
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		return ((IChunk)getChunk(pos)).getWire(wireBlock, pos);
	}
	
	@Override
	public void placeWire(WireNode wire) {
		((IChunk)getChunk(wire.pos)).placeWire(wire);
	}
	
	@Override
	public void removeWire(WireNode wire) {
		((IChunk)getChunk(wire.pos)).removeWire(wire);
	}
	
	@Override
	public void clearWires() {
		((IServerChunkManager)getChunkManager()).clearWires();
	}
	
	@Override
	public void updateWireConnections(BlockPos pos) {
		BlockState state = getBlockState(pos);
		Block block = state.getBlock();
		
		if (block instanceof WireBlock) {
			updateWireConnections((WireBlock)block, pos);
		}
	}
	
	@Override
	public void updateWireConnections(WireBlock wireBlock, BlockPos pos) {
		WireNode wire = getWire(wireBlock, pos);
		
		if (wire != null) {
			wire.updateConnections();
		}
	}
	
	@Override
	public WireHandler getWireHandler(WireBlock wireBlock) {
		WireHandler wireHandler = wireHandlers.get(wireBlock);
		
		if (wireHandler == null) {
			wireHandler = new WireHandler((ServerWorld)(Object)this, wireBlock);
			wireHandlers.put(wireBlock, wireHandler);
		}
		
		return wireHandler;
	}
}
