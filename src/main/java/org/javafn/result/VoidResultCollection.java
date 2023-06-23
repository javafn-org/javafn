package org.javafn.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VoidResultCollection<E> {

	public static <EE> VoidResultCollection<EE> empty() {
		return new VoidResultCollection<>(Collections.emptyList(), 0);
	}

	public static <EE> VoidResultCollection<EE> singleton(final VoidResult<EE> result) {
		return result.reduce(
				err -> new VoidResultCollection<>(Collections.singletonList(err), 0),
				() -> new VoidResultCollection<>(Collections.emptyList(), 1 ));
	}

	public static <EE> VoidResultCollection<EE> from(final List<EE> errs, final int numOks) {
		return new VoidResultCollection<>(errs == null ? null : new ArrayList<>(errs), numOks);
	}

	public static <EE> VoidResultCollection<EE> from(final Optional<List<EE>> errs, final OptionalInt numOks) {
		return new VoidResultCollection<>(
				errs == null ? null : new ArrayList<>(errs.orElse(Collections.emptyList())),
				numOks == null ? 0 : numOks.orElse(0));
	}

	public static <EE> VoidResultCollection<EE> from(final List<EE> errs, final int numOks, List<VoidResult<EE>> moreResults) {
		return new VoidResultCollection<>(errs == null ? null : new ArrayList<>(errs), numOks)
				.addAll(moreResults.stream().collect(VoidResult.collector()));
	}

	private final List<E> errs;
	private final int numOks;

	VoidResultCollection(final List<E> _errs, final int _numOks) {
		errs = _errs == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_errs);
		numOks = _numOks;
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
		return numOks > 0;
	}

	/**
	 * return whether this collection contains both err and ok types, i.e., {@code hasErrs() && hasOks()}
	 */
	public boolean hasBoth() {
		return !errs.isEmpty() && numOks > 0;
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
	public int getNumOks() {
		return numOks;
	}

	public <NEWE> VoidResultCollection<NEWE> mapErrs(final Function<List<E>, List<NEWE>> fn) {
		return new VoidResultCollection<>(new ArrayList<>(fn.apply(errs)), numOks);
	}

	public <NEWO> ResultCollection<E, NEWO> mapOksToObj(final IntFunction<List<NEWO>> fn) {
		return new ResultCollection<>(errs, new ArrayList<>(fn.apply(numOks)));
	}

	public VoidResultCollection<E> addErrs(final List<E> errs) {
		return new VoidResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.numOks);
	}

	public VoidResultCollection<E> addOks(final int numMoreOks) {
		return new VoidResultCollection<>(this.errs, this.numOks + numMoreOks);
	}

	public VoidResultCollection<E> addAll(final List<E> errs, final int numMoreOks) {
		return new VoidResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.numOks + numMoreOks);
	}

	public VoidResultCollection<E> addAll(final VoidResultCollection<E> other) {
		return new VoidResultCollection<>(
				Stream.concat(this.errs.stream(), other.errs.stream()).collect(Collectors.toList()),
				this.numOks + other.numOks);
	}

	public VoidResultCollection<E> addAll(final List<VoidResult<E>> moreResults) {
		return addAll(moreResults.stream().collect(VoidResult.collector()));
	}

	public <U> VoidResult<U> fold(final Function<List<E>, U> errFn) {
		if (hasErrs()) return VoidResult.err(errFn.apply(errs));
		return VoidResult.ok();
	}

	public IntResult<List<E>> fold() {
		if (hasErrs()) return IntResult.err(errs);
		return IntResult.ok(numOks);
	}

	public <U, V> Result<U, V> foldToObj(final Function<List<E>, U> fnErr, final IntFunction<V> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(numOks));
	}

	public <T> T reduce(final Function<List<E>, T> fnErr, final IntFunction<T> fnOk) {
		if (hasErrs()) return fnErr.apply(errs);
		return fnOk.apply(numOks);
	}

	public <T, U, V> T reduce(
			final Function<List<E>, U> fnErr,
			final IntFunction<V> fnOk,
			final BiFunction<Optional<U>, Optional<V>, T> combiner) {
		return combiner.apply(
				errs.isEmpty() ? Optional.empty() : Optional.of(fnErr.apply(errs)),
				numOks == 0 ? Optional.empty() : Optional.of(fnOk.apply(numOks)));
	}
	public static final class VoidResultCollector<EE>
			implements Collector<VoidResult<EE>, VoidResultCollectionBuilder<EE>, VoidResultCollection<EE>> {
		@Override public Supplier<VoidResultCollectionBuilder<EE>> supplier () { return VoidResultCollectionBuilder::new; }
		@Override public BiConsumer<VoidResultCollectionBuilder<EE>, VoidResult<EE>> accumulator ()
		{ return VoidResultCollectionBuilder::add; }
		@Override public BinaryOperator<VoidResultCollectionBuilder<EE>> combiner () { return VoidResultCollectionBuilder::addAll; }
		@Override public Function<VoidResultCollectionBuilder<EE>, VoidResultCollection<EE>> finisher()
		{ return VoidResultCollectionBuilder::collect; }
		@Override public Set<Characteristics> characteristics () { return Collections.emptySet(); }
	}

	private static final class VoidResultCollectionBuilder<E> {
		private final Stream.Builder<E> errs = Stream.builder();
		private int numOks = 0;

		/** Add a single result to this collector */
		public void add(final VoidResult<E> result) {
			if (result.isErr()) {
				errs.add(result.asErr().get());
			} else {
				numOks += 1;
			}
		}

		/** Add all elements from partial to this */
		public VoidResultCollectionBuilder<E> addAll(final VoidResultCollectionBuilder<E> partial) {
			partial.errs.build().forEach(errs::add);
			numOks += partial.numOks;
			return this;
		}

		public VoidResultCollection<E> collect() {
			return new VoidResultCollection<>(errs.build().collect(Collectors.toList()), numOks);
		}
	}
}
