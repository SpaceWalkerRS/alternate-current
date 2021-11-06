package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	@Inject(
			method = "method_10485",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdate(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
		// Using redirects for calls to this method makes conflicts with
		// other mods more likely, so we inject-cancel instead.
		cir.setReturnValue(state);
	}
	
	@Inject(
			method = "method_9615",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_10485(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/BlockState;)Lnet/minecraft/BlockState;"
			)
	)
	private void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, CallbackInfo ci) {
		((IServerWorld)world).getAccess(this).getWireHandler().onWireAdded(pos);
		
		// Because of a check in World.setBlockState, shape updates
		// after placing a block are omitted if the block state
		// changes while setting it in the chunk. This can happen
		// due to the above call to the wire handler. To make sure
		// connections are properly updated after placing a redstone
		// wire, shape updates are emitted here.
		BlockState newState = world.getBlockState(pos);
		
		if (newState != state) {
			newState.method_73271(world, pos, 2);
			newState.method_73283(world, pos, 2);
		}
	}
	
	@Inject(
			method = "onBlockAdded", // the mapping is wrong
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_10485(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/BlockState;)Lnet/minecraft/BlockState;"
			)
	)
	private void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		((IServerWorld)world).getAccess(this).getWireHandler().onWireRemoved(pos);
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onNeighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, CallbackInfo ci) {
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
	public void findWireConnections(WireNode wire, WireHandler.NodeProvider nodeProvider) {
		boolean belowIsSolid = nodeProvider.getNeighbor(wire, WireHandler.Directions.DOWN).isSolidBlock();
		boolean aboveIsSolid = nodeProvider.getNeighbor(wire, WireHandler.Directions.UP).isSolidBlock();
		
		for (int iDir = 0; iDir < WireHandler.Directions.HORIZONTAL.length; iDir++) {
			Node neighbor = nodeProvider.getNeighbor(wire, iDir);
			
			if (neighbor.isWire()) {
				wire.connections.add(neighbor.asWire(), iDir, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock();
			
			if (!sideIsSolid) {
				Node node = nodeProvider.getNeighbor(neighbor, WireHandler.Directions.DOWN);
				
				if (node.isWire()) {
					wire.connections.add(node.asWire(), iDir, true, belowIsSolid);
				}
			}
			if (!aboveIsSolid) {
				Node node = nodeProvider.getNeighbor(neighbor, WireHandler.Directions.UP);
				
				if (node.isWire()) {
					wire.connections.add(node.asWire(), iDir, sideIsSolid, true);
				}
			}
		}
	}
}
