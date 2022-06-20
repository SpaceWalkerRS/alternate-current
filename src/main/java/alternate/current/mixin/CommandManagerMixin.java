package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.command.AlternateCurrentCommand;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandRegistry;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin extends CommandRegistry {

	@Inject(
		method="<init>",
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target = "Lnet/minecraft/command/AbstractCommand;setCommandProvider(Lnet/minecraft/command/CommandProvider;)V"
		)
	)
	private void registerCommands(CallbackInfo ci) {
		registerCommand(new AlternateCurrentCommand());
	}
}
