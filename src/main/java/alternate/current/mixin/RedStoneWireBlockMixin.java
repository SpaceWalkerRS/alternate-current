package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {

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
	private void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerLevel)level).getWireHandler().onWireAdded(pos);
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
	private void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerLevel)level).getWireHandler().onWireRemoved(pos, state);
		}
	}

	@Inject(
		method = "neighborChanged",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerLevel)level).getWireHandler().onWireUpdated(pos);
			ci.cancel();
		}
	}
}
