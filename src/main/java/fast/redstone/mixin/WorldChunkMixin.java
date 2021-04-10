package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fast.redstone.interfaces.mixin.IChunk;
import fast.redstone.interfaces.mixin.IChunkSection;
import fast.redstone.v1.WireV1;
import fast.redstone.v2.WireV2;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	
	@Override
	public WireV1 getWireV1(BlockPos pos) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).getWireV1(x, y, z);
	}
	
	@Override
	public WireV1 setWireV1(BlockPos pos, WireV1 wire) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).setWireV1(x, y, z, wire);
	}
	
	@Override
	public WireV2 getWireV2(BlockPos pos) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).getWireV2(x, y, z);
	}
	
	@Override
	public WireV2 setWireV2(BlockPos pos, WireV2 wire) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).setWireV2(x, y, z, wire);
	}
	
	private ChunkSection getSection(int y) {
		if (y < 0) {
			return null;
		}
		
		int index = y >> 4;
		
		if (index >= sections.length) {
			return null;
		}
		
		ChunkSection section = sections[index];
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		return section;
	}
}
