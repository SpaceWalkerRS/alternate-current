package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.FastRedstone;
import fast.redstone.RedstoneWireHandler;
import fast.redstone.interfaces.mixin.IRedstoneWire;
import fast.redstone.interfaces.mixin.IWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements IRedstoneWire {
	
	@Shadow private boolean wiresGivePower;
	
	@Shadow protected abstract int getReceivedRedstonePower(World world, BlockPos pos);
	
	@Inject(
			method = "update",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdateInjectAtHead(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (FastRedstone.ACTIVE) {
			if (!world.isClient()) {
				RedstoneWireHandler wireHandler = ((IWorld)world).getOrCreateWireHandler(pos, state);
				
				wireHandler.updateNetworkPower(pos);
			}
			
			ci.cancel();
		}
	}
	
	@Inject(
			method = "onBlockAdded",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onOnBlockAddedInjectAtHead(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
		if (FastRedstone.ACTIVE) {
			if (!state.isOf(oldState.getBlock())) {
				RedstoneWireHandler wireHandler = new RedstoneWireHandler(world, pos, state);
				
				((IWorld)world).addWireHandler(wireHandler);
				
				wireHandler.updateNetworkConnections(pos, state);
				wireHandler.updateNetworkPower(pos);
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
	private void onOnStateReplacedInjectAtHead(BlockState state, World world, BlockPos pos, BlockState newState, boolean notify, CallbackInfo ci) {
		if (FastRedstone.ACTIVE) {
			if (!state.isOf(newState.getBlock())) {
				RedstoneWireHandler wireHandler = ((IWorld)world).getWireHandler(pos);
				
				if (wireHandler != null) {
					wireHandler.updateNetworkConnections(pos, newState);
				}
			}
			
			ci.cancel();
		}
	}
	
	@Override
	public void setWiresGivePower(boolean wiresGivePower) {
		this.wiresGivePower = wiresGivePower;
	}
}
