package alternate.current.util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.Direction;

public abstract class BlockUtil extends AbstractBlock {
	
	/** Directions in the order in which they are used for emitting shape updates */
	public static final Direction[] DIRECTIONS = AbstractBlock.FACINGS;
	
	protected BlockUtil(Settings settings) {
		super(settings);
	}
}
