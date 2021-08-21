package alternate.current.MIN_IMPACT.mixin;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import alternate.current.AlternateCurrentMod;
import alternate.current.PerformanceMode;
import alternate.current.MIN_IMPACT.Notifier;
import alternate.current.util.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {
	
	@Inject(
			method = "update",
			cancellable = true,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"
			)
	)
	private void onUpdateInjectBeforeNewHashSet(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (AlternateCurrentMod.MODE == PerformanceMode.MIN_IMPACT) {
			Set<Notifier> notifiers = new HashSet<>();
			
			Notifier self = new Notifier(pos, null);
			notifiers.add(self);
			
			for (Direction dir : Directions.ALL) {
				Notifier neighbor = new Notifier(pos.offset(dir), dir);
				notifiers.add(neighbor);
			}
			
			Block block = (RedstoneWireBlock)(Object)this;
			
			for (Notifier notifier : notifiers) {
				if (notifier == self) {
					world.updateNeighborsAlways(pos, block);
				} else {
					world.updateNeighborsExcept(notifier.pos, block, notifier.dir.getOpposite());
				}
			}
			
			ci.cancel();
		}
	}
}
