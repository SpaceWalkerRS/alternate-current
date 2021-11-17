package alternate.current.util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.Direction;

public abstract class BlockUtil extends AbstractBlock {
	
	/** Directions in the order in which they are used for emitting shape updates. */
	public static final Direction[] DIRECTIONS = AbstractBlock.DIRECTIONS;
	
	public static final int FLAG_NOTIFY_CLIENTS = 0b10;
	
	private BlockUtil(Settings settings) {
		super(settings);
	}
}
