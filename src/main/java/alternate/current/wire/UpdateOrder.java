package alternate.current.wire;

import java.util.Locale;
import java.util.function.Consumer;

import alternate.current.wire.WireHandler.Directions;
import alternate.current.wire.WireHandler.NodeProvider;

public enum UpdateOrder {

	HORIZONTAL_FIRST_OUTWARD(
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH, Directions.DOWN, Directions.UP },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST , Directions.DOWN, Directions.UP },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH, Directions.DOWN, Directions.UP },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST , Directions.DOWN, Directions.UP }
			
		},
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		}
	) {

		@Override
		public void forEachNeighbor(NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
			/*
			 * This iteration order is designed to be an extension of the Vanilla shape
			 * update order, and is determined as follows:
			 * <br>
			 * 1. Each neighbor is identified by the step(s) you must take, starting at the
			 * source, to reach it. Each step is 1 block, thus the position of a neighbor is
			 * encoded by the direction(s) of the step(s), e.g. (right), (down), (up, left),
			 * etc.
			 * <br>
			 * 2. Neighbors are iterated over in pairs that lie on opposite sides of the
			 * source.
			 * <br>
			 * 3. Neighbors are iterated over in order of their distance from the source,
			 * moving outward. This means they are iterated over in 3 groups: direct
			 * neighbors first, then diagonal neighbors, and last are the far neighbors that
			 * are 2 blocks directly out.
			 * <br>
			 * 4. The order within each group is determined using the following basic order:
			 * { front, back, right, left, down, up }. This order was chosen because it
			 * converts to the following order of absolute directions when west is said to
			 * be 'forward': { west, east, north, south, down, up } - this is the order of
			 * shape updates.
			 */

			int rightward = (forward + 1) & 0b11;
			int backward  = (forward + 2) & 0b11;
			int leftward  = (forward + 3) & 0b11;
			int downward  = Directions.DOWN;
			int upward    = Directions.UP;

			Node front = nodes.getNeighbor(source, forward);
			Node right = nodes.getNeighbor(source, rightward);
			Node back  = nodes.getNeighbor(source, backward);
			Node left  = nodes.getNeighbor(source, leftward);
			Node below = nodes.getNeighbor(source, downward);
			Node above = nodes.getNeighbor(source, upward);

			// direct neighbors (6)
			action.accept(front);
			action.accept(back);
			action.accept(right);
			action.accept(left);
			action.accept(below);
			action.accept(above);

			// diagonal neighbors (12)
			action.accept(nodes.getNeighbor(front, rightward));
			action.accept(nodes.getNeighbor(back, leftward));
			action.accept(nodes.getNeighbor(front, leftward));
			action.accept(nodes.getNeighbor(back, rightward));
			action.accept(nodes.getNeighbor(front, downward));
			action.accept(nodes.getNeighbor(back, upward));
			action.accept(nodes.getNeighbor(front, upward));
			action.accept(nodes.getNeighbor(back, downward));
			action.accept(nodes.getNeighbor(right, downward));
			action.accept(nodes.getNeighbor(left, upward));
			action.accept(nodes.getNeighbor(right, upward));
			action.accept(nodes.getNeighbor(left, downward));

			// far neighbors (6)
			action.accept(nodes.getNeighbor(front, forward));
			action.accept(nodes.getNeighbor(back, backward));
			action.accept(nodes.getNeighbor(right, rightward));
			action.accept(nodes.getNeighbor(left, leftward));
			action.accept(nodes.getNeighbor(below, downward));
			action.accept(nodes.getNeighbor(above, upward));
		}
	},
	HORIZONTAL_FIRST_INWARD(
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH, Directions.DOWN, Directions.UP },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST , Directions.DOWN, Directions.UP },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH, Directions.DOWN, Directions.UP },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST , Directions.DOWN, Directions.UP }
		},
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		}
	) {

		@Override
		public void forEachNeighbor(NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
			/*
			 * This iteration order is designed to be an inversion of the above update
			 * order, and is determined as follows:
			 * <br>
			 * 1. Each neighbor is identified by the step(s) you must take, starting at the
			 * source, to reach it. Each step is 1 block, thus the position of a neighbor is
			 * encoded by the direction(s) of the step(s), e.g. (right), (down), (up, left),
			 * etc.
			 * <br>
			 * 2. Neighbors are iterated over in pairs that lie on opposite sides of the
			 * source.
			 * <br>
			 * 3. Neighbors are iterated over in order of their distance from the source,
			 * moving inward. This means they are iterated over in 3 groups: neighbors that
			 * are 2 blocks directly out first, then diagonal neighbors, and last are direct
			 * neighbors.
			 * <br>
			 * 4. The order within each group is determined using the following basic order:
			 * { front, back, right, left, down, up }. This order was chosen because it
			 * converts to the following order of absolute directions when west is said to
			 * be 'forward': { west, east, north, south, down, up } - this is the order of
			 * shape updates.
			 */

			int rightward = (forward + 1) & 0b11;
			int backward  = (forward + 2) & 0b11;
			int leftward  = (forward + 3) & 0b11;
			int downward  = Directions.DOWN;
			int upward    = Directions.UP;

			Node front = nodes.getNeighbor(source, forward);
			Node right = nodes.getNeighbor(source, rightward);
			Node back  = nodes.getNeighbor(source, backward);
			Node left  = nodes.getNeighbor(source, leftward);
			Node below = nodes.getNeighbor(source, downward);
			Node above = nodes.getNeighbor(source, upward);

			// far neighbors (6)
			action.accept(nodes.getNeighbor(front, forward));
			action.accept(nodes.getNeighbor(back, backward));
			action.accept(nodes.getNeighbor(right, rightward));
			action.accept(nodes.getNeighbor(left, leftward));
			action.accept(nodes.getNeighbor(below, downward));
			action.accept(nodes.getNeighbor(above, upward));

			// diagonal neighbors (12)
			action.accept(nodes.getNeighbor(front, rightward));
			action.accept(nodes.getNeighbor(back, leftward));
			action.accept(nodes.getNeighbor(front, leftward));
			action.accept(nodes.getNeighbor(back, rightward));
			action.accept(nodes.getNeighbor(front, downward));
			action.accept(nodes.getNeighbor(back, upward));
			action.accept(nodes.getNeighbor(front, upward));
			action.accept(nodes.getNeighbor(back, downward));
			action.accept(nodes.getNeighbor(right, downward));
			action.accept(nodes.getNeighbor(left, upward));
			action.accept(nodes.getNeighbor(right, upward));
			action.accept(nodes.getNeighbor(left, downward));

			
			// direct neighbors (6)
			action.accept(front);
			action.accept(back);
			action.accept(right);
			action.accept(left);
			action.accept(below);
			action.accept(above);
		}
	},
	VERTICAL_FIRST_OUTWARD(
		new int[][] {
			new int[] { Directions.DOWN, Directions.UP, Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.DOWN, Directions.UP, Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.DOWN, Directions.UP, Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.DOWN, Directions.UP, Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		},
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		}
	) {

		@Override
		public void forEachNeighbor(NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
			/*
			 * This iteration order is designed to be the opposite of the Vanilla shape
			 * update order, and is determined as follows:
			 * <br>
			 * 1. Each neighbor is identified by the step(s) you must take, starting at the
			 * source, to reach it. Each step is 1 block, thus the position of a neighbor is
			 * encoded by the direction(s) of the step(s), e.g. (right), (down), (up, left),
			 * etc.
			 * <br>
			 * 2. Neighbors are iterated over in pairs that lie on opposite sides of the
			 * source.
			 * <br>
			 * 3. Neighbors are iterated over in order of their distance from the source,
			 * moving outward. This means they are iterated over in 3 groups: direct
			 * neighbors first, then diagonal neighbors, and last are the far neighbors that
			 * are 2 blocks directly out.
			 * <br>
			 * 4. The order within each group is determined using the following basic order:
			 * { down, up, front, back, right, left }. This order was chosen because it
			 * converts to the following order of absolute directions when west is said to
			 * be 'forward': { down, up west, east, north, south } - this is the order of
			 * shape updates, with the vertical directions moved to the front.
			 */

			int rightward = (forward + 1) & 0b11;
			int backward  = (forward + 2) & 0b11;
			int leftward  = (forward + 3) & 0b11;
			int downward  = Directions.DOWN;
			int upward    = Directions.UP;

			Node front = nodes.getNeighbor(source, forward);
			Node right = nodes.getNeighbor(source, rightward);
			Node back  = nodes.getNeighbor(source, backward);
			Node left  = nodes.getNeighbor(source, leftward);
			Node below = nodes.getNeighbor(source, downward);
			Node above = nodes.getNeighbor(source, upward);

			// direct neighbors (6)
			action.accept(below);
			action.accept(above);
			action.accept(front);
			action.accept(back);
			action.accept(right);
			action.accept(left);

			// diagonal neighbors (12)
			action.accept(nodes.getNeighbor(below, forward));
			action.accept(nodes.getNeighbor(above, backward));
			action.accept(nodes.getNeighbor(below, backward));
			action.accept(nodes.getNeighbor(above, forward));
			action.accept(nodes.getNeighbor(below, rightward));
			action.accept(nodes.getNeighbor(above, leftward));
			action.accept(nodes.getNeighbor(below, leftward));
			action.accept(nodes.getNeighbor(above, rightward));
			action.accept(nodes.getNeighbor(front, rightward));
			action.accept(nodes.getNeighbor(back, leftward));
			action.accept(nodes.getNeighbor(front, leftward));
			action.accept(nodes.getNeighbor(back, rightward));

			// far neighbors (6)
			action.accept(nodes.getNeighbor(below, downward));
			action.accept(nodes.getNeighbor(above, upward));
			action.accept(nodes.getNeighbor(front, forward));
			action.accept(nodes.getNeighbor(back, backward));
			action.accept(nodes.getNeighbor(right, rightward));
			action.accept(nodes.getNeighbor(left, leftward));
		}
	},
	VERTICAL_FIRST_INWARD(
		new int[][] {
			new int[] { Directions.DOWN, Directions.UP, Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.DOWN, Directions.UP, Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.DOWN, Directions.UP, Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.DOWN, Directions.UP, Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		},
		new int[][] {
			new int[] { Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
			new int[] { Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
			new int[] { Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
			new int[] { Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
		}
	) {

		@Override
		public void forEachNeighbor(NodeProvider nodes, Node source, int forward, Consumer<Node> action) {
			/*
			 * This iteration order is designed to be an inversion of the above update
			 * order, and is determined as follows:
			 * <br>
			 * 1. Each neighbor is identified by the step(s) you must take, starting at the
			 * source, to reach it. Each step is 1 block, thus the position of a neighbor is
			 * encoded by the direction(s) of the step(s), e.g. (right), (down), (up, left),
			 * etc.
			 * <br>
			 * 2. Neighbors are iterated over in pairs that lie on opposite sides of the
			 * source.
			 * <br>
			 * 3. Neighbors are iterated over in order of their distance from the source,
			 * moving inward. This means they are iterated over in 3 groups: neighbors that
			 * are 2 blocks directly out first, then diagonal neighbors, and last are direct
			 * neighbors.
			 * <br>
			 * 4. The order within each group is determined using the following basic order:
			 * { down, up, front, back, right, left }. This order was chosen because it
			 * converts to the following order of absolute directions when west is said to
			 * be 'forward': { down, up west, east, north, south } - this is the order of
			 * shape updates, with the vertical directions moved to the front.
			 */

			int rightward = (forward + 1) & 0b11;
			int backward  = (forward + 2) & 0b11;
			int leftward  = (forward + 3) & 0b11;
			int downward  = Directions.DOWN;
			int upward    = Directions.UP;

			Node front = nodes.getNeighbor(source, forward);
			Node right = nodes.getNeighbor(source, rightward);
			Node back  = nodes.getNeighbor(source, backward);
			Node left  = nodes.getNeighbor(source, leftward);
			Node below = nodes.getNeighbor(source, downward);
			Node above = nodes.getNeighbor(source, upward);

			// far neighbors (6)
			action.accept(nodes.getNeighbor(below, downward));
			action.accept(nodes.getNeighbor(above, upward));
			action.accept(nodes.getNeighbor(front, forward));
			action.accept(nodes.getNeighbor(back, backward));
			action.accept(nodes.getNeighbor(right, rightward));
			action.accept(nodes.getNeighbor(left, leftward));

			// diagonal neighbors (12)
			action.accept(nodes.getNeighbor(below, forward));
			action.accept(nodes.getNeighbor(above, backward));
			action.accept(nodes.getNeighbor(below, backward));
			action.accept(nodes.getNeighbor(above, forward));
			action.accept(nodes.getNeighbor(below, rightward));
			action.accept(nodes.getNeighbor(above, leftward));
			action.accept(nodes.getNeighbor(below, leftward));
			action.accept(nodes.getNeighbor(above, rightward));
			action.accept(nodes.getNeighbor(front, rightward));
			action.accept(nodes.getNeighbor(back, leftward));
			action.accept(nodes.getNeighbor(front, leftward));
			action.accept(nodes.getNeighbor(back, rightward));

			// direct neighbors (6)
			action.accept(below);
			action.accept(above);
			action.accept(front);
			action.accept(back);
			action.accept(right);
			action.accept(left);
		}
	};

	private final int[][] directNeighbors;
	private final int[][] cardinalNeighbors;

	private UpdateOrder(int[][] directNeighbors, int[][] cardinalNeighbors) {
		this.directNeighbors = directNeighbors;
		this.cardinalNeighbors = cardinalNeighbors;
	}

	public String id() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	public static UpdateOrder byId(String id) {
		return valueOf(id.toUpperCase(Locale.ENGLISH));
	}

	public int[] directNeighbors(int forward) {
		return directNeighbors[forward];
	}

	public int[] cardinalNeighbors(int forward) {
		return cardinalNeighbors[forward];
	}

	/**
	 * Iterate over all neighboring nodes of the given source node. The iteration
	 * order is built from relative directions around the source, depending on the
	 * given 'forward' direction. This is an effort to eliminate any directional
	 * biases that would be emerge in rotationally symmetric circuits if the update
	 * order was built from absolute directions around the source.
	 * <br>
	 * Each update order must include the source's direct neighbors, but further
	 * neighbors may not be included.
	 */
	public abstract void forEachNeighbor(NodeProvider nodes, Node source, int forward, Consumer<Node> action);

}
