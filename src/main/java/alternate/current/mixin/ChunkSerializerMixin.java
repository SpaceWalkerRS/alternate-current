package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IChunkSection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.poi.PointOfInterestStorage;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	
	@Inject(
			method = "deserialize",
			at = @At(
					value = "RETURN"
			)
	)
	private static void onDeserialize(ServerWorld world, StructureManager structureManager, PointOfInterestStorage poiStorage, ChunkPos pos, CompoundTag chunkData, CallbackInfoReturnable<ProtoChunk> cir) {
		ProtoChunk chunk = cir.getReturnValue();
		CompoundTag levelData = chunkData.getCompound("Level");
		
		if (!levelData.contains(AlternateCurrentMod.MOD_ID)) {
			return;
		}
		
		CompoundTag acData = levelData.getCompound(AlternateCurrentMod.MOD_ID);
		ListTag wiresPerSection = acData.getList("WireNodes", 9);
		
		ChunkSection[] sections = chunk.getSectionArray();
		
		for (int index = 0; index < wiresPerSection.size(); index++) {
			if (index >= sections.length) {
				break;
			}
			
			ChunkSection section = sections[index];
			
			if (section == null) {
				continue;
			}
			
			ListTag wires = wiresPerSection.getList(index);
			((IChunkSection)section).readWireNbt(wires, world);
		}
	}
	
	@Inject(
			method = "serialize",
			at = @At(
					value = "RETURN"
			)
	)
	private static void onSerialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag chunkData = cir.getReturnValue();
		CompoundTag levelData = chunkData.getCompound("Level");
		
		CompoundTag acData = new CompoundTag();
		levelData.put(AlternateCurrentMod.MOD_ID, acData);
		
		ListTag wiresPerSection = new ListTag();
		acData.put("WireNodes", wiresPerSection);
		
		for (ChunkSection section : chunk.getSectionArray()) {
			if (section == null) {
				wiresPerSection.add(new ListTag());
			} else {
				ListTag wires = ((IChunkSection)section).getWireNbt();
				wiresPerSection.add(wires);
			}
		}
	}
}
