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
import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collector;
import java.util.stream.LongStream;

import static org.javafn.result.AnyError.joined;

public record LongResultBatch(@NonNull List<AnyError> errors, @NonNull long[] oks) {
    public LongResultBatch(@Nullable List<AnyError> errors, @Nullable long[] oks) {
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.oks = oks == null ? new long[0] : Arrays.copyOf(oks, oks.length);
    }

    /**
     * Construct an empty ResultBatch, useful for APIs that require a ResultBatch,
     * but calling/returning code does not have anything to report
     */
    public static @NonNull LongResultBatch empty() { return new LongResultBatch(List.of(), new long[0]); }

    public static @NonNull LongResultBatch of(@NonNull final LongResult... results) {
        return Arrays.stream(results).collect(collector());
    }

    public static @NonNull Collector<LongResult, ?, LongResultBatch> collector() {
        return Collector.of(
                LongResultBatch.Acc::new,
                Acc::add,
                LongResultBatch.Acc::merge,
                LongResultBatch.Acc::finish);
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
    public long size() { return errors.size() + oks.length; }

    /**
     * Apply the supplied mapping functions to their respective list elements and produce a new
     * ResultBatch containing the result of the mapping.
     */
    public @NonNull LongResultBatch map(@NonNull final Function<AnyError, AnyError> errFn, @NonNull final LongUnaryOperator okFn) {
        return new LongResultBatch(
                errors.stream().map(errFn).toList(),
                Arrays.stream(oks).map(okFn).toArray());
    }

    /**
     * Apply the supplied mapping function to the error elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull LongResultBatch mapErrs(@NonNull final Function<AnyError, AnyError> fn) {
        return new LongResultBatch(errors.stream().map(fn).toList(), oks);
    }

    /**
     * Apply the supplied mapping function to the ok elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull LongResultBatch mapOks(@NonNull final LongUnaryOperator fn) {
        return new LongResultBatch(errors, Arrays.stream(oks).map(fn).toArray());
    }

    public <O> @NonNull ResultBatch<O> mapOksToObjs(@NonNull final LongFunction<O> fn) {
        return new ResultBatch<>(errors, Arrays.stream(oks).mapToObj(fn).toList());
    }

    /**
     * Turn this LongResultBatch longo a Result.  If errors is not empty, return a
     * {@link Result.Err} containing the list of errors, dropping the oks.
     * Otherwise, return a {@link Result.Ok} containing an array of oks.
     */
    public @NonNull Result<long[]> fold() {
        return errors.isEmpty()
                ? Result.ok(oks)
                : Result.err(joined(errors));
    }

    /**
     * Combine the errors and oks longo a single type, for example, a JSON object that will
     * be returned to client code.
     */
    public <Z> @NonNull Z reduce(@NonNull final BiFunction<List<AnyError>, long[], Z> fn) {
        return fn.apply(errors, oks);
    }

    /**
     * Operate on the two sets of values without producing a new value.
     */
    public void consume(@NonNull final BiConsumer<List<AnyError>, long[]> fn) {
        fn.accept(errors, oks);
    }

    /**
     * Return a new ResultBatch with all elements of this and all the elements of that appended.
     */
    public @NonNull LongResultBatch append(@Nullable final LongResultBatch that) {
        if (that == null) return this;
        return new LongResultBatch(
                Data.append(errors, that.errors),
                Data.append(oks, that.oks));
    }

    record Acc(ArrayList<AnyError> errors, LongStream.Builder oks) {
        Acc() { this(new ArrayList<>(), LongStream.builder()); }
        void add(final LongResult result) { result.consume(errors::add, oks::add); }
        LongResultBatch.Acc merge(final LongResultBatch.Acc other) {
            errors.addAll(other.errors);
            other.oks.build().forEachOrdered(oks::add);
            return this;
        }
        LongResultBatch finish() { return new LongResultBatch(errors, oks.build().toArray()); }
    }
}
