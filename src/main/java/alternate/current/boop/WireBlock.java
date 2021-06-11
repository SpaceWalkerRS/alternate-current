package alternate.current.boop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;

public interface WireBlock {
	
	public Block asBlock();
	
	public IntProperty getPowerProperty();
	
	public int getMinPower();
	
	public int getMaxPower();
	
	public boolean isOf(BlockState state);
	
}
