package alternate.current.mixin;

import java.util.Collection;
import java.util.Collections;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IChunk;
import alternate.current.redstone.WireNode;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
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
		NbtList wireNodes = acData.getList("WireNodes", 10);
		
		for (int index = 0; index < wireNodes.size(); index++) {
			NbtCompound wireData = wireNodes.getCompound(index);
			WireNode wire = WireNode.fromNbt(wireData, world);
			
			wire.stateChanged(chunk.getBlockState(wire.pos));
			((IChunk)chunk).placeWire(wire);
		}
	}
	
	@Inject(
			method = "serialize",
			at = @At(
					value = "RETURN"
			)
	)
	private static void onSerialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<NbtCompound> cir) {
		Collection<WireNode> wires = Collections.emptyList();
		
		if (wires.isEmpty()) {
			return;
		}
		
		NbtCompound chunkData = cir.getReturnValue();
		NbtCompound levelData = chunkData.getCompound("Level");
		
		NbtCompound acData = new NbtCompound();
		levelData.put(AlternateCurrentMod.MOD_ID, acData);
		
		NbtList wireNodes = new NbtList();
		acData.put("WireNodes", wireNodes);
		
		for (WireNode wire : wires) {
			NbtCompound wireData = new NbtCompound();
			wire.toNbt(wireData);
			wireNodes.add(wireData);
		}
	}
}
