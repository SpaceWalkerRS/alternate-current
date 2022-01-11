package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IServerLevel;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.LevelAccess;

import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements IServerLevel {
	
	private final Map<WireBlock, LevelAccess> access = new HashMap<>();
	
	@Override
	public LevelAccess getAccess(WireBlock wireBlock) {
		return access.computeIfAbsent(wireBlock, key -> new LevelAccess(wireBlock, (ServerLevel)(Object)this));
	}
}
