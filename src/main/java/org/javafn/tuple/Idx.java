package org.javafn.tuple;

import java.util.function.Function;

public class Idx<T> {

	@FunctionalInterface
	public interface IndexedFunction<TT, R> {
		R accept(TT tt);
	}

	public static class Indexed<TT> {
		int count = 0;

		Idx<TT> accept(final TT t) {
			return new Idx<>(count++, t);
		}
	}

	public static <TT> Function<TT, Idx<TT>> thisStream() {
		return new Indexed<TT>()::accept;
	}

	private final int i;
	private final T val;

	public Idx(final int _i, final T _val) {
		i = _i;
		val = _val;
	}

	public int i() { return i; }

	public T val() { return val; }

	public <R> Idx<R> map(final IndexedFunction<T, R> fn) {
		return new Idx<>(i, fn.accept(val));
	}

	public <R> R mapDropIdx(final IndexedFunction<T, R> fn) {
		return fn.accept(val);
	}

	public T dropIdx() {
		return val;
	}
}
