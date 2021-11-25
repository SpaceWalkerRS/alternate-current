package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireHandler.NodeProvider;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;
import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;

import net.minecraft.block.Block;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements WireBlock {
	
	@Inject(
			method = "method_8781",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdate(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			// Using redirects for calls to this method makes conflicts with
			// other mods more likely, so we inject-cancel instead.
			ci.cancel();
		}
	}
	
	@Inject(
			method = "method_8622",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_8781(Lnet/minecraft/world/World;III)V"
			)
	)
	private void onBlockAdded(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).getAccess(this).getWireHandler().onWireAdded(new BlockPos(x, y, z));
		}
	}
	
	@Inject(
			method = "method_8613",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/RedstoneWireBlock;method_8781(Lnet/minecraft/world/World;III)V"
			)
	)
	private void onBlockRemoved(World world, int x, int y, int z, Block block, int blockData, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).getAccess(this).getWireHandler().onWireRemoved(new BlockPos(x, y, z));
		}
	}
	
	@Inject(
			method = "neighborUpdate",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onNeighborUpdate(World world, int x, int y, int z, Block fromBlock, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (!world.isClient) {
				((IServerWorld)world).getAccess(this).getWireHandler().onWireUpdated(new BlockPos(x, y, z));
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
		return state.getBlockData();
	}
	
	@Override
	public BlockState updatePowerState(WorldAccess world, BlockPos pos, BlockState state, int power) {
		return state.withBlockData(power);
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
