package alternate.current.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CollectionsUtils {
	
	/**
	 * Returns a Collection that contains all elements
	 * that are present in {@code c1} or {@code c2} but
	 * not in both.
	 * @param <T>
	 * @param c1 The first collection
	 * @param c2 The second collection
	 * @return A collection that contains all elements 
	 * such that for each element {@code e} in the collection
	 * {@code c1.contains(e) != c2.contains(e)}.
	 */
	public static <T> Collection<T> difference(Collection<T> c1, Collection<T> c2) {
		Collection<T> difference = new HashSet<>();
		
		Set<T> temp = new HashSet<>(c1);
		
		for (T e2 : c2) {
			boolean found = false;
			
			Iterator<T> it = temp.iterator();
			while (it.hasNext()) {
				T e1 = it.next();
				
				if (e1.equals(e2)) {
					found = true; // This element is common between the two collections
					it.remove();
				}
			}
			
			if (!found) {
				difference.add(e2);
			}
		}
		
		difference.addAll(temp);
		
		return difference;
	}
}
