package fast.redstone.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.FastRedstoneMod;
import fast.redstone.NeighborType;
import fast.redstone.Neighbor;
import fast.redstone.RedstoneWireHandler;
import fast.redstone.Wire;
import fast.redstone.interfaces.mixin.IWireBlock;
import fast.redstone.interfaces.mixin.IWorld;
import fast.redstone.utils.Directions;

import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements IWireBlock {
	
	@Shadow private boolean wiresGivePower;
	
	RedstoneWireHandler wireHandler;
	
	@Inject(
			method = "<init>",
			at = @At(
					value = "RETURN"
			)
	)
	private void onInitInjectAtReturn(Settings settings, CallbackInfo ci) {
		wireHandler = new RedstoneWireHandler((Block)(Object)this);
	}
	
	@Inject(
			method = "onBlockAdded",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onOnBlockAddedInjectAtHead(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
		if (FastRedstoneMod.ENABLED) {
			if (!oldState.isOf((Block)(Object)this)) {
				Wire wire = new Wire(world, pos, state);
				((IWorld)world).setWire(pos, wire, true);
				
				tryUpdatePower(wire);
				//updateNeighborsOfWire(world, pos, state); // Removed for the sake of vanilla parity
				updateNeighborsOfConnectedWires(wire);
			}
			
			ci.cancel();
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
		if (FastRedstoneMod.ENABLED) {
			if (newState.isOf((Block)(Object)this)) {
				Wire wire = getWire(world, pos, true, false);
				wire.state = newState;
				
				if (newState.get(Properties.POWER) == state.get(Properties.POWER)) {
					wire.updateConnections();
				}
			} else {
				//super.onStateReplaced(state, world, pos, newState, moved); // Only removes block entity
				
				Wire wire = getWire(world, pos, false, false);
				((IWorld)world).setWire(pos, null, true);
				
				if (!moved) {
					updateNeighborsOfWire(world, pos);
					
					if (wire != null) {
						updateNeighborsOfConnectedWires(wire);
					}
				}
			}
			
			ci.cancel();
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
		if (FastRedstoneMod.ENABLED) {
			Wire wire = getWire(world, pos, true, true);
			
			wire.collectNeighbors();
			if (fromBlock != (Block)(Object)this) {
				wire.findConnections();
			}
			
			tryUpdatePower(wire);
			
			ci.cancel();
		}
	}
	
	@Override
	public Wire getWire(World world, BlockPos pos, boolean orCreate, boolean updateConnections) {
		Wire wire = ((IWorld)world).getWireV2(pos);
		
		if (orCreate && wire == null) {
			BlockState state = world.getBlockState(pos);
			
			if (state.isOf((Block)(Object)this)) {
				wire = new Wire(world, pos, state);
				((IWorld)world).setWire(pos, wire, updateConnections);
			}
		}
		
		return wire;
	}
	
	private void tryUpdatePower(Wire wire) {
		int receivedPower = getReceivedPower(wire);
		
		if (wire.power != receivedPower) {
			wireHandler.updatePower(wire);
			
			/*World world = wire.getWorld();
			BlockPos pos = wire.getPos();
			
			BlockState newState = wire.getState().with(Properties.POWER, receivedPower);
			world.setBlockState(pos, newState, 2);
			
			wire.setPower(receivedPower);
			
			Set<BlockPos> notifiers = new HashSet<>();
			Set<BlockPos> blockUpdates = new LinkedHashSet<>();
			
			notifiers.add(pos);
			
			for (Direction dir : Direction.values()) {
				notifiers.add(pos.offset(dir));
			}
			for (BlockPos notifier : notifiers) {
				for (Direction dir : Direction.values()) {
					blockUpdates.add(notifier.offset(dir));
				}
			}
			
			for (BlockPos neighborPos : blockUpdates) {
				world.updateNeighbor(neighborPos, wire.getWireBlock(), pos);
			}*/
		}
	}
	
	private int getReceivedPower(Wire wire) {
		int externalPower = getExternalPower(wire);
		
		if (externalPower > wireHandler.MAX_POWER) {
			return wireHandler.MAX_POWER;
		}
		
		int wirePower = getWirePower(wire);
		
		if (wirePower > externalPower) {
			return wirePower;
		}
		
		return externalPower;
	}
	
	private int getExternalPower(Wire wire) {
		int power = 0;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Neighbor neighbor = wire.neighbors[index];
			int powerFromNeighbor = 0;
			
			if (neighbor.type == NeighborType.SOLID_BLOCK) {
				powerFromNeighbor = getStrongPowerTo(wire.world, neighbor.pos, Directions.ALL[index].getOpposite());
			} else if (neighbor.type == NeighborType.REDSTONE_COMPONENT) {
				powerFromNeighbor = neighbor.state.getWeakRedstonePower(wire.world, neighbor.pos, Directions.ALL[index]);
			}
			
			if (powerFromNeighbor > power) {
				power = powerFromNeighbor;
				
				if (power >= wireHandler.MAX_POWER) {
					return wireHandler.MAX_POWER;
				}
			}
		}
		
		return power;
	}
	
	private int getStrongPowerTo(World world, BlockPos pos, Direction ignore) {
		int power = 0;
		
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			BlockState state = world.getBlockState(side);
			
			if (!state.isOf((Block)(Object)this) && state.emitsRedstonePower()) {
				int powerFromSide = state.getStrongRedstonePower(world, side, dir);
				
				if (powerFromSide > power) {
					power = powerFromSide;
					
					if (power >= wireHandler.MAX_POWER) {
						return wireHandler.MAX_POWER;
					}
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(Wire wire) {
		int power = 0;
		
		for (BlockPos pos : wire.connectionsIn) {
			Wire connectedWire = getWire(wire.world, pos, true, true);
			
			if (connectedWire != null) {
				int powerFromWire = connectedWire.power - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
				}
			}
		}
		
		return power;
	}
	
	private void updateNeighborsOfWire(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (!state.isOf((Block)(Object)this)) {
			return;
		}
		
		List<BlockPos> positions = new ArrayList<>();
		wireHandler.collectNeighborPositions(pos, positions);
		
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
	private void updateNeighborsOfConnectedWires(Wire wire) {
		for (BlockPos pos : wire.connectionsOut) {
			updateNeighborsOfWire(wire.world, pos);
		}
		for (BlockPos pos : wire.connectionsIn) {
			updateNeighborsOfWire(wire.world, pos);
		}
	}
}
