package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fast.redstone.Wire;
import fast.redstone.interfaces.mixin.IChunk;
import fast.redstone.interfaces.mixin.IChunkSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	
	@Override
	public Wire getWire(BlockPos pos) {
		ChunkSection section = getSection(pos.getY());
		
		if (section == null || ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).getWire(x, y, z);
	}
	
	@Override
	public Wire setWire(BlockPos pos, Wire wire) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return ((IChunkSection)section).setWire(x, y, z, wire);
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
