package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IChunkSection;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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
	private static void onDeserialize(ServerWorld world, StructureManager structureManager, PointOfInterestStorage poiStorage, ChunkPos pos, NbtCompound chunkData, CallbackInfoReturnable<ProtoChunk> cir) {
		ProtoChunk chunk = cir.getReturnValue();
		NbtCompound levelData = chunkData.getCompound("Level");
		
		if (!levelData.contains(AlternateCurrentMod.MOD_ID)) {
			return;
		}
		
		NbtCompound acData = levelData.getCompound(AlternateCurrentMod.MOD_ID);
		NbtList wiresPerSection = acData.getList("WireNodes", 9);
		
		ChunkSection[] sections = chunk.getSectionArray();
		
		for (int index = 0; index < wiresPerSection.size(); index++) {
			if (index >= sections.length) {
				break;
			}
			
			ChunkSection section = sections[index];
			
			if (section == null) {
				continue;
			}
			
			NbtList wires = wiresPerSection.getList(index);
			((IChunkSection)section).readWireNbt(wires, world);
		}
	}
	
	@Inject(
			method = "serialize",
			at = @At(
					value = "RETURN"
			)
	)
	private static void onSerialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<NbtCompound> cir) {
		NbtCompound chunkData = cir.getReturnValue();
		NbtCompound levelData = chunkData.getCompound("Level");
		
		NbtCompound acData = new NbtCompound();
		levelData.put(AlternateCurrentMod.MOD_ID, acData);
		
		NbtList wiresPerSection = new NbtList();
		acData.put("WireNodes", wiresPerSection);
		
		for (ChunkSection section : chunk.getSectionArray()) {
			if (section == null) {
				wiresPerSection.add(new NbtList());
			} else {
				NbtList wires = ((IChunkSection)section).getWireNbt();
				wiresPerSection.add(wires);
			}
		}
	}
}
