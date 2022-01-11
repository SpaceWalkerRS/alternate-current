package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.redstone.Node;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireHandler.NodeProvider;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.LevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin implements WireBlock {
	
	@Inject(
			method = "updatePowerStrength",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdate(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			// Using redirects for calls to this method makes conflicts with
			// other mods more likely, so we inject-cancel instead.
			ci.cancel();
		}
	}
	
	@Inject(
			method = "onPlace",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
			)
	)
	private void onBlockAdded(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerLevel)level).getAccess(this).getWireHandler().onWireAdded(pos);
			
			// Because of a check in Level.setBlock, shape updates
			// after placing a block are omitted if the block state
			// changes while setting it in the chunk. This can happen
			// due to the above call to the wire handler. To make sure
			// connections are properly updated after placing a redstone
			// wire, shape updates are emitted here.
			BlockState newState = level.getBlockState(pos);
			
			if (newState != state) {
				newState.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
				newState.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
			}
		}
	}
	
	@Inject(
			method = "onRemove",
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;updatePowerStrength(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
			)
	)
	private void onBlockRemoved(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerLevel)level).getAccess(this).getWireHandler().onWireRemoved(pos);
		}
	}
	
	@Inject(
			method = "neighborChanged",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onNeighborUpdate(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (!level.isClientSide()) {
				((IServerLevel)level).getAccess(this).getWireHandler().onWireUpdated(pos);
			}
			
			ci.cancel();
		}
	}
	
	@Override
	public Block asBlock() {
		return (Block)(Object)this;
	}
	
	@Override
	public boolean isOf(BlockState state) {
		return asBlock() == state.getBlock();
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
	public int getPower(LevelAccess level, BlockPos pos, BlockState state) {
		return state.getValue(BlockStateProperties.POWER);
	}
	
	@Override
	public BlockState updatePowerState(LevelAccess level, BlockPos pos, BlockState state, int power) {
		return state.setValue(BlockStateProperties.POWER, power);
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
