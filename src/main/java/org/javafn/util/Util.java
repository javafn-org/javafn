package org.javafn.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {


	/**
	 * Return a new list containing all the elements from src with toAppend added, without modifying src.
	 * The returned list is unmodifiable.
	 */
	public static <T> List<T> append(final List<T> src, T toAppend) {
		final List<T> mut = new ArrayList<>(src);
		mut.add(toAppend);
		return List.copyOf(mut);
	}

	/**
	 * Return a new list containing all the elements from src and toAppend, without modifying either list.
	 * The returned list is unmodifiable.
	 */
	public static <T> List<T> append(final List<T> src, List<T> toAppend) {
		final List<T> mut = new ArrayList<>(src);
		mut.addAll(toAppend);
		return List.copyOf(mut);
	}

	/**
	 * Return a new set containing all the elements from src with toAppend added, without modifying src.
	 * The returned set is unmodifiable.
	 */
	public static <T> Set<T> append(final Set<T> src, T toAppend) {
		final Set<T> mut = new HashSet<>(src);
		mut.add(toAppend);
		return Set.copyOf(mut);
	}

	/**
	 * Return a new set containing all the elements from src and toAppend, without modifying either set.
	 * The returned set is unmodifiable.
	 */
	public static <T> Set<T> append(final Set<T> src, Set<T> toAppend) {
		final Set<T> mut = new HashSet<>(src);
		mut.addAll(toAppend);
		return Set.copyOf(mut);
	}
}
