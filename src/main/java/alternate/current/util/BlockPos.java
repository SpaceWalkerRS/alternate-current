package alternate.current.util;

public class BlockPos {

	public final int x;
	public final int y;
	public final int z;

	public BlockPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlockPos) {
			BlockPos pos = (BlockPos)obj;
			return pos.x == x && pos.y == y && pos.z == z;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return x + 31 * (y + 31 * z);
	}

	public BlockPos offset(Direction dir) {
		return offset(dir, 1);
	}

	public BlockPos offset(Direction dir, int dist) {
		return offset(dist * dir.offsetX, dist * dir.offsetY, dist * dir.offsetZ);
	}

	public BlockPos offset(int offsetX, int offsetY, int offsetZ) {
		return new BlockPos(x + offsetX, y + offsetY, z + offsetZ);
	}
}
