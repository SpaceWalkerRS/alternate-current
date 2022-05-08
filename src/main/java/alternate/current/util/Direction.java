package alternate.current.util;

public enum Direction {

	DOWN(0, 1, 0, -1, 0),
	UP(1, 0, 0, 1, 0),
	NORTH(2, 3, 0, 0, -1),
	SOUTH(3, 2, 0, 0, 1),
	WEST(4, 5, -1, 0, 0),
	EAST(5, 4, 1, 0, 0);

	public static final Direction[] ALL;

	static {

		ALL = new Direction[values().length];

		for (Direction dir : values()) {
			ALL[dir.index] = dir;
		}
	}

	public final int index;
	public final int oppositeIndex;
	public final int offsetX;
	public final int offsetY;
	public final int offsetZ;

	private Direction(int index, int opposite, int offsetX, int offsetY, int offsetZ) {
		this.index = index;
		this.oppositeIndex = opposite;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
	}

	public static Direction fromIndex(int index) {
		if (index < 0 && index >= ALL.length) {
			return null;
		}

		return ALL[index];
	}

	public Direction getOpposite() {
		return ALL[oppositeIndex];
	}
}
