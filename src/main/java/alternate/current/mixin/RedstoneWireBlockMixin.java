package alternate.current.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.PerformanceMode;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.utils.Directions;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
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
		if (AlternateCurrentMod.MODE == PerformanceMode.MAX_PERFORMANCE) {
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
		if (AlternateCurrentMod.MODE == PerformanceMode.MAX_PERFORMANCE) {
			ci.cancel(); // replaced by WireBlock.onWireRemoved
		}
	}
	
	@Inject(
			method = "update",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdateInjectAtHead(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.MODE == PerformanceMode.MAX_PERFORMANCE) {
			WireNode wire = getOrCreateWire(world, pos, true);
			
			if (wire != null) {
				tryUpdatePower(wire);
			}
			
			ci.cancel();
		}
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
		
		// temporary solution
		if (wire.power > MIN_POWER) {
			wire.state.updateNeighbors(world, pos, 2);
			wire.state.prepare(world, pos, 2);
		}
		
		updateNeighborsOfConnectedWires(wire);
	}
	
	@Override
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		if (!moved) {
			tryUpdatePower(wire);
			
			updateNeighborsOf(world, pos);
			updateNeighborsOfConnectedWires(wire);
		}
	}
	
	private void tryUpdatePower(WireNode wire) {
		if (shouldUpdatePower(wire)) {
			((IServerWorld)wire.world).getWireHandler(this).updatePower(wire);
		}
	}
	
	private boolean shouldUpdatePower(WireNode wire) {
		wire.prevPower = clampPower(wire.power);
		
		if (wire.removed || wire.shouldBreak) {
			wire.power = MIN_POWER;
		} else {
			wiresGivePower = false;
			wire.power = wire.externalPower = getExternalPower(wire);
			wiresGivePower = true;
			
			if (wire.power < MAX_POWER) {
				int wirePower = getWirePower(wire);
				
				if (wirePower > wire.power) {
					wire.power = wirePower;
				}
			}
		}
		
		return wire.power != wire.prevPower;
	}
	
	private int getExternalPower(WireNode wire) {
		int power = MIN_POWER;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = wire.pos.offset(dir);
			BlockState neighbor = wire.world.getBlockState(side);
			
			if (neighbor.isSolidBlock(wire.world, side)) {
				power = Math.max(power, getStrongPowerTo(wire.world, side, dir.getOpposite()));
			}
			if (neighbor.emitsRedstonePower()) {
				power = Math.max(power, neighbor.getWeakRedstonePower(wire.world, side, dir));
			}
			
			if (power >= MAX_POWER) {
				return MAX_POWER;
			}
		}
		
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
			BlockState neighbor = world.getBlockState(side);
			
			if (neighbor.emitsRedstonePower()) {
				power = Math.max(power, neighbor.getStrongRedstonePower(world, side, dir));
				
				if (power >= MAX_POWER) {
					return MAX_POWER;
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(WireNode wire) {
		int power = MIN_POWER;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getOrCreateWire(wire.world, pos, true);
			
			if (connectedWire != null) {
				power = Math.max(power, connectedWire.power - 1);
			}
		}
		
		return power;
	}
	
	private void tryUpdateNeighborsOfWire(World world, BlockPos pos) {
		tryUpdateNeighborsOfWire(world, pos, world.getBlockState(pos));
	}
	
	private void tryUpdateNeighborsOfWire(World world, BlockPos pos, BlockState state) {
		if (isOf(state)) {
			updateNeighborsOf(world, pos);
		}
	}
	
	private void updateNeighborsOf(World world, BlockPos pos) {
		List<BlockPos> positions = new ArrayList<>();
		WireHandler.collectNeighborPositions(pos, positions);
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, asBlock(), pos);
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
