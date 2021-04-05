package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.FastRedstoneMod;
import fast.redstone.RedstoneWireHandler;
import fast.redstone.Wire;
import fast.redstone.WireConnection;
import fast.redstone.interfaces.mixin.IWireBlock;
import fast.redstone.interfaces.mixin.IWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin extends Block implements IWireBlock {
	
	@Shadow private boolean wiresGivePower;
	
	private RedstoneWireHandler wireHandler;
	
	public RedstoneWireBlockMixin(Settings settings) {
		super(settings);
	}
	
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
			//super.onStateReplaced(state, world, pos, newState, moved); // Only removes block entity
			
			if (newState.isOf((Block)(Object)this)) {
				getWire(world, pos, newState, true, true).updateState(newState);
			} else {
				Wire wire = getWire(world, pos, state, true, true);
				((IWorld)world).setWire(pos, null, true);
				
				if (!moved) {
					updateNeighborsOfWire(wire);
					updateNeighborsOfConnectedWires(wire);
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
			Wire wire = getWire(world, pos, state, true, true);
			
			if (fromBlock != (Block)(Object)this) {
				wire.updateConnections();
			}
			
			tryUpdatePower(wire);
			
			ci.cancel();
		}
	}
	
	@Override
	public Wire getWire(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections) {
		Wire wire = ((IWorld)world).getWire(pos);
		
		if (orCreate && wire == null) {
			wire = new Wire(world, pos, state);
			((IWorld)world).setWire(pos, wire, updateConnections);
		}
		
		return wire;
	}
	
	@Override
	public void setWiresGivePower(boolean wiresGivePower) {
		this.wiresGivePower = wiresGivePower;
	}
	
	private void tryUpdatePower(Wire wire) {
		if (shouldUpdatePower(wire)) {
			wireHandler.updatePower(wire);
		}
	}
	
	private boolean shouldUpdatePower(Wire wire) {
		return wire.getPower() != getReceivedPower(wire);
	}
	
	private int getReceivedPower(Wire wire) {
		World world = wire.getWorld();
		BlockPos pos = wire.getPos();
		
		int externalPower = getExternalPower(world, pos);
		
		if (externalPower >= wireHandler.MAX_POWER) {
			return wireHandler.MAX_POWER;
		}
		
		int powerFromWires = getPowerFromWires(wire);
		
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
	private int getExternalPower(World world, BlockPos pos) {
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
	private int getPowerFromWires(Wire wire) {
		int power = 0;
		
		for (WireConnection connection : wire.getConnections()) {
			if (connection.in) {
				int powerFromWire = connection.wire.getPower() - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
					
					if (power >= wireHandler.MAX_POWER) {
						return wireHandler.MAX_POWER;
					}
				}
			}
		}
		
		return power;
	}
	
	/**
	 * Updates all the blocks within a distance of 2 of the given location.
	 * @param world
	 * @param pos
	 * @param state
	 */
	private void updateNeighborsOfWire(Wire wire) {
		World world = wire.getWorld();
		BlockPos pos = wire.getPos();
		Block wireBlock = wire.getWireBlock();
		
		for (BlockPos neighborPos : wireHandler.collectNeighborPositions(pos)) {
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
	private void updateNeighborsOfConnectedWires(Wire wire) {
		for (WireConnection connection : wire.getConnections()) {
			updateNeighborsOfWire(connection.wire);
		}
	}
}
