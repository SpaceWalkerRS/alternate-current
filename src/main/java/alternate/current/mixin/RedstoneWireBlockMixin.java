package alternate.current.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	@Inject(
			method = "onBlockAdded",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onOnBlockAddedInjectAtHead(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
		if (AlternateCurrentMod.ENABLED) {
			ci.cancel(); // replaced by WireBlock.onWireAdded
		}
	}
	
	@Inject(
			method = "onStateReplaced",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onOnStateReplacedInjectAtHead(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		if (AlternateCurrentMod.ENABLED) {
			ci.cancel(); // replaced by WireBlock.onWireRemoved
		}
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
			)
	)
	private void onNeighborUpdateInjectBeforeUpdate(BlockState state, World world, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		if (AlternateCurrentMod.ENABLED) {
			WireNode wire = getWire(world, pos);
			
			if (wire != null) {
				updatePower(wire);
			}
			
			ci.cancel();
		}
	}
	
	@Override
	public void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		updatePower(wire);
		
		//updateNeighborsOf(world, pos); // Removed for the sake of vanilla parity
		updateNeighborsOfConnectedWires(wire);
	}
	
	@Override
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		if (!moved) {
			updatePower(wire);
			
			updateNeighborsOf(world, pos);
			updateNeighborsOfConnectedWires(wire);
		}
	}
	
	private void updatePower(WireNode wire) {
		WireHandler wireHandler = ((IServerWorld)wire.world).getWireHandler(this);
		wireHandler.updatePower(wire);
	}
	
	private void tryUpdateNeighborsOfWire(World world, BlockPos pos) {
		tryUpdateNeighborsOfWire(world, pos, world.getBlockState(pos));
	}
	
	private void tryUpdateNeighborsOfWire(World world, BlockPos pos, BlockState state) {
		if (state.isOf((Block)(Object)this)) {
			updateNeighborsOf(world, pos);
		}
	}
	
	private void updateNeighborsOf(World world, BlockPos pos) {
		List<BlockPos> positions = new ArrayList<>();
		WireHandler.collectNeighborPositions(pos, positions);
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, (Block)(Object)this, pos);
		}
	}
	
	// This is kept for the sake of vanilla parity.
	// In vanilla, wire blocks do not update neighbors
	// when their connection properties change.
	// Instead, they rely on block updates due to the
	// state change (in World.setBlockState) to update
	// direct neighbors, and distant neighbors are
	// not updated at all. 
	// Weirdly, when a wire block is placed or destroyed,
	// it updates all the neighbors around neighboring
	// wire blocks. This behavior is replicated here.
	private void updateNeighborsOfConnectedWires(WireNode wire) {
		for (BlockPos pos : wire.connectionsOut) {
			tryUpdateNeighborsOfWire(wire.world, pos);
		}
		for (BlockPos pos : wire.connectionsIn) {
			tryUpdateNeighborsOfWire(wire.world, pos);
		}
	}
}
