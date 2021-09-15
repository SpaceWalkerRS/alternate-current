package alternate.current.redstone;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * A WireNode is a Node that represents a redstone wire in the world.
 * It stores all the information about the redstone wire that the
 * WireHandler needs to calculate power changes.
 * 
 * @author Space Walker
 */
public class WireNode extends Node {
	
	public final WireConnectionManager connections;
	
	/** The power level this wire currently holds in the world */
	public int currentPower;
	/**
	 * While calculating power changes for a network, this field
	 * is used to keep track of the power level this wire should
	 * have.
	 */
	public int virtualPower;
	/** The power level received from non-wire components */
	public int externalPower;
	/**
	 * A 4-bit number that keeps track of the power flow of the
	 * wires that give this wire its power level.
	 */
	public int flowIn;
	/** The direction of power flow, based on the incoming flow */
	public int flowOut;
	public boolean shouldBreak;
	public boolean removed;
	public boolean prepared;
	public boolean inNetwork;
	
	public WireNode(WireBlock wireBlock, WorldAccess world, BlockPos pos, BlockState state) {
		super(wireBlock, world);
		
		this.pos = pos.toImmutable();
		this.state = state;
		
		this.connections = new WireConnectionManager(this);
		
		this.virtualPower = this.currentPower = this.wireBlock.getPower(this.world, this.pos, this.state);
	}
	
	@Override
	public Node update(BlockPos pos, BlockState state) {
		throw new UnsupportedOperationException("Cannot update a WireNode!");
	}
	
	@Override
	public boolean isWire() {
		return true;
	}
	
	@Override
	public WireNode asWire() {
		return this;
	}
	
	public int nextPower() {
		return wireBlock.clampPower(virtualPower);
	}
	
	public boolean offerPower(int power, int iDir) {
		if (power == virtualPower) {
			flowIn |= (1 << iDir);
			return false;
		}
		if (power > virtualPower) {
			virtualPower = power;
			flowIn = (1 << iDir);
			
			return true;
		}
		
		return false;
	}
	
	public boolean updateState() {
		if (removed) {
			return true;
		}
		
		state = world.getBlockState(pos);
		
		if (shouldBreak) {
			return world.breakBlock(pos, state);
		}
		
		currentPower = wireBlock.clampPower(virtualPower);
		state = wireBlock.updatePowerState(world, pos, state, currentPower);
		
		return world.setWireState(pos, state);
	}
}
