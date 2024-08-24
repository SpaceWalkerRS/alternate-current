package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IServerWorld;

import net.minecraft.world.World;

@Mixin(World.class)
public abstract class WorldMixin implements IServerWorld {

	@Inject(
		method = "saveData",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$save(CallbackInfo ci) {
		((IServerWorld) this).alternate_current$getWireHandler().getConfig().save(false);
	}
}
