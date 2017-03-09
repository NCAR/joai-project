package org.apache.commons.collections.map;

import java.util.*;

/**
 *  Map implementation based on Apache Commons LinkedMap that maintains a sorted list of values for iteration.
 *  <p>
 *
 *  Code posted by Stefan Fußenegger was taken from http://techblog.molindo.at/2008/11/java-map-sorted-by-value.html
 *  on 6/16/2009.
 *
 * @author    Stefan Fußenegger
 */
public class ValueSortedMap extends LinkedMap {
	private final boolean _asc;


	/**
	 *  Constructor for the ValueSortedHashMap object
	 *
	 * @param  ascending  NOT YET DOCUMENTED
	 */
	public ValueSortedMap(final boolean ascending) {
		super(DEFAULT_CAPACITY);
		_asc = ascending;
	}

	// SNIP: some more constructors with initial capacity and the like

	/**
	 *  Adds a feature to the Entry attribute of the ValueSortedMap object
	 *
	 * @param  entry      The feature to be added to the Entry attribute
	 * @param  hashIndex  The feature to be added to the Entry attribute
	 */
	protected void addEntry(final HashEntry entry, final int hashIndex) {
		final LinkEntry link = (LinkEntry) entry;
		insertSorted(link);
		data[hashIndex] = entry;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  entry     NOT YET DOCUMENTED
	 * @param  newValue  NOT YET DOCUMENTED
	 */
	protected void updateEntry(final HashEntry entry,
	                                 final Object newValue) {
		entry.setValue(newValue);
		final LinkEntry link = (LinkEntry) entry;
		link.before.after = link.after;
		link.after.before = link.before;
		link.after = link.before = null;
		insertSorted(link);
	}


	private void insertSorted(final LinkEntry link) {
		LinkEntry cur = header;
		// iterate whole list, could (should?) be replaced with quicksearch
		// start at end to optimize speed for in-order insertions
		while ((cur = cur.before) != header && !insertAfter(cur, link)) {
		}
		link.after = cur.after;
		link.before = cur;
		cur.after.before = link;
		cur.after = link;
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  cur   NOT YET DOCUMENTED
	 * @param  link  NOT YET DOCUMENTED
	 * @return       NOT YET DOCUMENTED
	 */
	protected boolean insertAfter(final LinkEntry cur,
	                                    final LinkEntry link) {
		if (_asc) {
			return ((Comparable) cur.getValue())
				.compareTo(link.getValue()) <= 0;
		}
		else {
			return ((Comparable) cur.getValue())
				.compareTo(link.getValue()) >= 0;
		}
	}


	/**
	 *  Gets the ascending attribute of the ValueSortedMap object
	 *
	 * @return    The ascending value
	 */
	public boolean isAscending() {
		return _asc;
	}
}

