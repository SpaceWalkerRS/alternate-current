package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;

import net.minecraft.block.Block;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public abstract class RedstoneWireBlockMixin extends Block {

	private RedstoneWireBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Inject(
		method = "updatePower",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$onUpdatePower(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			// Using redirects for calls to this method makes conflicts with
			// other mods more likely, so we inject-cancel instead.
			ci.cancel();
		}
	}

	@Inject(
		method = "onAdded",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/RedstoneWireBlock;updatePower(Lnet/minecraft/world/World;III)V"
		)
	)
	private void alternate_current$onAdded(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IWorld)world).alternate_current$getWireHandler().onWireAdded(new BlockPos(x, y, z));
		}
	}

	@Inject(
		method = "onRemoved",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/block/RedstoneWireBlock;updatePower(Lnet/minecraft/world/World;III)V"
		)
	)
	private void alternate_current$onRemoved(World world, int x, int y, int z, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			((IWorld)world).alternate_current$getWireHandler().onWireRemoved(new BlockPos(x, y, z), new BlockState(id, world.getBlockMetadata(x, y, z)));
		}
	}

	@Inject(
		method = "neighborChanged",
		cancellable = true,
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$onNeighborChanged(World world, int x, int y, int z, int neighborBlockId, CallbackInfo ci) {
		if (AlternateCurrentMod.on) {
			if (((IWorld)world).alternate_current$getWireHandler().onWireUpdated(new BlockPos(x, y, z))) {
				ci.cancel(); // needed to fix duplication bugs
			}
		}
	}
}
