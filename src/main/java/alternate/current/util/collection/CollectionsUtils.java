package alternate.current.util.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CollectionsUtils {
	
	/**
	 * Returns a Collection that contains all elements that are
	 * present in {@code c1} or {@code c2} but not in both.
	 * 
	 * @param <T>
	 * @param c1 The first collection
	 * @param c2 The second collection
	 * @return A collection that contains all elements such that
	 * for each element {@code e} in the collection
	 * {@code c1.contains(e) != c2.contains(e)}.
	 */
	public static <T> Collection<T> difference(Collection<T> c1, Collection<T> c2) {
		Collection<T> difference = new HashSet<>();
		
		Set<T> temp1 = new HashSet<>(c1);
		Set<T> temp2 = new HashSet<>(c2);
		
		Iterator<T> it = temp1.iterator();
		
		while (it.hasNext()) {
			T e = it.next();
			
			if (temp2.remove(e)) {
				it.remove();
			}
		}
		
		difference.addAll(temp1);
		difference.addAll(temp2);
		
		return difference;
	}
}
