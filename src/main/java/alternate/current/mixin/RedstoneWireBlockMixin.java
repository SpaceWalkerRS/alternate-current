package alternate.current.mixin;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
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
		cir.setReturnValue(state);
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
		((IServerWorld)world).getAccess(this).getWireHandler().onWireAdded(pos);
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
		((IServerWorld)world).getAccess(this).getWireHandler().onWireRemoved(pos);
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onNeighborUpdateInjectAtHead(World world, BlockPos pos, BlockState state, Block block, CallbackInfo ci) {
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
	public void findWireConnections(WireNode wire, BiFunction<Node, Integer, Node> nodeProvider) {
		boolean belowIsSolid = nodeProvider.apply(wire, WireHandler.Directions.DOWN).isSolidBlock();
		boolean aboveIsSolid = nodeProvider.apply(wire, WireHandler.Directions.UP).isSolidBlock();
		
		for (int iDir = 0; iDir < WireHandler.Directions.HORIZONTAL.length; iDir++) {
			Node neighbor = nodeProvider.apply(wire, iDir);
			
			if (neighbor.isWire()) {
				wire.connections.add(neighbor.asWire(), iDir, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock();
			
			if (!sideIsSolid) {
				Node node = nodeProvider.apply(neighbor, WireHandler.Directions.DOWN);
				
				if (node.isWire()) {
					wire.connections.add(node.asWire(), iDir, true, belowIsSolid);
				}
			}
			if (!aboveIsSolid) {
				Node node = nodeProvider.apply(neighbor, WireHandler.Directions.UP);
				
				if (node.isWire()) {
					wire.connections.add(node.asWire(), iDir, sideIsSolid, true);
				}
			}
		}
	}
}
