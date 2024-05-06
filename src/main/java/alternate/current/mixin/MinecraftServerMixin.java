package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(
		method = "saveAllChunks",
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$save(boolean silent, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
		ServerLevel overworld = ((MinecraftServer) (Object) this).overworld();
		WireHandler wireHandler = ((IServerLevel) overworld).alternate_current$getWireHandler();

		wireHandler.getConfig().save(silent);
	}
}
