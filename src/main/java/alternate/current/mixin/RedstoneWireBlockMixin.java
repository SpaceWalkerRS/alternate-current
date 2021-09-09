package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	@Inject(
			method = "update",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdate(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
		// Using redirects for calls to this method makes conflicts with
		// other mods more likely, so we inject-cancel instead.
		cir.cancel();
	}
	
	@Inject(
			method = "onCreation",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
			)
	)
	private void onOnCreationInjectBeforeUpdate(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		WorldAccess worldAccess = ((IServerWorld)world).getAccess(this);
		WireNode wire = worldAccess.getWire(pos, true, true);
		
		worldAccess.getWireHandler().onWireAdded(wire);
	}
	
	@Inject(
			method = "onBreaking",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
			)
	)
	private void onOnBreakingInjectBeforeUpdate(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
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
	private void onNeighborUpdateInjectAtHead(World world, BlockPos pos, BlockState state, Block fromBlock, CallbackInfo ci) {
		if (!world.isClient) {
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
		return state.get(RedstoneWireBlock.POWER);
	}
	
	@Override
	public BlockState updatePowerState(WorldAccess world, BlockPos pos, BlockState state, int power) {
		return state.with(RedstoneWireBlock.POWER, clampPower(power));
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
			
			boolean sideIsSolid = neighbor.getBlock().isFullCube();
			
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
