package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.interfaces.mixin.IChunk;
import alternate.current.redstone.interfaces.mixin.IServerWorld;
import alternate.current.redstone.interfaces.mixin.IWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements WorldView, IWorld, IServerWorld {
	
	private final Map<WireBlock, WireHandler> wireHandlers = new HashMap<>();
	
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
