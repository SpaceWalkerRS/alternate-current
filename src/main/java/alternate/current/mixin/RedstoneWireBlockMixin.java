package alternate.current.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.utils.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	private static final int MIN_POWER = 0;
	private static final int MAX_POWER = 15;
	
	@Shadow private boolean wiresGivePower;
	
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
				tryUpdatePower(wire);
			}
			
			ci.cancel();
		}
	}
	
	@Override
	public IntProperty getPowerProperty() {
		return Properties.POWER;
	}
	
	@Override
	public int getMinPower() {
		return MIN_POWER;
	}
	
	@Override
	public int getMaxPower() {
		return MAX_POWER;
	}
	
	@Override
	public void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		tryUpdatePower(wire);
		//updateNeighborsOfWire(world, pos, state); // Removed for the sake of vanilla parity
		updateNeighborsOfConnectedWires(wire);
	}
	
	@Override
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		if (!moved) {
			updateNeighborsOf(world, pos);
			updateNeighborsOfConnectedWires(wire);
		}
	}
	
	private void tryUpdatePower(WireNode wire) {
		int receivedPower = getReceivedPower(wire);
		
		if (wire.power != receivedPower) {
			((IServerWorld)wire.world).getWireHandler().updatePower(wire);
		}
	}
	
	private int getReceivedPower(WireNode wire) {
		wiresGivePower = false;
		int externalPower = getExternalPower(wire);
		wiresGivePower = true;
		
		if (externalPower > MAX_POWER) {
			return MAX_POWER;
		}
		
		int wirePower = getWirePower(wire);
		
		if (wirePower > externalPower) {
			return wirePower;
		}
		
		return externalPower;
	}
	
	private int getExternalPower(WireNode wire) {
		int power = MIN_POWER;
		wire.collectNeighbors();
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			Node neighbor = wire.neighbors[index];
			
			if (neighbor.isSolidBlock()) {
				power = Math.max(power, getStrongPowerTo(wire.world, neighbor.pos, dir.getOpposite()));
			}
			if (neighbor.isRedstoneComponent()) {
				power = Math.max(power, neighbor.state.getWeakRedstonePower(wire.world, neighbor.pos, dir));
			}
			
			if (power >= MAX_POWER) {
				return MAX_POWER;
			}
		}
		
		wire.clearNeighbors();
		return power;
	}
	
	private int getStrongPowerTo(World world, BlockPos pos, Direction ignore) {
		int power = MIN_POWER;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			BlockState sideState = world.getBlockState(side);
			
			if (sideState.emitsRedstonePower()) {
				int powerFromSide = sideState.getStrongRedstonePower(world, side, dir);
				
				if (powerFromSide > power) {
					power = powerFromSide;
					
					if (power >= MAX_POWER) {
						return MAX_POWER;
					}
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(WireNode wire) {
		int power = MIN_POWER;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getWire(wire.world, pos, true);
			
			if (connectedWire != null) {
				int powerFromWire = connectedWire.power - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
				}
			}
		}
		
		return power;
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
