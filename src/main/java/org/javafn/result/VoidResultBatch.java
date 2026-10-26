package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.javafn.result.AnyError.joined;

/**
 * A product type collection of multiple results, capturing both successful and failures, sorted.
 * This type can be produced from a {@link java.util.stream.Stream} of {@link VoidResult}s.
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
 * @param errors the error values
 * @param okCount the number of {@link VoidResult} ok values
 */
public record VoidResultBatch(@NonNull List<AnyError> errors, int okCount) {

    @FunctionalInterface
    public interface ObjIntFunction<A, R> {
        R apply(A a, int b);
    }
    public VoidResultBatch(@Nullable List<AnyError> errors, int okCount) {
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.okCount = okCount;
    }

    /**
     * Construct an empty ResultBatch, useful for APIs that require a ResultBatch,
     * but calling/returning code does not have anything to report
     */
    public static @NonNull VoidResultBatch empty() { return new VoidResultBatch(List.of(), 0); }

    /**
     * Construct a ResultBatch from the supplied results.  This is useful for APIs that require
     * a ResultBatch, e.g. as a return type, but rather than operating on a stream, you have one
     * item to report.
     */
    public static @NonNull VoidResultBatch of(@NonNull final VoidResult... results) {
        return Arrays.stream(results).collect(collector());
    }

    /**
     * Return a {@link Collector} over a stream of {@link Result} types producing a ResultBatch.
     * @see Result#batch()
     */
    public static @NonNull Collector<VoidResult, ?, VoidResultBatch> collector() {
        return Collector.of(Acc::new, Acc::add, Acc::merge, Acc::finish);
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasOks() { return okCount > 0; }
    public boolean isOnlyErrors() { return !errors.isEmpty() && okCount == 0; }
    public boolean isOnlyOks() { return errors.isEmpty() && okCount > 0; }
    public boolean hasBoth() { return !errors.isEmpty() && okCount > 0; }

    /** The number of errors and oks */
    public int size() { return errors.size() + okCount; }

    /**
     * Apply the supplied mapping functions to their respective list elements and produce a new
     * ResultBatch containing the result of the mapping.
     */
    public <O> @NonNull ResultBatch<O> map(@NonNull final Function<AnyError, AnyError> errFn, @NonNull final IntFunction<List<O>> okFn) {
        return new ResultBatch<>(
                errors.stream().map(errFn).toList(),
                okFn.apply(okCount));
    }

    /**
     * Apply the supplied mapping function to the error elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull VoidResultBatch mapErrs(@NonNull final Function<AnyError, AnyError> fn) {
        return new VoidResultBatch(errors.stream().map(fn).toList(), okCount);
    }

    /**
     * Apply the supplied mapping function to the ok elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public <Z> @NonNull ResultBatch<Z> mapOks(@NonNull final IntFunction<List<Z>> fn) {
        return new ResultBatch<>(errors, fn.apply(okCount));
    }

    /**
     * Turn this ResultBatch into a VoidResult.  If errors is not empty, return a
     * {@link VoidResult.Err} containing the list of errors, dropping the oks.
     * Otherwise, return a {@link VoidResult.Ok}.
     */
    public @NonNull VoidResult fold() {
        return errors.isEmpty()
                ? VoidResult.ok()
                : VoidResult.err(joined(errors));
    }

    /**
     * Combine the errors and oks into a single type, for example, a JSON object that will
     * be returned to client code.
     */
    public <Z> @NonNull Z reduce(@NonNull final ObjIntFunction<List<AnyError>, Z> fn) {
        return fn.apply(errors, okCount);
    }

    /**
     * Operate on the two sets of values without producing a new value.
     */
    public void consume(@NonNull final ObjIntConsumer<List<AnyError>> fn) {
        fn.accept(errors, okCount);
    }

    /**
     * Return a new ResultBatch with all elements of this and all the elements of that appended.
     */
    public @NonNull VoidResultBatch append(@Nullable final VoidResultBatch that) {
        if (that == null) return this;
        return new VoidResultBatch(
                Data.append(errors, that.errors),
                okCount + that.okCount);
    }

    private record Acc(ArrayList<AnyError> errors, AtomicInteger okCount) {
        Acc() { this(new ArrayList<>(), new AtomicInteger(0)); }
        void add(final VoidResult result) { result.consume(errors::add, okCount::incrementAndGet); }
        Acc merge(final Acc other) {
            errors.addAll(other.errors);
            okCount.addAndGet(other.okCount.get());
            return this;
        }
        VoidResultBatch finish() { return new VoidResultBatch(errors, okCount.get()); }
    }
}
