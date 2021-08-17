package alternate.current.mixin.debug;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	
	@Inject(
			method = "save",
			at = @At(
					value = "HEAD"
			)
	)
	private void onSaveInjectAtHead(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
		if (!suppressLogs) {
			ProfilerResults.log();
			ProfilerResults.clear();
		}
	}
}
