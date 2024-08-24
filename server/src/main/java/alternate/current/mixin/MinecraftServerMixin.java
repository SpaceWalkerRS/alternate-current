package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.util.DimensionUtil;
import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(
		method = "saveWorlds",
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$save(CallbackInfo ci) {
		ServerWorld overworld = ((MinecraftServer) (Object) this).getWorld(DimensionUtil.OVERWORLD);
		WireHandler wireHandler = ((IServerWorld) overworld).alternate_current$getWireHandler();

		wireHandler.getConfig().save(false);
	}
}