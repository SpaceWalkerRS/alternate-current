package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	@Redirect(
			method = "onBlockAdded",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
			)
	)
	private void onOnBlockAddedRedirectUpdate(RedstoneWireBlock redstoneWireBlock, World world, BlockPos pos, BlockState state) {
		WorldAccess worldAccess = ((IServerWorld)world).getAccess(this);
		WireNode wire = worldAccess.getWire(pos, true, true);
		
		worldAccess.getWireHandler().onWireAdded(wire);
		
		// Because of a check in World.setBlockState, shape updates
		// after placing a block are omitted if the block state
		// changes while setting it in the chunk. This can happen
		// due to the above call to the wire handler. To make sure
		// connections are properly updated after placing a redstone
		// wire, shape updates are emitted here.
		wire.state.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);
		wire.state.prepare(world, pos, Block.NOTIFY_LISTENERS);
	}
	
	@Redirect(
			method = "onStateReplaced",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
			)
	)
	private void onOnStateReplacedRedirectUpdate(RedstoneWireBlock redstoneWireBlock, World world, BlockPos pos, BlockState state) {
		WorldAccess worldAccess = ((IServerWorld)world).getAccess(this);
		WireNode wire = worldAccess.getWire(pos, true, true);
		
		worldAccess.removeWire(wire);
		wire.updateConnectedWires();
		
		// Only call the wire handler if the wire was not removed
		// by the wire handler.
		if (!wire.shouldBreak) {
			worldAccess.getWireHandler().onWireRemoved(wire);
		}
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onOnNeighborUpdateInjectAtHead(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		if (!world.isClient()) {
			((IServerWorld)world).getAccess(this).getWireHandler().onWireUpdated(pos);
		}
		
		ci.cancel();
	}
	
	@Override
	public int getMinPower() {
		return 0;
	}
	
	@Override
	public int getMaxPower() {
		return 15;
	}
	
	@Override
	public int getPowerStep() {
		return 1;
	}
	
	@Override
	public int getPower(WorldAccess world, BlockPos pos, BlockState state) {
		return state.get(Properties.POWER);
	}
	
	@Override
	public BlockState updatePowerState(WorldAccess world, BlockPos pos, BlockState state, int power) {
		return state.with(Properties.POWER, clampPower(power));
	}
	
	@Override
	public void findWireConnections(WireNode wire) {
		WorldAccess world = wire.world;
		BlockPos pos = wire.pos;
		
		boolean aboveIsSolid = world.isSolidBlock(pos.up());
		boolean belowIsSolid = world.isSolidBlock(pos.down());
		
		for (int iDir = 0; iDir < WireHandler.Directions.HORIZONTAL.length; iDir++) {
			Direction dir = WireHandler.Directions.HORIZONTAL[iDir];
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (isOf(neighbor)) {
				wire.connections.add(side, iDir, true, true);
				continue;
			}
			
			boolean sideIsSolid = world.isSolidBlock(side, neighbor);
			
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				neighbor = world.getBlockState(belowSide);
				
				if (isOf(neighbor)) {
					wire.connections.add(belowSide, iDir, true, belowIsSolid);
				}
			}
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				neighbor = world.getBlockState(aboveSide);
				
				if (isOf(neighbor)) {
					wire.connections.add(aboveSide, iDir, sideIsSolid, true);
				}
			}
		}
	}
}
