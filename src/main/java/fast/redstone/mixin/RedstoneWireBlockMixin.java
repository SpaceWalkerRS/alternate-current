package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.FastRedstoneMod;
import fast.redstone.RedstoneWireHandler;

import fast.redstone.interfaces.mixin.IWireBlock;
import net.minecraft.block.AbstractBlock.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements IWireBlock {
	
	@Shadow private boolean wiresGivePower;
	
	RedstoneWireHandler wireHandler;
	
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
			method = "update",
			cancellable = true,
			at = @At(
					value = "HEAD"
			)
	)
	private void onUpdateInjectAtHead(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (FastRedstoneMod.ENABLED) {
			wireHandler.updatePower(world, pos, state);
			ci.cancel();
		}
	}
	
	@Override
	public void setWiresGivePower(boolean wiresGivePower) {
		this.wiresGivePower = wiresGivePower;
	}
}
