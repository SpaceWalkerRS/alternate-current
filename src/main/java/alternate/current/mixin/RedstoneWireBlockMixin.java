package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.wire.WireBlock;
import alternate.current.wire.WireType;
import alternate.current.wire.WireTypes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin implements WireBlock {

	private static final WireType TYPE = WireTypes.REDSTONE;

	@Inject(
		method = "update",
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
		method = "onCreation",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
		)
	)
	private void onPlace(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).getWireHandler().onWireAdded(pos, TYPE);
		}
	}

	@Inject(
		method = "onBreaking",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/block/RedstoneWireBlock;update(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;"
		)
	)
	private void onRemove(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IServerWorld)world).getWireHandler().onWireRemoved(pos, state, TYPE);
		}
	}

	@Inject(
		method = "method_8641",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void onNeighborChanged(BlockState state, World world, BlockPos pos, Block fromBlock, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (!world.isClient) {
				((IServerWorld)world).getWireHandler().onWireUpdated(pos, TYPE);
			}

			ci.cancel();
		}
	}

	@Override
	public WireType getWireType() {
		return TYPE;
	}
}
