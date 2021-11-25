package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireHandler.NodeProvider;
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
			method = "method_26769",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdate(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
		if (AlternateCurrentMod.on) {
			// Using redirects for calls to this method makes conflicts with
			// other mods more likely, so we inject-cancel instead.
			cir.setReturnValue(state);
		}
	}
	
	@Inject(
			method = "onBlockAdded",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_26769(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
			)
	)
	private void onBlockAdded(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).getAccess(this).getWireHandler().onWireAdded(pos);
		}
	}
	
	@Inject(
			method = "onBlockRemoved",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_26769(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
			)
	)
	private void onBlockRemoved(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (!AlternateCurrentMod.on) {
			((IServerWorld)world).getAccess(this).getWireHandler().onWireRemoved(pos);
		}
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onNeighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (!world.isClient) {
				((IServerWorld)world).getAccess(this).getWireHandler().onWireUpdated(pos);
			}
			
			ci.cancel();
		}
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
		return state.with(RedstoneWireBlock.POWER, power);
	}
	
	@Override
	public void findWireConnections(WireNode wire, NodeProvider nodes) {
		boolean belowIsConductor = nodes.getNeighbor(wire, WireHandler.Directions.DOWN).isConductor();
		boolean aboveIsConductor = nodes.getNeighbor(wire, WireHandler.Directions.UP).isConductor();
		
		wire.connections.set((connections, iDir) -> {
			Node neighbor = nodes.getNeighbor(wire, iDir);
			
			if (neighbor.isWire()) {
				connections.add(neighbor.asWire(), iDir, true, true);
				return;
			}
			
			boolean sideIsConductor = neighbor.isConductor();
			
			if (!sideIsConductor) {
				Node node = nodes.getNeighbor(neighbor, WireHandler.Directions.DOWN);
				
				if (node.isWire()) {
					connections.add(node.asWire(), iDir, true, belowIsConductor);
				}
			}
			if (!aboveIsConductor) {
				Node node = nodes.getNeighbor(neighbor, WireHandler.Directions.UP);
				
				if (node.isWire()) {
					connections.add(node.asWire(), iDir, sideIsConductor, true);
				}
			}
		});
	}
}
