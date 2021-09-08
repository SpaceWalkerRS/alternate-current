package alternate.current.redstone;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
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
