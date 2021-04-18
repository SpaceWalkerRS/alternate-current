package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import alternate.current.utils.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Node implements Comparable<Node> {
	
	public NodeType type;
	public BlockPos pos;
	public BlockState state;
	
	public Node[] neighbors;
	public List<Node> connectionsIn;
	public List<Node> connectionsOut;
	
	public boolean inNetwork;
	public boolean isPowerSource;
	
	public int power;
	
	public Node() {
		this.neighbors = new Node[Directions.ALL.length];
		this.connectionsIn = new ArrayList<>();
		this.connectionsOut = new ArrayList<>();
	}
	
	@Override
	public int compareTo(Node node) {
		int c = Integer.compare(node.power, power);
		
		if (c == 0) {
			c = Integer.compare(pos.getX(), node.pos.getX());
			
			if (c == 0) {
				c = Integer.compare(pos.getZ(), node.pos.getZ());
				
				if (c == 0) {
					c = Integer.compare(pos.getY(), node.pos.getY());
				}
			}
		}
		
		return c;
	}
	
	public void update(World world, BlockPos pos, BlockState state, Block wireBlock) {
		NodeType type;
		
		if (state.isOf(wireBlock)) {
			type = NodeType.WIRE;
		} else if (state.isSolidBlock(world, pos)) {
			type = NodeType.SOLID_BLOCK;
		} else if (state.emitsRedstonePower()) {
			type = NodeType.REDSTONE_COMPONENT;
		} else {
			type = NodeType.OTHER;
		}
		
		update(type, pos, state);
	}
	
	public void update(NodeType type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos;
		this.state = state;
		
		Arrays.fill(neighbors, null);
		this.connectionsIn.clear();
		this.connectionsOut.clear();
		
		this.inNetwork = false;
		this.isPowerSource = false;
		
		if (isWire()) {
			this.power = state.get(Properties.POWER);
		}
	}
	
	public boolean isWire() {
		return type == NodeType.WIRE;
	}
	
	public boolean isSolidBlock() {
		return type == NodeType.SOLID_BLOCK;
	}
	
	public boolean isRedstoneComponent() {
		return type == NodeType.REDSTONE_COMPONENT;
	}
	
	public void addConnection(Node node, boolean in, boolean out) {
		if (in) {
			connectionsIn.add(node);
		}
		if (out) {
			connectionsOut.add(node);
		}
	}
}
