package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld, WorldAccess {
	
	private int blockUpdateCount;
	
	@Shadow public abstract boolean isDebugWorld();
	
	@Inject(
			method = "updateNeighbor",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/BlockState;neighborUpdate(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Z)V"
			)
	)
	private void onUpdateNeighborInjectAtNeighborUpdate(BlockPos pos, Block fromBlock, BlockPos fromPos, CallbackInfo ci) {
		if (!isClient()) {
			blockUpdateCount++;
		}
	}
	
	@Override
	public void reset() {
		blockUpdateCount = 0;
	}
	
	@Override
	public int getCount() {
		return blockUpdateCount;
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		return getWire(wireBlock, pos, true);
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate) {
		if (isClient() || isDebugWorld()) {
			return null;
		}
		
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		
		return ((IChunk)getChunk(chunkX, chunkZ)).getWire(wireBlock, pos, orCreate);
	}
	
	@Override
	public void placeWire(WireNode wire) {
		setWire(wire.wireBlock, wire.pos, wire);
	}
	
	@Override
	public void removeWire(WireNode wire) {
		setWire(wire.wireBlock, wire.pos, null);
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
	
	private void setWire(WireBlock wireBlock, BlockPos pos, WireNode wire) {
		if (isClient() || isDebugWorld()) {
			return;
		}
		
		int x = pos.getX() >> 4;
		int z = pos.getZ() >> 4;
		
		WireNode prevWire = ((IChunk)getChunk(x, z)).setWire(wireBlock, pos, wire);
		
		if (prevWire != null) {
			prevWire.updateNeighboringWires();
		}
		if (wire != null) {
			wire.updateConnections();
		}
	}
}
