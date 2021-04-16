package alternate.current.interfaces.mixin;

import alternate.current.Wire;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWireBlock {
	
	public Wire getWire(World world, BlockPos pos, boolean orCreate, boolean updateConnections);
	
}
