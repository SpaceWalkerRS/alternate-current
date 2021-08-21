package alternate.current.MAX_PERFORMANCE.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IMinecraftServer;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements IMinecraftServer {
	
	@Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;
	
	@Override
	public void clearWires() {
		for (ServerWorld world : worlds.values()) {
			((IWorld)world).clearWires();
		}
	}
}
