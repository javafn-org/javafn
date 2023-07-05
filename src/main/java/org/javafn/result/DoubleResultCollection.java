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
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class DoubleResultCollection<E> {


	public static <EE> DoubleResultCollection<EE> empty() {
		return new DoubleResultCollection<>(Collections.emptyList(), new double[0]);
	}

	public static <EE> DoubleResultCollection<EE> singleton(final DoubleResult<EE> result) {
		return result.reduce(
				err -> new DoubleResultCollection<>(Collections.singletonList(err), new double[0]),
				ok -> new DoubleResultCollection<>(Collections.emptyList(), new double[]{ok} ));
	}

	public static <EE> DoubleResultCollection<EE> from(final List<EE> errs, final double[] oks) {
		return new DoubleResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks);
	}

	public static <EE> DoubleResultCollection<EE> from(final Optional<List<EE>> errs, final Optional<double[]> oks) {
		return new DoubleResultCollection<>(
				errs == null ? null : new ArrayList<>(errs.orElse(Collections.emptyList())),
				oks == null ? null : oks.orElse(new double[0]));
	}

	public static <EE> DoubleResultCollection<EE> from(final List<EE> errs, final double[] oks, List<DoubleResult<EE>> moreResults) {
		return new DoubleResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks)
				.addAll(moreResults.stream().collect(DoubleResult.collector()));
	}

	private final List<E> errs;
	private final double[] oks;

	DoubleResultCollection(final List<E> _errs, final double[] _oks) {
		errs = _errs == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_errs);
		oks = _oks == null
				? new double[0]
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
	public double[] getOks() {
		return oks;
	}

	public <NEWE> DoubleResultCollection<NEWE> mapErrs(final Function<List<E>, List<NEWE>> fn) {
		return new DoubleResultCollection<>(new ArrayList<>(fn.apply(errs)), oks);
	}

	public DoubleResultCollection<E> mapOks(final Function<double[], double[]> fn) {
		return new DoubleResultCollection<>(errs, fn.apply(oks));
	}

	public <NEWO> ResultCollection<E, NEWO> mapOksToObj(final Function<double[], List<NEWO>> fn) {
		return new ResultCollection<>(errs, new ArrayList<>(fn.apply(oks)));
	}

	public DoubleResultCollection<E> addErrs(final List<E> errs) {
		return new DoubleResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.oks);
	}

	public DoubleResultCollection<E> addOks(final double[] oks) {
		return new DoubleResultCollection<>(
				this.errs,
				DoubleStream.concat(
								Arrays.stream(this.oks),
								oks == null ? DoubleStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public DoubleResultCollection<E> addAll(final List<E> errs, final double[] oks) {
		return new DoubleResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				DoubleStream.concat(
								Arrays.stream(this.oks),
								oks == null ? DoubleStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public DoubleResultCollection<E> addAll(final DoubleResultCollection<E> other) {
		return new DoubleResultCollection<>(
				Stream.concat(this.errs.stream(), other.errs.stream()).collect(Collectors.toList()),
				DoubleStream.concat(Arrays.stream(this.oks), Arrays.stream(other.oks)).toArray());
	}

	public DoubleResultCollection<E> addAll(final List<DoubleResult<E>> moreResults) {
		return addAll(moreResults.stream().collect(DoubleResult.collector()));
	}

	public Result<List<E>, double[]> fold() {
		if (hasErrs()) return Result.err(errs);
		return Result.ok(oks);
	}

	public <U> Result<U, double[]> fold(final Function<List<E>, U> fnErr, final Function<double[], double[]> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <U, V> Result<U, V> foldToObj(final Function<List<E>, U> fnErr, final Function<double[], V> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <T> T reduce(final Function<List<E>, T> fnErr, final Function<double[], T> fnOk) {
		if (hasErrs()) return fnErr.apply(errs);
		return fnOk.apply(oks);
	}

	public <T, U, V> T reduce(
			final Function<List<E>, U> fnErr,
			final Function<double[], V> fnOk,
			final BiFunction<Optional<U>, Optional<V>, T> combiner) {
		return combiner.apply(
				errs.isEmpty() ? Optional.empty() : Optional.of(fnErr.apply(errs)),
				oks.length == 0 ? Optional.empty() : Optional.of(fnOk.apply(oks)));
	}

	public <R> R reduce(final BiFunction<List<E>, double[], R> fn)
	{ return fn.apply(errs, oks); }

	public static final class DoubleResultCollector<EE>
			implements Collector<DoubleResult<EE>, DoubleResultCollectionBuilder<EE>, DoubleResultCollection<EE>> {
		@Override public Supplier<DoubleResultCollectionBuilder<EE>> supplier () { return DoubleResultCollectionBuilder::new; }
		@Override public BiConsumer<DoubleResultCollectionBuilder<EE>, DoubleResult<EE>> accumulator ()
		{ return DoubleResultCollectionBuilder::add; }
		@Override public BinaryOperator<DoubleResultCollectionBuilder<EE>> combiner () { return DoubleResultCollectionBuilder::addAll; }
		@Override public Function<DoubleResultCollectionBuilder<EE>, DoubleResultCollection<EE>> finisher()
		{ return DoubleResultCollectionBuilder::collect; }
		@Override public Set<Characteristics> characteristics () { return Collections.emptySet(); }
	}

	private static final class DoubleResultCollectionBuilder<E> {
		private final Stream.Builder<E> errs = Stream.builder();
		private final DoubleStream.Builder oks = DoubleStream.builder();

		/** Add a single result to this collector */
		public void add(final DoubleResult<E> result) {
			if (result.isErr()) {
				errs.add(result.asErr().get());
			} else {
				oks.add(result.asOk().get());
			}
		}

		/** Add all elements from partial to this */
		public DoubleResultCollectionBuilder<E> addAll(final DoubleResultCollectionBuilder<E> partial) {
			partial.errs.build().forEach(errs::add);
			partial.oks.build().forEach(oks::add);
			return this;
		}

		public DoubleResultCollection<E> collect() {
			return new DoubleResultCollection<>(
					errs.build().collect(Collectors.toList()),
					oks.build().toArray());
		}
	}
}
