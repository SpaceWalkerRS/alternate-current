package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.command.AlternateCurrentCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.handler.CommandManager;
import net.minecraft.server.command.handler.CommandRegistry;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin extends CommandRegistry {

	@Inject(
		method="<init>",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/server/command/Command;setListener(Lnet/minecraft/server/command/handler/CommandListener;)V"
		)
	)
	private void registerCommands(MinecraftServer server, CallbackInfo ci) {
		register(new AlternateCurrentCommand());
	}
}
