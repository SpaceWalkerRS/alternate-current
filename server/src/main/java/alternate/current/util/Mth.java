package alternate.current.util;

public class Mth {

	public static int clamp(int i, int min, int max) {
		return i <= min ? min : (i >= max ? max : i);
	}
}
