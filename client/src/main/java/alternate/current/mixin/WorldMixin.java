package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.interfaces.mixin.IWorld;
import alternate.current.wire.WireHandler;

import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.storage.WorldStorage;

@Mixin(World.class)
public class WorldMixin implements IWorld {

	private WireHandler wireHandler;

	@Inject(
		method = "<init>(Lnet/minecraft/world/storage/WorldStorage;Ljava/lang/String;Lnet/minecraf/world/dimension/Dimension;Lnet/minecraft/world/WorldSettings;)V",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$init(WorldStorage storage, String name, Dimension dimension, WorldSettings settings, CallbackInfo ci) {
		this.wireHandler = new WireHandler((World)(Object)this, storage);
	}

	@Inject(
		method = "<init>(Lnet/minecraft/world/storage/WorldStorage;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$init(WorldStorage storage, String name, WorldSettings settings, CallbackInfo ci) {
		this.wireHandler = new WireHandler((World)(Object)this, storage);
	}

	@Inject(
		method = "saveData",
		at = @At(
			value = "TAIL"
		)
	)
	private void alternate_current$save(CallbackInfo ci) {
		wireHandler.getConfig().save(false);
	}

	@Override
	public WireHandler alternate_current$getWireHandler() {
		return wireHandler;
	}
}
