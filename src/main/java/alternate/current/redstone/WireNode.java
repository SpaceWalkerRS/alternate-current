package alternate.current.redstone;

import java.util.Collection;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A WireNode is a Node that represents a redstone wire in the world.
 * It stores all the information about the redstone wire that the
 * WireHandler needs to calculate power changes.
 * 
 * While regular Nodes are only used by and thus stored in the
 * WireHandler, WireNodes are stored in the WorldChunks. This is
 * done so that wire connections do not have to be re-calculated
 * each time a wire network is updated, which makes building the
 * network a lot faster.
 * 
 * @author Space Walker
 */
public class WireNode extends Node {
	
	public final WireConnectionManager connections;
	
	/** The power level this wire currently has in the world */
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
	
	public WireNode(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		super(world, wireBlock);
		
		this.connections = new WireConnectionManager(this);
		
		this.pos = pos.toImmutable();
		this.state = state;
		
		this.isWire = true;
		this.isSolidBlock = false;
		this.isRedstoneComponent = false;
		
		this.virtualPower = this.currentPower = this.wireBlock.getPower(this.world, this.pos, this.state);
	}
	
	@Override
	public Node update(BlockPos pos, BlockState state) {
		throw new UnsupportedOperationException("Cannot update a WireNode!");
	}
	
	@Override
	public WireNode asWire() {
		return this;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return this.wireBlock == wireBlock;
	}
	
	public void stateChanged(BlockState newState) {
		state = newState;
		currentPower = wireBlock.getPower(world, pos, state);
	}
	
	/**
	 * Tell connected wires that they should update their connections.
	 */
	public void updateConnectedWires() {
		updateNeighboringWires(connections.getAll(), WireConnectionManager.DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	/**
	 * Tell some collection of wires that they should update their connections
	 * 
	 * @param wires     a collection of positions of redstone wires
	 * @param maxDepth  the maximum depth to which these updates should propagate
	 */
	public void updateNeighboringWires(Collection<BlockPos> wires, int maxDepth) {
		for (BlockPos pos : wires) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, false);
			
			if (wire != null) {
				wire.connections.update(maxDepth);
			}
		}
	}
	
	public int nextPower() {
		return wireBlock.clampPower(virtualPower);
	}
	
	public boolean offerPower(int power, int iDir) {
		int min = wireBlock.getMinPower();
		
		if (virtualPower == min || power > virtualPower) {
			return setPower(power, iDir);
		}
		if (virtualPower < min) {
			flowIn |= (1 << iDir);
		} else {
			if (power > virtualPower) {
				return setPower(power, iDir);
			}
			if (power == virtualPower) {
				flowIn |= (1 << iDir);
			}
		}
		
		return false;
	}
	
	private boolean setPower(int power, int iDir) {
		virtualPower = power;
		flowIn = (1 << iDir);
		
		return true;
	}
}
