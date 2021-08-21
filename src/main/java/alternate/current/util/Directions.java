package alternate.current.util;

import net.minecraft.util.math.Direction;

public class Directions {
	
	public static final Direction[] ALL        = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN, Direction.UP };
	public static final Direction[] HORIZONTAL = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH };
	public static final Direction[] VERTICAL   = { Direction.DOWN, Direction.UP };
	
	// Indices for the arrays above
	public static final int WEST  = 0;
	public static final int NORTH = 1;
	public static final int EAST  = 2;
	public static final int SOUTH = 3;
	public static final int DOWN  = 4;
	public static final int UP    = 5;
	
	public static boolean isHorizontal(int iDir) {
		return iDir >= 0 && iDir <= 3;
	}
}
