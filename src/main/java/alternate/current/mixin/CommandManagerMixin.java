package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import alternate.current.AlternateCurrentMod;
import alternate.current.command.AlternateCurrentCommand;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
	
	@Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;
	
	@Inject(
			method="<init>",
			at = @At(
					value = "RETURN"
			)
	)
	private void registerCommands(CommandManager.RegistrationEnvironment environment, CallbackInfo ci) {
		if (AlternateCurrentMod.DEBUG) {
			AlternateCurrentCommand.registerCommand(dispatcher);
		}
	}
}
