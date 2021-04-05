package fast.redstone.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.FastRedstoneMod;
import fast.redstone.interfaces.mixin.IWorld;
import net.minecraft.server.world.ServerWorld;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
	
	@Inject(method = "tick", at = @At(value = "HEAD"))
	private void tickstart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		((IWorld)this).reset();
	}
	
	@Inject(method = "tick", at = @At(value = "RETURN"))
	private void tickend(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		int count = ((IWorld)this).getCount();
		
		if (count > 0) {
			FastRedstoneMod.LOGGER.info(count);
		}
	}
}
