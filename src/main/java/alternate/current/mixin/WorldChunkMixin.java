package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.interfaces.mixin.IWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
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
	
	@Inject(
			method = "setBlockState",
			locals = LocalCapture.CAPTURE_FAILHARD,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/BlockState;onStateReplaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V"
			)
	)
	private void onSetBlockStateInjectBeforeStateReplaced(BlockPos pos, BlockState newState, boolean moved, CallbackInfoReturnable<BlockState> cir, int chunkX, int y, int chunkZ, ChunkSection chunkSection, boolean isEmpty, BlockState prevState, Block newBlock, Block prevBlock) {
		if (world.isClient() || world.isDebugWorld()) {
			return;
		}
		
		if (AlternateCurrentMod.ENABLED) {
			boolean wasWire = prevBlock instanceof WireBlock;
			boolean isWire = newBlock instanceof WireBlock;
			
			if (newBlock == prevBlock) {
				if (wasWire) {
					WireBlock wireBlock = (WireBlock)prevBlock;
					WireNode wire = getWire(wireBlock, pos, true);
					
					if (wire != null) {
						wire.updateState(newState);
						
						if (wireBlock.shouldUpdateConnections(world, pos, prevState, newState, wire)) {
							wire.updateConnections();
						}
					}
				}
			} else {
				if (wasWire) {
					WireBlock wireBlock = (WireBlock)prevBlock;
					WireNode wire = getWire(wireBlock, pos, false);
					
					if (wire != null) {
						setWire(wireBlock, pos, null);
						wireBlock.onWireRemoved(world, pos, prevState, wire, moved);
					}
				}
				if (isWire) {
					WireBlock wireBlock = (WireBlock)newBlock;
					WireNode wire = wireBlock.createWire(world, pos, newState);
					
					setWire(wire.wireBlock, wire.pos, wire);
					wireBlock.onWireAdded(world, pos, newState, wire, moved);
				}
			}
			
			if (!wasWire && !isWire) {
				((IWorld)world).updateWireConnectionsAround(pos);
			}
		}
	}
	
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
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		WireNode wire = ((IChunkSection)section).getWire(wireBlock, pos);
		
		if (orCreate && wire == null) {
			BlockState state = getBlockState(pos);
			
			if (wireBlock.isOf(state)) {
				wire = wireBlock.createWire(world, pos, state);
				setWire(wireBlock, pos, wire);
			}
		}
		
		return wire;
	}
	
	@Override
	public WireNode setWire(WireBlock wireBlock, BlockPos pos, WireNode wire) {
		ChunkSection section = getSection(pos.getY());
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		return ((IChunkSection)section).setWire(wireBlock, pos, wire);
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
