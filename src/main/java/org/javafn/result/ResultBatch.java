package org.javafn.result;

import org.javafn.utils.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * A product type collection of multiple results, capturing both successful and failures, sorted.
 * This type can be produced from a {@link java.util.stream.Stream} of {@link Result}s.
 * There are two primary use cases.  First, when you want to attempt multiple operations
 * and succeed if all succeed or fail reporting all the failures (rather than reporting
 * the first failure, having client code fix that error, then resubmit only to find the next error).
 * This use case can be thought of as a transaction.  If {@link #errors} is empty, commit the operations,
 * otherwise, rollback the successes and report the errors.  Second, you want to support partial
 * operations.  You want to attempt multiple operations which may succeed or fail, but these don't
 * affect the other operations.  You can aggregate the operations that fail and still report
 * on the successful ones.
 * <p>
 * Contrasting with the Result type, this is a product type, not a sum type.  Both values are present,
 * although they may be empty.
 * @param errors the {@link Result} error values
 * @param oks the {@link Result} ok values
 */
public record ResultBatch<ERR, OK>(List<ERR> errors, List<OK> oks) {

    public ResultBatch {
        errors = errors == null ? List.of() : List.copyOf(errors);
        oks = oks == null ? List.of() : List.copyOf(oks);
    }

    /**
     * Construct an empty ResultBatch, useful for APIs that require a ResultBatch,
     * but calling/returning code does not have anything to report
     */
    public static <E, O> ResultBatch<E, O> empty() { return new ResultBatch<>(List.of(), List.of()); }

    /**
     * Construct a ResultBatch from the supplied results.  This is useful for APIs that require
     * a ResultBatch, e.g. as a return type, but rather than operating on a stream, you have one
     * item to report.
     */
    @SafeVarargs
    public static <E, O> ResultBatch<E, O> of(final Result<E, O>... results) {
        return Arrays.stream(results).collect(collector());
    }

    /**
     * Return a {@link Collector} over a stream of {@link Result} types producing a ResultBatch.
     * @see Result#batch()
     */
    public static <E, O> Collector<Result<E, O>, ?, ResultBatch<E, O>> collector() {
        return Collector.<Result<E, O>, Acc<E, O>, ResultBatch<E, O>>of(
                Acc::new, Acc::add, Acc::merge, Acc::finish);
    }

    /** true if {@link #errors} has one or more elements */
    public boolean hasErrors() { return !errors.isEmpty(); }
    /** true if {@link #oks} has one or more elements */
    public boolean hasOks() { return !oks.isEmpty(); }
    /** true if {@link #errors} has at least one element and {@link #oks} has no elements */
    public boolean onlyErrors() { return !errors.isEmpty() && oks.isEmpty(); }
    /** true if {@link #errors} has no elements and {@link #oks} has at least one element */
    public boolean onlyOks() { return errors.isEmpty() && !oks.isEmpty(); }
    /** true if both {@link #errors} and {@link #oks} has at least one element each */
    public boolean hasBoth() { return !errors.isEmpty() && ! oks.isEmpty(); }

    /** The number of errors and oks */
    public int size() { return errors.size() + oks.size(); }

    /**
     * Apply the supplied mapping functions to their respective list elements and produce a new
     * ResultBatch containing the result of the mapping.
     */
    public <E, O> ResultBatch<E, O> map(final Function<ERR, E> errFn, final Function<OK, O> okFn) {
        return new ResultBatch<>(
                errors.stream().map(errFn).toList(),
                oks.stream().map(okFn).toList());
    }

    /**
     * Apply the supplied mapping function to the error elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public <Z> ResultBatch<Z, OK> mapErrs(final Function<ERR, Z> fn) {
        return new ResultBatch<>(errors.stream().map(fn).toList(), oks);
    }

    /**
     * Apply the supplied mapping function to the ok elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public <Z> ResultBatch<ERR, Z> mapOks(final Function<OK, Z> fn) {
        return new ResultBatch<>(errors, oks.stream().map(fn).toList());
    }

    /**
     * Turn this ResultBatch into a Result.  If errors is not empty, return a
     * {@link org.javafn.result.Result.Err} containing the list of errors, dropping the oks.
     * Otherwise, return a {@link org.javafn.result.Result.Ok} containing a list of oks.
     */
    public Result<List<ERR>, List<OK>> fold() {
        return errors.isEmpty()
                ? Result.ok(oks)
                : Result.err(errors);
    }

    /**
     * Combine the errors and oks into a single type, for example, a JSON object that will
     * be returned to client code.
     */
    public <Z> Z reduce(final BiFunction<List<ERR>, List<OK>, Z> fn) {
        return fn.apply(errors, oks);
    }

    /**
     * Operate on the two sets of values without producing a new value.
     */
    public void use(final BiConsumer<List<ERR>, List<OK>> fn) {
        fn.accept(errors, oks);
    }

    /**
     * Return a new ResultBatch with all elements of this and all the elements of that appended.
     */
    public ResultBatch<ERR, OK> append(final ResultBatch<ERR, OK> that) {
        return new ResultBatch<>(
                Data.append(errors, that.errors),
                Data.append(oks, that.oks));
    }

    private record Acc<E, O>(ArrayList<E> errors, ArrayList<O> oks) {
        Acc() { this(new ArrayList<>(), new ArrayList<>()); }
        void add(final Result<E, O> result) { result.use(errors::add, oks::add); }
        Acc<E, O> merge(final Acc<E, O> other) {
            errors.addAll(other.errors);
            oks.addAll(other.oks);
            return this;
        }
        ResultBatch<E, O> finish() { return new ResultBatch<>(errors, oks); }
    }
}
