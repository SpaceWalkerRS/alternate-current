package alternate.current.MIN_IMPACT;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Notifier {
	
	public final BlockPos pos;
	public final Direction dir;
	
	public Notifier(BlockPos pos, Direction dir) {
		this.pos = pos;
		this.dir = dir;
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode(); // this preserves locationality
	}
}
