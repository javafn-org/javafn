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
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

import static org.javafn.result.AnyError.joined;

public record DoubleResultBatch(@NonNull List<AnyError> errors, @NonNull double[] oks) {
    public DoubleResultBatch(@Nullable List<AnyError> errors, @Nullable double[] oks) {
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.oks = oks == null ? new double[0] : Arrays.copyOf(oks, oks.length);
    }

    /**
     * Construct an empty ResultBatch, useful for APIs that require a ResultBatch,
     * but calling/returning code does not have anything to report
     */
    public static @NonNull DoubleResultBatch empty() { return new DoubleResultBatch(List.of(), new double[0]); }

    public static @NonNull DoubleResultBatch of(@NonNull final DoubleResult... results) {
        return Arrays.stream(results).collect(collector());
    }

    public static @NonNull Collector<DoubleResult, ?, DoubleResultBatch> collector() {
        return Collector.of(
                DoubleResultBatch.Acc::new,
                Acc::add,
                DoubleResultBatch.Acc::merge,
                DoubleResultBatch.Acc::finish);
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
    public @NonNull DoubleResultBatch map(@NonNull final Function<AnyError, AnyError> errFn, @NonNull final DoubleUnaryOperator okFn) {
        return new DoubleResultBatch(
                errors.stream().map(errFn).toList(),
                Arrays.stream(oks).map(okFn).toArray());
    }

    /**
     * Apply the supplied mapping function to the error elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull DoubleResultBatch mapErrs(@NonNull final Function<AnyError, AnyError> fn) {
        return new DoubleResultBatch(errors.stream().map(fn).toList(), oks);
    }

    /**
     * Apply the supplied mapping function to the ok elements and produce a new ResultBatch
     * containing the result of the mapping.
     */
    public @NonNull DoubleResultBatch mapOks(@NonNull final DoubleUnaryOperator fn) {
        return new DoubleResultBatch(errors, Arrays.stream(oks).map(fn).toArray());
    }

    public <O> @NonNull ResultBatch<O> mapOksToObjs(@NonNull final DoubleFunction<O> fn) {
        return new ResultBatch<>(errors, Arrays.stream(oks).mapToObj(fn).toList());
    }

    /**
     * Turn this DoubleResultBatch longo a Result.  If errors is not empty, return a
     * {@link Result.Err} containing the list of errors, dropping the oks.
     * Otherwise, return a {@link Result.Ok} containing an array of oks.
     */
    public @NonNull Result<double[]> fold() {
        return errors.isEmpty()
                ? Result.ok(oks)
                : Result.err(joined(errors));
    }

    /**
     * Combine the errors and oks longo a single type, for example, a JSON object that will
     * be returned to client code.
     */
    public <Z> @NonNull Z reduce(@NonNull final BiFunction<List<AnyError>, double[], Z> fn) {
        return fn.apply(errors, oks);
    }

    /**
     * Operate on the two sets of values without producing a new value.
     */
    public void consume(@NonNull final BiConsumer<List<AnyError>, double[]> fn) {
        fn.accept(errors, oks);
    }

    /**
     * Return a new ResultBatch with all elements of this and all the elements of that appended.
     */
    public @NonNull DoubleResultBatch append(@Nullable final DoubleResultBatch that) {
        if (that == null) return this;
        return new DoubleResultBatch(
                Data.append(errors, that.errors),
                Data.append(oks, that.oks));
    }

    record Acc(ArrayList<AnyError> errors, DoubleStream.Builder oks) {
        Acc() { this(new ArrayList<>(), DoubleStream.builder()); }
        void add(final DoubleResult result) { result.consume(errors::add, oks::add); }
        DoubleResultBatch.Acc merge(final DoubleResultBatch.Acc other) {
            errors.addAll(other.errors);
            other.oks.build().forEachOrdered(oks::add);
            return this;
        }
        DoubleResultBatch finish() { return new DoubleResultBatch(errors, oks.build().toArray()); }
    }
}
