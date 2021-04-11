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
import fast.redstone.interfaces.mixin.IWireBlock;
import fast.redstone.interfaces.mixin.IWorld;
import fast.redstone.v2.RedstoneWireHandlerV2;
import fast.redstone.v2.WireV2;

import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements IWireBlock {
	
	@Shadow private boolean wiresGivePower;
	
	RedstoneWireHandlerV2 wireHandler;
	
	@Inject(
			method = "<init>",
			at = @At(
					value = "RETURN"
			)
	)
	private void onInitInjectAtReturn(Settings settings, CallbackInfo ci) {
		wireHandler = new RedstoneWireHandlerV2((Block)(Object)this);
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
				WireV2 wire = new WireV2(world, pos, state);
				((IWorld)world).setWire(pos, wire, true);
				
				tryUpdatePowerV2(wire);
				//updateNeighborsOfWire(world, pos, state); // Removed for the sake of vanilla parity
				updateNeighborsOfConnectedWiresV2(wire);
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
				WireV2 wire = getWire(world, pos, newState, true, false);
				
				wire.updateState(newState);
				if (newState.get(Properties.POWER) == state.get(Properties.POWER)) {
					wire.updateConnections();
				}
			} else {
				//super.onStateReplaced(state, world, pos, newState, moved); // Only removes block entity
				
				WireV2 wire = getWire(world, pos, state, true, true);
				((IWorld)world).setWire(pos, null, true);
				
				if (!moved) {
					updateNeighborsOfWireV2(wire);
					updateNeighborsOfConnectedWiresV2(wire);
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
			WireV2 wire = getWire(world, pos, state, true, true);
			
			if (fromBlock != (Block)(Object)this) {
				wire.updateConnections();
			}
			tryUpdatePowerV2(wire);
			
			ci.cancel();
		}
	}
	
	@Override
	public WireV2 getWire(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections) {
		WireV2 wire = ((IWorld)world).getWire(pos);
		
		if (orCreate && wire == null) {
			wire = new WireV2(world, pos, state);
			((IWorld)world).setWire(pos, wire, updateConnections);
		}
		
		return wire;
	}
	
	private void tryUpdatePowerV2(WireV2 wire) {
		int receivedPower = getReceivedPowerV2(wire);
		
		if (wire.getPower() != receivedPower) {
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
	
	private int getReceivedPowerV2(WireV2 wire) {
		World world = wire.getWorld();
		BlockPos pos = wire.getPos();
		
		int externalPower = getExternalPowerV2(world, pos);
		
		if (externalPower >= wireHandler.MAX_POWER) {
			return wireHandler.MAX_POWER;
		}
		
		int powerFromWires = getPowerFromWiresV2(wire);
		
		if (powerFromWires > externalPower) {
			return powerFromWires;
		}
		
		return externalPower;
	}
	
	/**
	 * @param world
	 * @param pos
	 * @return The redstone power a block at the given location
	 * receives from non wire blocks.
	 */
	private int getExternalPowerV2(World world, BlockPos pos) {
		wiresGivePower = false;
		int power = world.getReceivedRedstonePower(pos);
		wiresGivePower = true;
		
		return power;
	}
	
	/**
	 * @param wire
	 * @return The redstone power the given wire receives
	 * from other wires it is connected to.
	 */
	private int getPowerFromWiresV2(WireV2 wire) {
		int power = 0;
		
		for (WireV2 connectedWire : wire.getConnectionsIn()) {
			int powerFromWire = connectedWire.getPower() - 1;
			
			if (powerFromWire > power) {
				power = powerFromWire;
			}
		}
		
		return power;
	}
	
	/**
	 * Updates all the blocks within a distance of 2 of the given wire.
	 * @param wire
	 */
	private void updateNeighborsOfWireV2(WireV2 wire) {
		World world = wire.getWorld();
		BlockPos pos = wire.getPos();
		Block wireBlock = wire.getWireBlock();
		
		List<BlockPos> positions = new ArrayList<>();
		wireHandler.collectNeighborPositions(pos, positions);
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, wireBlock, pos);
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
	private void updateNeighborsOfConnectedWiresV2(WireV2 wire) {
		for (WireV2 connectedWire : wire.getConnectionsOut()) {
			updateNeighborsOfWireV2(connectedWire);
		}
		for (WireV2 connectedWire : wire.getConnectionsIn()) {
			updateNeighborsOfWireV2(connectedWire);
		}
	}
}
