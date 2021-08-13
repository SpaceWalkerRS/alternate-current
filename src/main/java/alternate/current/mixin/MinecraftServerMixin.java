package alternate.current.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.interfaces.mixin.IMinecraftServer;
import alternate.current.interfaces.mixin.IWorld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements IMinecraftServer {
	
	@Shadow @Final private Map<DimensionType, ServerWorld> worlds;
	
	@Override
	public void clearWires() {
		for (ServerWorld world : worlds.values()) {
			((IWorld)world).clearWires();
		}
	}
}
