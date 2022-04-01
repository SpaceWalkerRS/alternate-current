package alternate.current.wire;

/**
 * This interface should be implemented by each wire block type. While Vanilla
 * only has one wire block type, they could add more in the future, and any mods
 * that add more wire block types that wish to take advantage of Alternate
 * Current's performance improvements should have those wire blocks implement
 * this interface.
 * 
 * @author Space Walker
 */
public interface WireBlock {

	public WireType getWireType();

}
