package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerChunkManager;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements IServerWorld {
	
	private final Map<WireBlock, WireHandler> wireHandlers = new HashMap<>();
	
	@Shadow @Final private ServerChunkManager serverChunkManager;
	
	@Inject(method = "tick", at = @At(value = "HEAD"))
	private void tickstart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		((IWorld)this).reset();
	}
	
	@Inject(method = "tick", at = @At(value = "RETURN"))
	private void tickend(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		int count = ((IWorld)this).getCount();
		
		if (count > 0) {
			AlternateCurrentMod.LOGGER.info(count);
		}
	}
	
	@Override
	public WireHandler getWireHandler(WireBlock wireBlock) {
		WireHandler wireHandler = wireHandlers.get(wireBlock);
		
		if (wireHandler == null) {
			wireHandler = new WireHandler((ServerWorld)(Object)this, wireBlock);
			wireHandlers.put(wireBlock, wireHandler);
		}
		
		return wireHandler;
	}
	
	@Override
	public void clearWires() {
		((IServerChunkManager)serverChunkManager).clearWires();
	}
}
