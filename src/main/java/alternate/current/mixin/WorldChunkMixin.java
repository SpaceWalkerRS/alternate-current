package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.Wire;
import alternate.current.boop.WireBlock;
import alternate.current.boop.WireNode;
import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IChunkSection;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements Chunk, IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	@Shadow @Final private World world;
	
	@Override
	public void clearWires() {
		for (ChunkSection section : sections) {
			if (ChunkSection.isEmpty(section)) {
				continue;
			}
			
			((IChunkSection)section).clearWires();
		}
	}
	
	@Override
	public Wire getWire(BlockPos pos) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
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
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		WireNode wire = ((IChunkSection)section).getWire(wireBlock, pos);
		
		if (orCreate && wire == null) {
			BlockState state = getBlockState(pos);
			
			if (wireBlock.isOf(state)) {
				wire = new WireNode(wireBlock, world, pos, state);
				setWire(wire);
			}
		}
		
		return wire;
	}
	
	@Override
	public WireNode setWire(WireNode wire) {
		ChunkSection section = getSection(wire.pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		return ((IChunkSection)section).setWire(wire);
	}
	
	private ChunkSection getSection(int y) {
		if (y < 0) {
			return WorldChunk.EMPTY_SECTION;
		}
		
		int index = y >> 4;
		
		if (index >= sections.length) {
			return WorldChunk.EMPTY_SECTION;
		}
		
		ChunkSection section = sections[index];
		
		if (ChunkSection.isEmpty(section)) {
			return WorldChunk.EMPTY_SECTION;
		}
		
		return section;
	}
}
