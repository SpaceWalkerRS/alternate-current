package alternate.current.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class BlockUtil extends BlockBehaviour {

	/** Directions in the order in which they are used for emitting shape updates. */
	public static final Direction[] SHAPE_UPDATE_ORDER = UPDATE_SHAPE_ORDER;

	private BlockUtil(Properties properties) {
		super(properties);
	}
}
