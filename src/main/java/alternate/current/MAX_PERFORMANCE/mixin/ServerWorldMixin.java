package alternate.current.MAX_PERFORMANCE.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.MAX_PERFORMANCE.WireBlock;
import alternate.current.MAX_PERFORMANCE.WireHandler;
import alternate.current.MAX_PERFORMANCE.WireNode;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IChunk;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IServerChunkManager;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IServerWorld;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements WorldAccess, IWorld, IServerWorld {
	
	private final Map<WireBlock, WireHandler> wireHandlers = new HashMap<>();
	
	@Shadow @Final private ServerChunkManager chunkManager;
	
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
		((IServerChunkManager)chunkManager).clearWires();
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
			wire.connections.update();
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
