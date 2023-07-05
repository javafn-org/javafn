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
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class LongResultCollection<E> {



	public static <EE> LongResultCollection<EE> empty() {
		return new LongResultCollection<>(Collections.emptyList(), new long[0]);
	}

	public static <EE> LongResultCollection<EE> singleton(final LongResult<EE> result) {
		return result.reduce(
				err -> new LongResultCollection<>(Collections.singletonList(err), new long[0]),
				ok -> new LongResultCollection<>(Collections.emptyList(), new long[]{ok} ));
	}

	public static <EE> LongResultCollection<EE> from(final List<EE> errs, final long[] oks) {
		return new LongResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks);
	}

	public static <EE> LongResultCollection<EE> from(final Optional<List<EE>> errs, final Optional<long[]> oks) {
		return new LongResultCollection<>(
				errs == null ? null : new ArrayList<>(errs.orElse(Collections.emptyList())),
				oks == null ? null : oks.orElse(new long[0]));
	}

	public static <EE> LongResultCollection<EE> from(final List<EE> errs, final long[] oks, List<LongResult<EE>> moreResults) {
		return new LongResultCollection<>(errs == null ? null : new ArrayList<>(errs), oks)
				.addAll(moreResults.stream().collect(LongResult.collector()));
	}

	private final List<E> errs;
	private final long[] oks;

	LongResultCollection(final List<E> _errs, final long[] _oks) {
		errs = _errs == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_errs);
		oks = _oks == null
				? new long[0]
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
	public long[] getOks() {
		return oks;
	}

	public <NEWE> LongResultCollection<NEWE> mapErrs(final Function<List<E>, List<NEWE>> fn) {
		return new LongResultCollection<>(new ArrayList<>(fn.apply(errs)), oks);
	}

	public LongResultCollection<E> mapOks(final Function<long[], long[]> fn) {
		return new LongResultCollection<>(errs, fn.apply(oks));
	}

	public <NEWO> ResultCollection<E, NEWO> mapOksToObj(final Function<long[], List<NEWO>> fn) {
		return new ResultCollection<>(errs, new ArrayList<>(fn.apply(oks)));
	}

	public LongResultCollection<E> addErrs(final List<E> errs) {
		return new LongResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.oks);
	}

	public LongResultCollection<E> addOks(final long[] oks) {
		return new LongResultCollection<>(
				this.errs,
				LongStream.concat(
								Arrays.stream(this.oks),
								oks == null ? LongStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public LongResultCollection<E> addAll(final List<E> errs, final long[] oks) {
		return new LongResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				LongStream.concat(
								Arrays.stream(this.oks),
								oks == null ? LongStream.empty() : Arrays.stream(oks))
						.toArray());
	}

	public LongResultCollection<E> addAll(final LongResultCollection<E> other) {
		return new LongResultCollection<>(
				Stream.concat(this.errs.stream(), other.errs.stream()).collect(Collectors.toList()),
				LongStream.concat(Arrays.stream(this.oks), Arrays.stream(other.oks)).toArray());
	}

	public LongResultCollection<E> addAll(final List<LongResult<E>> moreResults) {
		return addAll(moreResults.stream().collect(LongResult.collector()));
	}

	public Result<List<E>, long[]> fold() {
		if (hasErrs()) return Result.err(errs);
		return Result.ok(oks);
	}

	public <U> Result<U, long[]> fold(final Function<List<E>, U> fnErr, final Function<long[], long[]> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <U, V> Result<U, V> foldToObj(final Function<List<E>, U> fnErr, final Function<long[], V> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	public <T> T reduce(final Function<List<E>, T> fnErr, final Function<long[], T> fnOk) {
		if (hasErrs()) return fnErr.apply(errs);
		return fnOk.apply(oks);
	}

	public <T, U, V> T reduce(
			final Function<List<E>, U> fnErr,
			final Function<long[], V> fnOk,
			final BiFunction<Optional<U>, Optional<V>, T> combiner) {
		return combiner.apply(
				errs.isEmpty() ? Optional.empty() : Optional.of(fnErr.apply(errs)),
				oks.length == 0 ? Optional.empty() : Optional.of(fnOk.apply(oks)));
	}

	public <R> R reduce(final BiFunction<List<E>, long[], R> fn)
	{ return fn.apply(errs, oks); }

	public static final class LongResultCollector<EE>
			implements Collector<LongResult<EE>, LongResultCollectorBuilder<EE>, LongResultCollection<EE>> {
		@Override public Supplier<LongResultCollectorBuilder<EE>> supplier () { return LongResultCollectorBuilder::new; }
		@Override public BiConsumer<LongResultCollectorBuilder<EE>, LongResult<EE>> accumulator ()
		{ return LongResultCollectorBuilder::add; }
		@Override public BinaryOperator<LongResultCollectorBuilder<EE>> combiner () { return LongResultCollectorBuilder::addAll; }
		@Override public Function<LongResultCollectorBuilder<EE>, LongResultCollection<EE>> finisher()
		{ return LongResultCollectorBuilder::collect; }
		@Override public Set<Characteristics> characteristics () { return Collections.emptySet(); }
	}

	private static final class LongResultCollectorBuilder<E> {
		private final Stream.Builder<E> errs = Stream.builder();
		private final LongStream.Builder oks = LongStream.builder();

		/** Add a single result to this collector */
		public void add(final LongResult<E> result) {
			if (result.isErr()) {
				errs.add(result.asErr().get());
			} else {
				oks.add(result.asOk().get());
			}
		}

		/** Add all elements from partial to this */
		public LongResultCollectorBuilder<E> addAll(final LongResultCollectorBuilder<E> partial) {
			partial.errs.build().forEach(errs::add);
			partial.oks.build().forEach(oks::add);
			return this;
		}

		public LongResultCollection<E> collect() {
			return new LongResultCollection<>(
					errs.build().collect(Collectors.toList()),
					oks.build().toArray());
		}
	}
}
