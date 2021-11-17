package alternate.current.redstone;

import java.util.Arrays;
import java.util.function.BiConsumer;

import alternate.current.redstone.WireHandler.Directions;

public class WireConnectionManager {
	
	/**
	 * The number of bits allocated to store the start indices
	 * of connections in any cardinal direction.
	 */
	private static final int BITS = 4;
	private static final int MASK = (1 << BITS) - 1;
	
	/** The owner of these connections. */
	public final WireNode wire;
	
	/** All connections to other wires. */
	public WireConnection[] all;
	
	/** The total number of connections. */
	public int count;
	/** The number of connections per cardinal direction. */
	private int indices;
	
	/**
	 * A 4 bit number that encodes which in direction(s) the owner
	 * has connections to other wires.
	 */
	private int flowTotal;
	/** The direction of flow  based connections to other wires. */
	public int flow;
	
	public WireConnectionManager(WireNode wire) {
		this.wire = wire;
		this.all = new WireConnection[Directions.HORIZONTAL.length];
		
		this.count = 0;
		this.indices = 0;
		
		this.flowTotal = 0;
		this.flow = -1;
	}
	
	public void set(BiConsumer<ConnectionConsumer, Integer> setter) {
		if (count > 0) {
			clear();
		}
		
		for (int iDir = 0; iDir < Directions.HORIZONTAL.length; iDir++) {
			setIndex(iDir, count);
			setter.accept(this::add, iDir);
		}
		
		setIndex(Directions.HORIZONTAL.length, count);
	}
	
	private void clear() {
		Arrays.fill(all, null);
		
		count = 0;
		indices = 0;
		
		flowTotal = 0;
		flow = -1;
	}
	
	private void add(WireNode wire, int iDir, boolean in, boolean out) {
		addConnection(new WireConnection(wire, iDir, in, out));
	}
	
	private void addConnection(WireConnection connection) {
		if (count == all.length) {
			all = doubleSize(all);
		}
		
		all[count++] = connection;
		
		flowTotal |= (1 << connection.iDir);
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
	}
	
	/**
	 * Retrieve the start index of all connections in the given direction.
	 */
	public int start(int iDir) {
		return getIndex(iDir);
	}
	
	/**
	 * Retrieve the end index of all connections in the given direction.
	 */
	public int end(int iDir) {
		return getIndex(iDir + 1);
	}
	
	private void setIndex(int i, int index) {
		indices |= (index & MASK) << (i * BITS);
	}
	
	private int getIndex(int i) {
		return (indices >> (i * BITS)) & MASK;
	}
	
	private static WireConnection[] doubleSize(WireConnection[] array) {
		WireConnection[] newArray = new WireConnection[array.length << 1];
		
		for (int index = 0; index < array.length; index++) {
			newArray[index] = array[index];
		}
		
		return newArray;
	}
	
	@FunctionalInterface
	public interface ConnectionConsumer {
		
		public void add(WireNode wire, int iDir, boolean in, boolean out);
		
	}
}
