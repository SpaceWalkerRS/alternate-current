package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.wire.WireBlock;
import alternate.current.wire.WireType;
import alternate.current.wire.WireTypes;

import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin implements WireBlock {

	private static final WireType TYPE = WireTypes.REDSTONE;

	@Inject(
		method = "method_375",
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
		method = "method_460",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/block/RedstoneWireBlock;method_375(Lnet/minecraft/world/World;III)V"
		)
	)
	private void onPlace(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			onWireAdded(world, x, y, z);
		}
	}

	@Inject(
		method = "method_411",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/block/RedstoneWireBlock;method_375(Lnet/minecraft/world/World;III)V"
		)
	)
	private void onRemove(World world, int x, int y, int z, int blockId, int metadata, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			onWireRemoved(world, x, y, z, blockId, metadata);
		}
	}

	@Inject(
		method = "method_408",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void onNeighborChanged(World world, int x, int y, int z, int fromBlockId, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			onWireUpdated(world, x, y, z);
			ci.cancel();
		}
	}

	@Override
	public WireType getWireType() {
		return TYPE;
	}
}
