package alternate.current.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.math.BlockPos;

public class NbtUtil {
	
	public static CompoundTag posToTag(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		
		return tag;
	}
	
	public static BlockPos tagToPos(CompoundTag tag) {
		int x = tag.getInt("x");
		int y = tag.getInt("y");
		int z = tag.getInt("z");
		
		return new BlockPos(x, y, z);
	}
	
	public static ListTag getList(ListTag nbt, int index) {
		Tag tag = nbt.get(index);
		
		if (tag.getType() == nbt.getType()) {
			return (ListTag)tag;
		}
		
		return new ListTag();
	}
}
