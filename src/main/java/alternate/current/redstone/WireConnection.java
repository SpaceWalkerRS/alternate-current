package alternate.current.redstone;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;

public class WireConnection {
	
	public final BlockPos pos;
	public final int iDir;
	public final boolean in;
	public final boolean out;
	
	public WireConnection(BlockPos pos, int iDir, boolean in, boolean out) {
		this.pos = pos;
		this.iDir = iDir;
		this.in = in;
		this.out = out;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WireConnection) {
			WireConnection other = (WireConnection)obj;
			return pos.equals(other.pos) && in == other.in && out == other.out;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode();
	}
	
	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		
		nbt.put("pos", NbtHelper.fromBlockPos(pos));
		nbt.putInt("dir", iDir);
		nbt.putBoolean("in", in);
		nbt.putBoolean("out", out);
		
		return nbt;
	}
	
	public static WireConnection fromNbt(NbtCompound nbt) {
		BlockPos pos = NbtHelper.toBlockPos(nbt.getCompound("pos"));
		int iDir = nbt.getInt("dir");
		boolean in = nbt.getBoolean("in");
		boolean out = nbt.getBoolean("out");
		
		return new WireConnection(pos, iDir, in, out);
	}
}
