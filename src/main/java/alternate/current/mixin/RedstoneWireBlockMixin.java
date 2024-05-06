package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;

import net.minecraft.block.Block;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {

	@Inject(
		method = "updatePower",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$onUpdate(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {
		if (AlternateCurrentMod.on) {
			// Using redirects for calls to this method makes conflicts with
			// other mods more likely, so we inject-cancel instead.
			cir.setReturnValue(state);
		}
	}

	@Inject(
		method = "onAdded",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/RedstoneWireBlock;updatePower(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockState;)Lnet/minecraft/block/state/BlockState;"
		)
	)
	private void alternate_current$onAdded(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).alternate_current$getWireHandler().onWireAdded(pos);
		}
	}

	@Inject(
		method = "onRemoved",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/RedstoneWireBlock;updatePower(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/BlockState;)Lnet/minecraft/block/state/BlockState;"
		)
	)
	private void alternate_current$onRemoved(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).alternate_current$getWireHandler().onWireRemoved(pos, state);
		}
	}

	@Inject(
		method = "neighborChanged",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$onNeighborChanged(World world, BlockPos pos, BlockState state, Block neighborBlock, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (((IServerWorld)world).alternate_current$getWireHandler().onWireUpdated(pos)) {
				ci.cancel(); // needed to fix duplication bugs
			}
		}
	}
}
