package com.riverflows.data;

import java.util.Iterator;
import java.util.List;

/**
 * Class that encapsulates both a page of a result set and the length of the
 * entire result set.
 * @author robin
 *
 * @param <T>
 */
public class Page<T> implements Iterable<T>{
	/**
	 * null if no result count was specified in request, in which case you can
	 * use pageElements.size() instead
	 */
	public final Integer totalElementCount;
	public final List<T> pageElements;
	
	public Page(List<T> pageElements, Integer totalElementCount) {
		this.totalElementCount = totalElementCount;
		this.pageElements = pageElements;
	}
	
	public Iterator<T> iterator() {
		return pageElements.iterator();
	}
}
