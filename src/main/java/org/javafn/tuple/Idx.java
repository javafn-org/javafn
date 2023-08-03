package org.javafn.tuple;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Add an index to each element in a stream.  Similar to
 * <a href="https://doc.rust-lang.org/std/iter/trait.Iterator.html#method.enumerate">Rust's enumerate</a>
 * or <a href="https://docs.python.org/3/library/functions.html#enumerate">Python's enumerate</a>.
 * This class can be compared to the JavaFn {@link Pair} except it is specialized to not box the integer index.
 * <pre>{@code
 * someStream.map(Idx.enumerate())
 *      .peek(Idx.Peek( (i, item) -> println("Completing item " + i ))
 *      ...;
 * }</pre>
 * or equivalently,
 * <pre>{@code
 * Idx.enumerate(someStream)
 *      .peek(Idx.Peek( (i, item) -> println("Completing item " + i ))
 *      ...;
 * }</pre>
 * @param <T>
 */
public class Idx<T> {

	@FunctionalInterface public interface IndexedFunction<TT, R> { R apply(int i, TT tt); }

	@FunctionalInterface public interface IndexedConsumer<TT> { void accept(int i, TT tt); }
	@FunctionalInterface public interface IndexedPredicate<TT> { boolean test(int i, TT tt); }

	public static class StreamIndexer<TT> {
		int count = 0;

		public Idx<TT> accept(final TT t) {
			return new Idx<>(count++, t);
		}
	}
	public static class ParallelStreamIndexer<TT> {
		AtomicInteger count = new AtomicInteger(0);

		public Idx<TT> accept(final TT t) {
			return new Idx<>(count.getAndIncrement(), t);
		}
	}

	public static <TT, R> Function<Idx<TT>, Idx<R>> Map(final IndexedFunction<TT, R> fn) { return idx -> idx.map(fn); }
	public static <TT, R> Function<Idx<TT>, Idx<R>> Map(final Function<TT, R> fn) { return idx -> idx.map(fn); }
	public static <TT, R> Function<Idx<TT>, R> MapDropIdx(final IndexedFunction<TT, R> fn) { return idx -> idx.mapDropIdx(fn); }
	public static <TT> Consumer<Idx<TT>> Peek(final IndexedConsumer<TT> fn) { return idx -> idx.peek(fn); }
	public static <TT> Predicate<Idx<TT>> Filter(final IndexedPredicate<TT> fn) { return idx -> idx.filter(fn); }

	public static <TT> Stream<Idx<TT>> enumerate(final Stream<TT> stream) {
		return stream.map(new StreamIndexer<TT>()::accept);
	}

	public static <TT> Function<TT, Idx<TT>> enumerate() {
		return new StreamIndexer<TT>()::accept;
	}

	public static <TT> Stream<Idx<TT>> enumerateParallel(final Stream<TT> stream) {
		return stream.map(new ParallelStreamIndexer<TT>()::accept);
	}

	public static <TT> Function<TT, Idx<TT>> enumerateParallel() {
		return new ParallelStreamIndexer<TT>()::accept;
	}

	private final int i;
	private final T val;

	public Idx(final int _i, final T _val) {
		i = _i;
		val = _val;
	}

	public int i() { return i; }
	public T val() { return val; }

	/**
	 * Map the value in this Idx to a new type and return a new Idx with the same index and the new value.
	 */
	public <R> Idx<R> map(final IndexedFunction<T, R> fn) { return new Idx<>(i, fn.apply(i, val)); }

	/**
	 * Map the value in this Idx to a new type and return a new Idx with the same index and the new value.
	 */
	public <R> Idx<R> map(final Function<T, R> fn) { return new Idx<>(i, fn.apply(val)); }
	/**
	 * Map the value in this Idx to a new type and return the new value, dropping the index.
	 */
	public <R> R mapDropIdx(final IndexedFunction<T, R> fn) { return fn.apply(i, val); }
	/**
	 * Inspect this Idx but do not update it.
	 */
	public Idx<T> peek(final IndexedConsumer<T> fn) { fn.accept(i, val); return this; }
	/**
	 * Perform the supplied Predicate on this Idx.
	 */
	public boolean filter(final IndexedPredicate<T> fn) { return fn.test(i, val); }
	/**
	 * Inspect this Idx but do not update it, and do not return it for further processing.
	 */
	public void forEach(final IndexedConsumer<T> fn) { fn.accept(i, val); }

	/**
	 * Return the wrapped value and drop the index.
	 */
	public T dropIdx() { return val; }

	@Override public int hashCode() { return Objects.hash(i, val); }
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Idx) {
			final Idx<?> that = (Idx<?>) obj;
			return i == that.i && Objects.equals(val, that.val);
		}
		return false;
	}
	@Override public String toString() { return "([" + i + "]:" + val.toString() + ")"; }
}
