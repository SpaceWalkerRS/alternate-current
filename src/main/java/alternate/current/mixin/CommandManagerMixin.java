package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.command.AlternateCurrentCommand;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandRegistry;

@Mixin(CommandManager.class)
public class CommandManagerMixin extends CommandRegistry {
	
	@Inject(
			method="<init>",
			at = @At(
					value = "RETURN"
			)
	)
	private void registerCommands(CallbackInfo ci) {
		if (AlternateCurrentMod.DEBUG) {
			registerCommand(new AlternateCurrentCommand());
		}
	}
}
