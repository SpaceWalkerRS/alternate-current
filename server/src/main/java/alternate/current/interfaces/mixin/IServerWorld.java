package alternate.current.interfaces.mixin;

import alternate.current.wire.WireHandler;

import net.minecraft.server.MinecraftServer;

public interface IServerWorld {

	MinecraftServer alternate_current$getServer();

	WireHandler alternate_current$getWireHandler();

}
