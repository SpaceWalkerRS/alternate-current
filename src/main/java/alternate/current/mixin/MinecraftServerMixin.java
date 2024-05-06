package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(
		method = "saveWorlds",
		at = @At(
			value = "HEAD"
		)
	)
	private void alternate_current$save(boolean silent, CallbackInfo ci) {
		ServerWorld overworld = ((MinecraftServer) (Object) this).getWorld(DimensionType.OVERWORLD);
		WireHandler wireHandler = ((IServerWorld) overworld).alternate_current$getWireHandler();

		wireHandler.getConfig().save(silent);
	}
}
