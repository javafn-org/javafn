package org.javafn.result;

import java.util.ArrayList;
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
import java.util.stream.Stream;

/**
 * An immutable aggregation over a stream of Result objects, generated using the Result.collector() method.
 * All Err Result values will be collected into the errs list and all Ok Result values will be collected into the oks list.
 * Note that is not a sum type like Result; it can contain both types.
 * <pre>{@code
 * final Results<X, Y> rc = Arrays.stream(...)
 *     .map(Result.TryMap(...))
 *     .collect(Results.collect());
 * logger.info("There were {} successes and {} errors.", rc.oks().size(), rc.errs().size());
 * final Z z = rc.reduce(x_vals -> zFrom(x_vals), y_vals -> zFrom(y_vals));
 * }</pre>
 * This class also contains convince methods for merging with existing collections, however since this class
 * is immutable, these functions return new instances of Results.
 * This class also contains several constructor methods for creating Results where no stream
 * of Results is available.
 *
 * @see ResultCollection#addAll(List, List)
 * @see ResultCollection#addErrs(List)
 * @see ResultCollection#addOks(List)
 * @see ResultCollection#addAll(ResultCollection)
 * @see ResultCollection#fold()
 * @see ResultCollection#fold(Function, Function)
 * @see ResultCollection#reduce(Function, Function)
 * @see ResultCollection#reduce(Function, Function, BiFunction)
 */
public final class ResultCollection<E, O> {

	/**
	 * Construct an empty Results, which would be used in no-op functions.
	 * <pre>{@code
	 * public ResultCollection<Foo, Bar> either() {
	 *  // no-op implementation of some class
	 *  return ResultCollection.empty();
	 * }
	 * }</pre>
	 *
	 * @return an empty ResultCollection
	 */
	public static <EE, OO> ResultCollection<EE, OO> empty() {
		return new ResultCollection<>(Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Construct a ResultCollection for a single Result object, useful for APIs that operate on a collection,
	 * but your operation only produces a single result.
	 * <pre>{@code
	 * public ResultCollection<Foo, Bar> doThing() {
	 *  final Result<Foo, Bar> result = Result.ok(new Bar());
	 *  return ResultCollection.singleton(result);
	 * }
	 * }</pre>
	 *
	 * @param result the single result to put into this collection
	 * @return a ResultCollection with one element taken from the supplied parameter
	 */
	public static <EE, OO> ResultCollection<EE, OO> singleton(final Result<EE, OO> result) {
		return result.reduce(
				err -> new ResultCollection<>(Collections.singletonList(err), Collections.emptyList()),
				ok -> new ResultCollection<>(Collections.emptyList(), Collections.singletonList(ok)));
	}

	/**
	 * Create a ResultCollection from the supplied error and ok lists.  Null is a valid value, for example,
	 * when creating a ResultCollection where errors are not possible.
	 * <pre>{@code
	 * public ResultCollection<Foo, Bar> either() {
	 *  final List<Bar> bars = constructSomeBars();
	 *  return ResultCollection.from(null, bars);
	 * }
	 * }</pre>
	 *
	 * @param errs the list of error values or null if there are no errors
	 * @param oks  the list of ok values or null if there are no oks
	 * @return a ResultCollection with the supplied errors and oks
	 */
	public static <EE, OO> ResultCollection<EE, OO> from(final List<EE> errs, final List<OO> oks) {
		return new ResultCollection<>(
				errs == null ? null : new ArrayList<>(errs),
				oks == null ? null : new ArrayList<>(oks));
	}

	/**
	 * Create a ResultCollection from the supplied Optional error and ok lists.
	 * <pre>{@code
	 * public ResultCollection<Foo, Bar> either() {
	 *  final Optional<List<Bar>> bars = maybeGetBars();
	 *  return ResultCollection.from(Optional.empty(), bars);
	 * }
	 * }</pre>
	 *
	 * @param errs the Optional list of error values
	 * @param oks  the Optional list of ok values
	 * @return a ResultCollection with the supplied errors and oks
	 */
	public static <EE, OO> ResultCollection<EE, OO> from(
			final Optional<List<EE>> errs,
			final Optional<List<OO>> oks) {
		return new ResultCollection<>(
				errs == null ? null : new ArrayList<>(errs.orElse(Collections.emptyList())),
				oks == null ? null : new ArrayList<>(oks.orElse(Collections.emptyList())));
	}

	/**
	 * Create a ResultCollection from the supplied error and ok lists along with a list of additional results.
	 * Null is a valid value, for example, when creating a ResultCollection where errors are not possible.
	 * This method should be used as a final step, for example, when testing some parameters, then
	 * performing some (also failable) action on the results.
	 * <pre>{@code
	 * public ResultCollection<Error, Success> either(List<Foo> foos) {
	 *  List<Error> errs = ...
	 *  List<Foo> cleaned = ...
	 *  for (final Foo foo : foos) {
	 *      if (...) {
	 *          errs.add(...);
	 *      } else {
	 *          cleaned.add(foo);
	 *      }
	 *  }
	 *  return ResultCollection.from(errs, null, processCleaned(foo));
	 * }
	 * }</pre>
	 *
	 * @param errs        the list of error values or null if there are no errors
	 * @param oks         the list of ok values or null if there are no oks
	 * @param moreResults a list of unsorted results
	 * @return a ResultCollection with the supplied values
	 */
	public static <EE, OO> ResultCollection<EE, OO> from(
			final List<EE> errs, final List<OO> oks, List<Result<EE, OO>> moreResults) {
		return new ResultCollection<>(
				errs == null ? null : new ArrayList<>(errs),
				oks == null ? null : new ArrayList<>(oks))
				.addAll(moreResults.stream().collect(Result.collector()));
	}

	private final List<E> errs;
	private final List<O> oks;

	ResultCollection(final List<E> _errs, final List<O> _oks) {
		errs = _errs == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_errs);
		oks = _oks == null
				? Collections.emptyList()
				: Collections.unmodifiableList(_oks);
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
		return !oks.isEmpty();
	}

	/**
	 * return whether this collection contains both err and ok types, i.e., {@code hasErrs() && hasOks()}
	 */
	public boolean hasBoth() {
		return !errs.isEmpty() && !oks.isEmpty();
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
	public List<O> getOks() {
		return oks;
	}

	/**
	 * Construct a new ResultCollection from this collection by mapping the
	 * list of errors to a new list.  The new list does not need to have the same
	 * number of elements.
	 *
	 * @param fn a function that maps a list of type E to a list of type NEWE
	 * @return a new ResultCollection with the same ok values and the err list updated from the supplied function
	 */
	public <NEWE> ResultCollection<NEWE, O> mapErrs(final Function<List<E>, List<NEWE>> fn) {
		return new ResultCollection<>(new ArrayList<>(fn.apply(errs)), oks);
	}

	/**
	 * Construct a new ResultCollection from this collection by mapping the
	 * list of oks to a new list.  The new list does not need to have the same
	 * number of elements.
	 *
	 * @param fn a function that maps a list of type O to a list of type NEWO
	 * @return a new ResultCollection with the same err values and the ok list updated from the supplied function
	 */
	public <NEWO> ResultCollection<E, NEWO> mapOks(final Function<List<O>, List<NEWO>> fn) {
		return new ResultCollection<>(errs, new ArrayList<>(fn.apply(oks)));
	}

	/**
	 * Construct a new ResultCollection by adding the supplied list of errors to this list of errors and including
	 * this list of oks unmodified.
	 * Order of the lists are maintained, with this collection's errors coming first.
	 *
	 * @param errs the list of errors that should be added to this collection's errs to produce a new collection
	 * @return a new ResultCollection whose error list is a combination of the existing errors
	 * and the supplied errors and whose ok list is the existing oks
	 */
	public ResultCollection<E, O> addErrs(final List<E> errs) {
		return new ResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				this.oks);
	}

	/**
	 * Construct a new ResultCollection by adding the supplied list of oks to this list of oks and including
	 * this list of errors unmodified.
	 * Order of the lists are maintained, with this collection's oks coming first.
	 *
	 * @param oks the list of oks that should be added to this collection's oks to produce a new collection
	 * @return a new ResultCollection whose ok list is a combination of the existing oks and the supplied oks
	 * and whose err list is the existing errs
	 */
	public ResultCollection<E, O> addOks(final List<O> oks) {
		return new ResultCollection<>(
				this.errs,
				Stream.concat(this.oks.stream(), oks == null
						? Stream.empty()
						: oks.stream()).collect(Collectors.toList()));
	}

	/**
	 * Construct a new ResultCollection by adding the supplied list of errors to this list of errors and adding
	 * the supplied list of oks to this list of oks.
	 * Order of the lists are maintained, with this collection's errors and oks coming first.
	 *
	 * @param errs the list of errors that should be added to this collection's errs
	 * @param oks  the list of oks that should be added to this collection's oks
	 * @return a new ResultCollection whose error list is a combination of the existing errors
	 * and the supplied errors and whose ok list is a combination of the existing oks and the supplied oks
	 */
	public ResultCollection<E, O> addAll(final List<E> errs, final List<O> oks) {
		return new ResultCollection<>(
				Stream.concat(this.errs.stream(), errs == null
						? Stream.empty()
						: errs.stream()).collect(Collectors.toList()),
				Stream.concat(this.oks.stream(), oks == null
						? Stream.empty()
						: oks.stream()).collect(Collectors.toList()));
	}

	/**
	 * Construct a new ResultCollection by adding the other collection's list of errors to this list of errors
	 * and adding the other collection's list of oks to this list of oks.
	 * Order of the lists are maintained, with this collection's errors and oks coming first.
	 *
	 * @param other the ResultCollection whose errors and oks should be appended to this collection's errors
	 *              and oks respectively, to produce a new ResultCollection
	 * @return a new ResultCollection whose error list is a combination of the existing errors
	 * and the other collection's errors * and whose ok list is a combination of the existing oks
	 * and the other collection's oks
	 */
	public ResultCollection<E, O> addAll(final ResultCollection<E, O> other) {
		return new ResultCollection<>(
				Stream.concat(this.errs.stream(), other.errs.stream()).collect(Collectors.toList()),
				Stream.concat(this.oks.stream(), other.oks.stream()).collect(Collectors.toList()));
	}

	/**
	 * Construct a new ResultCollection by adding the list of results to this collection's
	 * errors and oks as appropriate
	 * Order of the lists are maintained, with this collection's errors and oks coming first.
	 *
	 * @param moreResults a list of results whose errors and oks should be appended to this collection's errors
	 *                    and oks respectively, to produce a new ResultCollection
	 * @return a new ResultCollection whose error list is a combination of the existing errors
	 * and the errors in the moreResults list and whose ok list is a combination of the existing oks
	 * and the oks in the moreResults list
	 */
	public ResultCollection<E, O> addAll(final List<Result<E, O>> moreResults) {
		return addAll(moreResults.stream().collect(Result.collector()));
	}

	/**
	 * Collapse this collection into a Result.  If any err types exist, this function returns a Result.err
	 * wrapping the list of errs.
	 * Otherwise, this function returns a Result.ok wrapping the ok values.
	 * Note that because Result is a sum type, the result of this operation will either be an Err or an Ok
	 * but not both, i.e., ok values are lost if any errs are present.
	 * <pre>{@code
	 * final Result<List<String>, List<Integer>> res = resultCollection.fold();
	 * final String toPrint = res.reduce(
	 *     errs -> "There were " + errs.size() + " errors",
	 *     oks -> "There were " + oks.size() + " successes");
	 * );
	 * }</pre>
	 *
	 * @return an err result wrapping a list of err values, or if no errs were present,
	 * an ok result wrapping a list of ok values
	 */
	public Result<List<E>, List<O>> fold() {
		if (hasErrs()) return Result.err(errs);
		return Result.ok(oks);
	}

	/**
	 * Collapse this collection into a Result.  If any err types exist, this function calls the supplied
	 * fnErr and returns a Result.err wrapping fnErr's result.
	 * Otherwise, this function calls fnOk and returns a Result.ok wrapping fnOk's result.
	 * Note that the ok values are lost if there are any err values.
	 * <pre>{@code
	 * final Result<String, Integer> res = resultCollection.fold(
	 *     errs -> "There were " + errs.size() + " errors",
	 *     oks -> oks.size()
	 * );
	 * }</pre>
	 *
	 * @param fnErr the function to apply to the errs, if there are any
	 * @param fnOk  the function to apply to the oks, if there are no errs
	 * @param <U>   the type that errs will be mapped to
	 * @param <V>   the type that oks will be mapped to
	 * @return the errs reduced into a Result.err using the supplied fnErr, or if there are no errors,
	 * the oks reduced into a Result.ok using the supplied fnOk
	 */
	public <U, V> Result<U, V> fold(final Function<List<E>, U> fnErr, final Function<List<O>, V> fnOk) {
		if (hasErrs()) return Result.err(fnErr.apply(errs));
		return Result.ok(fnOk.apply(oks));
	}

	/**
	 * Collapse this collection into a single value.
	 * If any err types exist, this function calls the supplied fnErr and returns that.
	 * Otherwise, this function calls fnOk and returns that instead.
	 * Note that the ok values are lost if there are any err values.
	 * <pre>{@code
	 * final String toPrint = resultCollection.reduce(
	 *     errs -> "There were " + errs.size() + " errors",
	 *     oks -> "There were no errors and " + oks.size() + " successes"
	 * );
	 * }</pre>
	 *
	 * @param fnErr the function to apply to the errs, if there are any
	 * @param fnOk  the function to apply to the oks, if there are no errs
	 * @param <T>   the type that both errs and oks can be mapped to
	 * @return the errs mapped using the supplied fnErr, or if there are no errors,
	 * the oks mapped using the supplied fnOk
	 */
	public <T> T reduce(final Function<List<E>, T> fnErr, final Function<List<O>, T> fnOk) {
		if (hasErrs()) return fnErr.apply(errs);
		return fnOk.apply(oks);
	}

	/**
	 * <p>
	 * Collapse this collection into a single type.  If any errors exist, map them to the type U.
	 * If any oks exist, map them to the type V.
	 * Then call the combiner with an Optional U and Optional V, which returns the reduced type.
	 * </p>
	 * <p>
	 * Compare this with the {@link ResultCollection#fold(Function, Function)} which
	 * returns a Result.err with all of the err values mapped to one type, or if there are no errors,
	 * returns a Result.ok with all of the ok values mapped to a different type.
	 * Similarly compare this with the {@link ResultCollection#reduce(Function, Function)} method which maps the
	 * errs if they exist and drops the oks, or maps the oks otherwise.
	 * The notable difference with this function is that the err and ok types can be combined.
	 * Finally, compare this with the {@link ResultCollection#reduce(BiFunction)} method which does not support
	 * an intermediate mapping.
	 * </p>
	 * <pre>{@code
	 * final String toPrint = resultCollection.reduce(
	 *     List::size,
	 *     List::size,
	 *     (errCount, okCount) ->
	 *         "There were " + errCount.map(Integer::toString).orElse("no")
	 *         + " errors and " + okCount.map(Integer::toString).orElse("no")
	 *         + " successes."
	 * );
	 * }</pre>
	 *
	 * @param fnErr    the function that maps the errors to some type U
	 * @param fnOk     the function that maps the oks to some type V
	 * @param combiner the function that takes the mapped errors and mapped oks and returns some value
	 * @param <T>      the desired return type
	 * @param <U>      an intermediate type that the errors get mapped to
	 * @param <V>      an intermediate type that the oks get mapped to
	 * @return the result of the combiner function after mapping the errs and oks
	 */
	public <T, U, V> T reduce(
			final Function<List<E>, U> fnErr,
			final Function<List<O>, V> fnOk,
			final BiFunction<Optional<U>, Optional<V>, T> combiner) {
		return combiner.apply(
				errs.isEmpty() ? Optional.empty() : Optional.of(fnErr.apply(errs)),
				oks.isEmpty() ? Optional.empty() : Optional.of(fnOk.apply(oks)));
	}

	/**
	 * <p>
	 * Collapse this collection into a single type.
	 * </p>
	 * <p>
	 * Compare this with the {@link ResultCollection#fold(Function, Function)} which
	 * returns a Result.err with all of the err values mapped to one type, or if there are no errors,
	 * returns a Result.ok with all of the ok values mapped to a different type.
	 * Similarly compare this with the {@link ResultCollection#reduce(Function, Function)} method which maps the
	 * errs if they exist and drops the oks, or maps the oks otherwise.
	 * The notable difference with this function is that the err and ok types can be combined.
	 * Finally, compare this with the {@link ResultCollection#reduce(Function, Function, BiFunction)} which
	 * applies an intermediate mapping to the elements before combining them.
	 * </p>
	 * <pre>{@code
	 * final String toPrint = resultCollection.reduce(
	 *     (errList, okList) ->
	 *         "The errors were [" + String.join(",", errs) + "] and the oks were ["
	 *         + String.join(",", oks) + "]")
	 * );
	 * }</pre>
	 *
	 * @param fn the function that takes the errors and mapped oks and returns some value
	 * @param <R>      the desired return type
	 * @return the result of the combiner function
	 */
	public <R> R reduce(final BiFunction<List<E>, List<O>, R> fn) {
		return fn.apply(errs, oks);
	}

	public static final class ResultCollector<EE, OO>
			implements Collector<Result<EE, OO>, ResultCollectionBuilder<EE, OO>, ResultCollection<EE, OO>> {
		@Override public Supplier<ResultCollectionBuilder<EE, OO>> supplier () { return ResultCollectionBuilder::new; }
		@Override public BiConsumer<ResultCollectionBuilder<EE, OO>, Result<EE, OO>> accumulator ()
		{ return ResultCollectionBuilder::add; }
		@Override public BinaryOperator<ResultCollectionBuilder<EE, OO>> combiner () { return ResultCollectionBuilder::addAll; }
		@Override public Function<ResultCollectionBuilder<EE, OO>, ResultCollection<EE, OO>> finisher()
		{ return ResultCollectionBuilder::collect; }
		@Override public Set<Characteristics> characteristics () { return Collections.emptySet(); }
	}

	private static final class ResultCollectionBuilder<E, O> {
	    private final Stream.Builder<E> errs = Stream.builder();
	    private final Stream.Builder<O> oks = Stream.builder();

	    /** Add a single result to this collector */
	    public void add(final Result<E, O> result) {
	        if (result.isErr()) {
	            errs.add(result.asErr().get());
	        } else {
	            oks.add(result.asOk().get());
	        }
	    }

	    /** Add all elements from partial to this */
	    public ResultCollectionBuilder<E, O> addAll(final ResultCollectionBuilder<E, O> partial) {
	        partial.errs.build().forEach(errs::add);
	        partial.oks.build().forEach(oks::add);
	        return this;
	    }

	    /** Produce a Results from this intermediate data structure */
	    public ResultCollection<E, O> collect() {
	        return new ResultCollection<>(
	                errs.build().collect(Collectors.toList()),
	                oks.build().collect(Collectors.toList()));
	    }
	}
}
