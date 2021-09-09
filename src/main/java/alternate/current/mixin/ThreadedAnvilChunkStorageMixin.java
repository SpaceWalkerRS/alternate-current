package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IBlockStorage;
import alternate.current.util.NbtUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
	
	@Inject(
			method = "method_1463",
			at = @At(
					value = "RETURN"
			)
	)
	private void onDeserialize(World world, CompoundTag levelData, CallbackInfoReturnable<Chunk> cir) {
		if (world.isClient || !levelData.contains(AlternateCurrentMod.MOD_ID)) {
			return;
		}
		
		Chunk chunk = cir.getReturnValue();
		CompoundTag acData = levelData.getCompound(AlternateCurrentMod.MOD_ID);
		ListTag wiresPerSection = acData.getList("WireNodes", 9);
		
		BlockStorage[] blockStorage = chunk.getBlockStorage();
		
		for (int index = 0; index < wiresPerSection.size(); index++) {
			if (index >= blockStorage.length) {
				break;
			}
			
			BlockStorage storage = blockStorage[index];
			
			if (storage == null) {
				continue;
			}
			
			ListTag wires = NbtUtil.getList(wiresPerSection, index);
			((IBlockStorage)storage).readWireNbt(wires, (ServerWorld)world);
		}
	}
	
	@Inject(
			method = "method_1464",
			at = @At(
					value = "RETURN"
			)
	)
	private void onSerialize(Chunk chunk, World world, CompoundTag levelData, CallbackInfo ci) {
		if (world.isClient) {
			return;
		}
		
		CompoundTag acData = new CompoundTag();
		levelData.put(AlternateCurrentMod.MOD_ID, acData);
		
		ListTag wiresPerSection = new ListTag();
		acData.put("WireNodes", wiresPerSection);
		
		for (BlockStorage storage : chunk.getBlockStorage()) {
			if (storage == null) {
				wiresPerSection.add(new ListTag());
			} else {
				ListTag wires = ((IBlockStorage)storage).getWireNbt();
				wiresPerSection.add(wires);
			}
		}
	}
}
