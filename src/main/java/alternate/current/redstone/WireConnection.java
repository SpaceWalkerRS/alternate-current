package alternate.current.redstone;

import alternate.current.util.NbtUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;

/**
 * This class represents a connection between some WireNode (the
 * 'owner') and a neighboring WireNode. Two wires are considered
 * to be connected if power can flow from one wire to the other
 * (and/or vice versa).
 * 
 * @author Space Walker
 */
public class WireConnection {
	
	/** Position of the connected wire */
	public final BlockPos pos;
	/** Cardinal direction to the connected wire */
	public final int iDir;
	/** True if the connected wire can provide power to the owner of the connection */
	public final boolean in;
	/** True if the connected wire can accept power from the owner of the connection */
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
	
	public CompoundTag toNbt() {
		CompoundTag nbt = new CompoundTag();
		
		nbt.put("pos", NbtUtil.posToTag(pos));
		nbt.putInt("dir", iDir);
		nbt.putBoolean("in", in);
		nbt.putBoolean("out", out);
		
		return nbt;
	}
	
	public static WireConnection fromNbt(CompoundTag nbt) {
		BlockPos pos = NbtUtil.tagToPos(nbt.getCompound("pos"));
		int iDir = nbt.getInt("dir");
		boolean in = nbt.getBoolean("in");
		boolean out = nbt.getBoolean("out");
		
		return new WireConnection(pos, iDir, in, out);
	}
}
