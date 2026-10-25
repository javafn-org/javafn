package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static org.javafn.result.AnyError.joined;

public record IntResultBatch(@NonNull List<AnyError> errors, @NonNull int[] oks) {
    public IntResultBatch(@Nullable List<AnyError> errors, @Nullable int[] oks) {
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.oks = oks == null ? new int[0] : Arrays.copyOf(oks, oks.length);
    }

    /**
     * Construct an empty ResultBatch, useful for APIs that require a ResultBatch,
     * but calling/returning code does not have anything to report
     */
    public static @NonNull IntResultBatch empty() { return new IntResultBatch(List.of(), new int[0]); }

    public static @NonNull IntResultBatch of(@NonNull final IntResult... results) {
        return Arrays.stream(results).collect(collector());
    }

    public static @NonNull Collector<IntResult, ?, IntResultBatch> collector() {
        return Collector.of(
                IntResultBatch.Acc::new,
                Acc::add,
                IntResultBatch.Acc::merge,
                IntResultBatch.Acc::finish);
    }

    /** true if {@link #errors} has one or more elements */
    public boolean hasErrors() { return !errors.isEmpty(); }
    /** true if {@link #oks} has one or more elements */
    public boolean hasOks() { return oks.length > 0; }
    /** true if {@link #errors} has at least one element and {@link #oks} has no elements */
    public boolean isOnlyErrors() { return !errors.isEmpty() && oks.length == 0; }
    /** true if {@link #errors} has no elements and {@link #oks} has at least one element */
    public boolean isOnlyOks() { return errors.isEmpty() && oks.length > 0; }
    /** true if both {@link #errors} and {@link #oks} has at least one element each */
    public boolean hasBoth() { return !errors.isEmpty() && oks.length > 0; }

    /** The number of errors and oks */
    public int size() { return errors.size() + oks.length; }

    /**
     * Apply the supplied mapping functions to their respective list elements and produce a new
     * ResultBatch containing the result of the mapping.
     */
    public @NonNull IntResultBatch map(@NonNull final Function<AnyError, AnyError> errFn, @NonNull final IntUnaryOperator okFn) {
        return new IntResultBatch(
                errors.stream().map(errFn).toList(),
                Arrays.stream(oks).map(okFn).toArray());
    }

    /**
     * Apply the supplied mapping function to the error elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull IntResultBatch mapErrs(@NonNull final Function<AnyError, AnyError> fn) {
        return new IntResultBatch(errors.stream().map(fn).toList(), oks);
    }

    /**
     * Apply the supplied mapping function to the ok elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull IntResultBatch mapOks(@NonNull final IntUnaryOperator fn) {
        return new IntResultBatch(errors, Arrays.stream(oks).map(fn).toArray());
    }

    public <O> @NonNull ResultBatch<O> mapOksToObjs(@NonNull final IntFunction<O> fn) {
        return new ResultBatch<>(errors, Arrays.stream(oks).mapToObj(fn).toList());
    }

    /**
     * Turn this IntResultBatch into a Result.  If errors is not empty, return a
     * {@link org.javafn.result.Result.Err} containing the list of errors, dropping the oks.
     * Otherwise, return a {@link org.javafn.result.Result.Ok} containing an array of oks.
     */
    public @NonNull Result<int[]> fold() {
        return errors.isEmpty()
                ? Result.ok(oks)
                : Result.err(joined(errors));
    }

    /**
     * Combine the errors and oks into a single type, for example, a JSON object that will
     * be returned to client code.
     */
    public <Z> @NonNull Z reduce(@NonNull final BiFunction<List<AnyError>, int[], Z> fn) {
        return fn.apply(errors, oks);
    }

    /**
     * Operate on the two sets of values without producing a new value.
     */
    public void consume(@NonNull final BiConsumer<List<AnyError>, int[]> fn) {
        fn.accept(errors, oks);
    }

    /**
     * Return a new ResultBatch with all elements of this and all the elements of that appended.
     */
    public @NonNull IntResultBatch append(@Nullable final IntResultBatch that) {
        if (that == null) return this;
        return new IntResultBatch(
                Data.append(errors, that.errors),
                Data.append(oks, that.oks));
    }

    record Acc(ArrayList<AnyError> errors, IntStream.Builder oks) {
        Acc() { this(new ArrayList<>(), IntStream.builder()); }
        void add(final IntResult result) { result.consume(errors::add, oks::add); }
        IntResultBatch.Acc merge(final IntResultBatch.Acc other) {
            errors.addAll(other.errors);
            other.oks.build().forEachOrdered(oks::add);
            return this;
        }
        IntResultBatch finish() { return new IntResultBatch(errors, oks.build().toArray()); }
    }
}
