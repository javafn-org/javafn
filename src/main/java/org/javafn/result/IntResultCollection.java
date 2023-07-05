package org.javafn.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntResultCollection<E> {

	public static <EE> IntResultCollection<EE> empty() {
		return new IntResultCollection<>(Collections.emptyList(), new int[0]);
	}

	public static <EE> IntResultCollection<EE> singleton(final IntResult<EE> result) {
		return result.reduce(
				err -> new IntResultCollection<>(Collections.singletonList(err), new int[0]),
				ok -> new IntResultCollection<>(Collections.emptyList(), new int[]{ok} ));
	}

	public static <EE> IntResultCollection<EE> from(final List<EE> errs, final int[] oks) {
		return new IntResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks);
	}

	public static <EE> IntResultCollection<EE> from(final Optional<List<EE>> errs, final Optional<int[]> oks) {
		return new IntResultCollection<>(
				errs == null ? null : new ArrayList<>(errs.orElse(Collections.emptyList())),
				oks == null ? null : oks.orElse(new int[0]));
	}

	public static <EE> IntResultCollection<EE> from(final List<EE> errs, final int[] oks, List<IntResult<EE>> moreResults) {
		return new IntResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks)
				.addAll(moreResults.stream().collect(IntResult.collector()));
	}

	private final List<E> errs;
	private final int[] oks;

	IntResultCollection(final List<E> _errs, final int[] _oks) {
		errs = _errs == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_errs);
		oks = _oks == null
				? new int[0]
				: _oks;
	}

	/**
	 * return whether this collection contains any err types or not
	 */
	public boolean hasErrs() {
		return !errs.isEmpty();
	}

	/**
	 * return whether this collection contains any ok types or not
	 */
	public boolean hasOks() {
		return oks.length > 0;
	}

	/**
	 * return whether this collection contains both err and ok types, i.e., {@code hasErrs() && hasOks()}
	 */
	public boolean hasBoth() {
		return !errs.isEmpty() && oks.length > 0;
	}

	/**
	 * get the possibly empty list of err types
	 */
	public List<E> getErrs() {
		return errs;
	}

	/**
	 * get the possibly empty list of ok types
	 */
	public int[] getOks() {
		return oks;
	}

	public <NEWE> IntResultCollection<NEWE> mapErrs(final Function<List<E>, List<NEWE>> fn) {
		return new IntResultCollection<>(new ArrayList<>(fn.apply(errs)), oks);
	}

	public IntResultCollection<E> mapOks(final Function<int[], int[]> fn) {
		return new IntResultCollection<>(errs, fn.apply(oks));
	}

	public <NEWO> ResultCollection<E, NEWO> mapOksToObj(final Function<int[], List<NEWO>> fn) {
		return new ResultCollection<>(errs, new ArrayList<>(fn.apply(oks)));
	}

	public IntResultCollection<E> addErrs(final List<E> errs) {
		return new IntResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.oks);
	}

	public IntResultCollection<E> addOks(final int[] oks) {
		return new IntResultCollection<>(
				this.errs,
				IntStream.concat(
								Arrays.stream(this.oks),
								oks == null ? IntStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public IntResultCollection<E> addAll(final List<E> errs, final int[] oks) {
		return new IntResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				IntStream.concat(
								Arrays.stream(this.oks),
								oks == null ? IntStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public IntResultCollection<E> addAll(final IntResultCollection<E> other) {
		return new IntResultCollection<>(
				Stream.concat(this.errs.stream(), other.errs.stream()).collect(Collectors.toList()),
				IntStream.concat(Arrays.stream(this.oks), Arrays.stream(other.oks)).toArray());
	}

	public IntResultCollection<E> addAll(final List<IntResult<E>> moreResults) {
		return addAll(moreResults.stream().collect(IntResult.collector()));
	}

	public Result<List<E>, int[]> fold() {
		if (hasErrs()) return Result.err(errs);
		return Result.ok(oks);
	}

	public <U> Result<U, int[]> fold(final Function<List<E>, U> fnErr, final Function<int[], int[]> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <U, V> Result<U, V> foldToObj(final Function<List<E>, U> fnErr, final Function<int[], V> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <T> T reduce(final Function<List<E>, T> fnErr, final Function<int[], T> fnOk) {
		if (hasErrs()) return fnErr.apply(errs);
		return fnOk.apply(oks);
	}

	public <T, U, V> T reduce(
			final Function<List<E>, U> fnErr,
			final Function<int[], V> fnOk,
			final BiFunction<Optional<U>, Optional<V>, T> combiner) {
		return combiner.apply(
				errs.isEmpty() ? Optional.empty() : Optional.of(fnErr.apply(errs)),
				oks.length == 0 ? Optional.empty() : Optional.of(fnOk.apply(oks)));
	}

	public <R> R reduce(final BiFunction<List<E>, int[], R> fn)
	{ return fn.apply(errs, oks); }

	public static final class IntResultCollector<EE>
			implements Collector<IntResult<EE>, IntResultCollectionBuilder<EE>, IntResultCollection<EE>> {
		@Override public Supplier<IntResultCollectionBuilder<EE>> supplier () { return IntResultCollectionBuilder::new; }
		@Override public BiConsumer<IntResultCollectionBuilder<EE>, IntResult<EE>> accumulator ()
		{ return IntResultCollectionBuilder::add; }
		@Override public BinaryOperator<IntResultCollectionBuilder<EE>> combiner () { return IntResultCollectionBuilder::addAll; }
		@Override public Function<IntResultCollectionBuilder<EE>, IntResultCollection<EE>> finisher()
		{ return IntResultCollectionBuilder::collect; }
		@Override public Set<Characteristics> characteristics () { return Collections.emptySet(); }
	}

	private static final class IntResultCollectionBuilder<E> {
		private final Stream.Builder<E> errs = Stream.builder();
		private final IntStream.Builder oks = IntStream.builder();

		/** Add a single result to this collector */
		public void add(final IntResult<E> result) {
			if (result.isErr()) {
				errs.add(result.asErr().get());
			} else {
				oks.add(result.asOk().get());
			}
		}

		/** Add all elements from partial to this */
		public IntResultCollectionBuilder<E> addAll(final IntResultCollectionBuilder<E> partial) {
			partial.errs.build().forEach(errs::add);
			partial.oks.build().forEach(oks::add);
			return this;
		}

		public IntResultCollection<E> collect() {
			return new IntResultCollection<>(
					errs.build().collect(Collectors.toList()),
					oks.build().toArray());
		}
	}

}
