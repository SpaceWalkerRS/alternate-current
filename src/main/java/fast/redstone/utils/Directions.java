package fast.redstone.utils;

import net.minecraft.util.math.Direction;

public class Directions {
	
	public static final Direction[] ALL = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP };
	public static final Direction[] HORIZONTAL = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH };
	public static final Direction[] VERTICAL = new Direction[] { Direction.DOWN, Direction.UP };
	
	// Indices for the 'ALL' array
	public static final int WEST = 0;
	public static final int EAST = 1;
	public static final int NORTH = 2;
	public static final int SOUTH = 3;
	public static final int DOWN = 4;
	public static final int UP = 5;
	
}
