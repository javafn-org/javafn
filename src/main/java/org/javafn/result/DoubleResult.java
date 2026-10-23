package org.javafn.result;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.DoubleToLongFunction;
import java.util.function.Function;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

public interface DoubleResult {
    /** Create a new Result wrapping a successful integer value. */
    static DoubleResult ok(final double ok) {
        return new DoubleResult.Ok(ok);
    }

    /** Create a new DoubleResult wrapping an error value. */
    static DoubleResult err(final AnyError err) {
        return new DoubleResult.Err(err);
    }
    /** Create a new DoubleResult wrapping an error message. */
    static DoubleResult err(final String errorMessage) {
        return new DoubleResult.Err(AnyError.from(errorMessage));
    }
    /** Create a new DoubleResult wrapping a list of error messages. */
    static DoubleResult err(final List<String> errorMessages) {
        return new DoubleResult.Err(AnyError.from(errorMessages));
    }
    /** Create a new DoubleResult wrapping an exception. */
    static <E extends Exception, OK> DoubleResult err(final E e) {
        return new DoubleResult.Err(AnyError.from(e));
    }

    /**
     * Collect a stream of {@link DoubleResult}s into a {@link ResultBatch}.
     * Since we don't have primitive lists, this function boxes the ok results.
     * @return a collector that generates a ResultBatch
     */
    static Collector<DoubleResult, ?, ResultBatch<Double>> batch() {
        return ResultBatch.doubleCollector();
    }

    /**
     * Return true if this is an err variant, false otherwise.  This function is useful in a stream pipeline.
     * More general usage would involve an instanceof check with pattern matching.
     * @return true iff this is an err variant
     */
    default boolean isErr() { return false; }

    /**
     * Return true if this is an ok variant, false otherwise.
     * @return true iff this is an ok variant
     */
    default boolean isOk() { return false; }

    /**
     * Unsafe operation, only safe in a branch opposite a test against ok
     * or inside a branch that checked {@link #isErr()}.
     * Access the error value or throw an exception.
     * <pre>{@code
     * final DoubleResult res = ...;
     * if (res instanceof Ok o) {
     *     final double v = o.value();
     * } else {
     *     AnyError e = res.expectErr();
     * }
     * }</pre>
     * @return the error value in this DoubleResult
     * @throws IllegalStateException if this DoubleResult is not actually an Err
     */
    default AnyError expectErr() {
        throw new IllegalStateException("Expected DoubleResult to be an Err instance, but it was not");
    }

    /**
     * Unsafe operation, only safe in a branch opposite a test against err
     * or inside a branch that checked {@link #isOk()}.
     * Access the ok value or throw an exception.
     * <pre>{@code
     * final DoubleResult res = ...;
     * if (res instanceof Err e) {
     *     return e.into();
     * }
     * final double v = o.value();
     * }</pre>
     * @return the ok value in this Result
     * @throws IllegalStateException if this Result is not actually an Ok
     */
    default double expectOk() {
        throw new IllegalStateException("Expected DoubleResult to be an Ok instance, but it was not");
    }

    default Optional<AnyError> optErr() {
        return Optional.empty();
    }

    default OptionalDouble opt() {
        return OptionalDouble.empty();
    }

    /**
     * Perform the appropriate mapping operation on this DoubleResult.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return the result with the appropriate function applied
     */
    DoubleResult map(Function<AnyError, AnyError> fnErr, DoubleUnaryOperator fnOk);

    /**
     * Apply the mapping function to the error value if this is an Err, otherwise return this
     * @param fn the function to apply to an error value
     * @return a new result with the error mapped or this unmodified if this is an ok
     */
    default DoubleResult mapErr(Function<AnyError, AnyError> fn) { return this; }

    /**
     * Apply the mapping function to the ok value if this is an Ok, otherwise return this
     * @param fn the function to apply to an ok value
     * @return a new result with the ok mapped or this unmodified if this is an err
     */
    default DoubleResult map(final DoubleUnaryOperator fn) { return this; };

    IntResult mapToInt(DoubleToIntFunction fn);
    LongResult mapToLong(DoubleToLongFunction fn);
    /**
     * Convert this DoubleResult into a {@link Result<Z>} by calling the supplied mapping function.
     * @return a Result with this ok double value mapped to an object or the err value unchanged
     */
    <Z> Result<Z> mapToObj(DoubleFunction<Z> fn);

    /**
     * Convert this DoubleResult into a {@link Result<Double>} by boxing the wrapped ok or passing the error unchanged.
     * @return a Result with this ok double value boxed or the err value unchanged
     */
    Result<Double> boxed();

    /**
     * Apply the supplied predicate to the wrapped error value or return true if this is an ok.
     * Semantically, {@code return (this instanceof Ok || fn.test(err))}
     * If you want {@code &&} semantics, use {@link #isErrAnd(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return true if this is not an error or the result of applying the predicate to the error value
     */
    default boolean filterErr(Predicate<AnyError> fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped ok value or return true if this is an err.
     * Semantically, {@code return (this instanceof Err || fn.test(ok))}
     * If you want {@code &&} semantics, use {@link #isOkAnd(DoublePredicate)}
     * @param fn the predicate to apply to the ok value
     * @return true if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean filter(DoublePredicate fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped error value or return false if this is an ok.
     * Semantically, {@code return (this instanceof Err && fn.test(err))}
     * If you want {@code ||} semantics, use {@link #filterErr(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return false if this is not an error or the result of applying the predicate to the error value
     */
    default boolean isErrAnd(Predicate<AnyError> fn) { return false; }

    /**
     * Apply the supplied predicate to the wrapped ok value or return false if this is an error.
     * Semantically, {@code return (this instanceof Ok && fn.test(ok))}
     * If you want {@code ||} semantics, use {@link #filter(DoublePredicate)}
     * @param fn the predicate to apply to the ok value
     * @return false if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean isOkAnd(DoublePredicate fn) { return false; }

    /**
     * Apply the supplied consumer to the wrapped err value, or if this is an ok, perform no operation.
     * <strong>
     *     This function is used to achieve side effects.
     *     You are strongly encouraged to document the side effect if it involves mutating external state.
     * </strong>
     * In the most innocuous case, you're writing to a log.
     * In other cases, you could be modifying a collection, for example,
     * Collecting a list of errors before filtering them from the pipeline.
     * @return this, unmodified
     */
    default DoubleResult ifErr(Consumer<AnyError> fn) {
        return this;
    }

    /**
     * Apply the supplied consumer to the wrapped ok value, or if this is an error, perform no operation.
     * <strong>
     *     This function is used to achieve side effects.
     *     You are strongly encouraged to document the side effect if it involves mutating external state.
     * </strong>
     * In the most innocuous case, you're writing to a log.
     * In other cases, you could be modifying a collection, for example,
     * when comparing entries from a database and entries from some other source of truth,
     * collecting a delta as a side effect of some primary operation.
     * You could remove processed entries from a list, and then anything left over after the pipeline runs
     * are things that are not present in the other source.
     * @param fn the consumer to apply to the wrapped ok value, performing side effects
     * @return this, unmodified
     */
    default DoubleResult ifOk(DoubleConsumer fn) {
        return this;
    }

    /**
     * If this result is an err, apply the supplied predicate to determine if recovery is possible,
     * and if that returns true, apply the supplied mapping function to the err value
     * and return an ok result with the new value.
     * @param testFn the predicate to apply to the err value
     * @param mapFn the mapping function to apply to the err value
     * @return a new result with the err value mapped to an ok if this is an err and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default DoubleResult recoverIf(Predicate<AnyError> testFn, ToDoubleFunction<AnyError> mapFn) { return this; }

    /**
     * If this result is an ok, apply the supplied predicate, and if that returns true, apply the
     * supplied mapping function to the ok value and return an err result with the result.
     * @param testFn the predicate to apply to the ok value
     * @param mapFn the mapping function to apply to the ok value
     * @return a new result with the ok value mapped to an err if this is an ok and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default DoubleResult failIf(DoublePredicate testFn, DoubleFunction<AnyError> mapFn) { return this; }

    /**
     * Apply the supplied mapping function, which itself returns a Result, to this Result's err value if it is an err.
     * If the mapping function returns an Err, return that err rather than an <pre>{@code Err<Err<o>,o>}</pre>.
     * In other words, if the supplied mapping function would result in
     * <pre>{@code
     * Result<Result<Foo>, Foo> res2 = res1.mapOk(...);
     * }</pre>
     * this function "flattens" it to
     * <pre>{@code
     * Result<Foo> res2 = res1.flatRecover(...);
     * }</pre>
     * Comparable to {@link #flatMap(DoubleFunction)}, except calling the function on the Err value and returning a flat Result.
     * @param fn a function that returns a result with the same ok type as this Result
     * @return this, if this Result is already an ok, otherwise, the result of applying the function to the err value
     */
    DoubleResult flatRecover(Function<AnyError, DoubleResult> fn);

    /**
     * Apply the supplied mapping function, which itself returns a Result, to this Result's ok value if it is an ok.
     * If the mapping function returns an Err, turn this into an Err rather than an <pre>{@code Ok<Err<o>>}</pre>.
     * In other words, if the supplied mapping function would result in
     * <pre>{@code
     * Result<Result<Foo>> res2 = res1.mapOk(...);
     * }</pre>
     * this function "flattens" it to
     * <pre>{@code
     * Result<Foo> res2 = res1.flatMap(...);
     * }</pre>
     * Comparable to {@link #flatRecover(Function)}, except calling the function on the Ok value and returning a flat Result.
     * @param fn a function that returns a result with the same error type as this Result
     * @return this, if this Result is already an Err, otherwise, the result of applying the function to the ok value
     */
    DoubleResult flatMap(DoubleFunction<DoubleResult> fn);

    /**
     * Apply the appropriate mapping function and return the resulting value.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return a value of some common type, produced by applying the appropriate mapping function
     * @param <Z> the result type
     */
    <Z> Z reduce(Function<AnyError, Z> fnErr, DoubleFunction<Z> fnOk);

    /**
     * Call the appropriate side effect producing function and do not return anything.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     */
    void consume(Consumer<AnyError> fnErr, DoubleConsumer fnOk);

    /**
     * An DoubleResult implementation that wraps an error value, obtained by calling {@link DoubleResult#err(AnyError)}.
     * To access {@link Err#value}, You should obtain an Err by doing an instanceof check
     * <pre>{@code
     * final Result<String> res = ...
     * if (res instanceof Err e) {
     *     final AnyError value = e.value();
     * }
     * }</pre>
     */
    record Err(AnyError value) implements DoubleResult {

        @Override public boolean isErr() { return true; }

        @Override public AnyError expectErr() { return value; }

        @Override public Optional<AnyError> optErr() { return Optional.ofNullable(value); }

        @Override public DoubleResult map(final Function<AnyError, AnyError> fnErr, final DoubleUnaryOperator fnOk) {
            return err(fnErr.apply(value));
        }

        @Override public IntResult mapToInt(DoubleToIntFunction fn) { return IntResult.err(value); }
        @Override public LongResult mapToLong(DoubleToLongFunction fn) { return LongResult.err(value); }
        @Override public <Z> Result<Z> mapToObj(DoubleFunction<Z> fn) { return Result.err(value); }
        @Override public Result<Double> boxed() { return Result.err(value); }

        @Override public DoubleResult mapErr(final Function<AnyError, AnyError> fn) { return err(fn.apply(value)); }

        @Override public boolean filterErr(final Predicate<AnyError> fn) { return fn.test(value); }

        @Override public boolean isErrAnd(Predicate<AnyError> fn) { return fn.test(value); }

        @Override public DoubleResult ifErr(final Consumer<AnyError> fn) {
            fn.accept(value);
            return this;
        }

        @Override public DoubleResult recoverIf(final Predicate<AnyError> testFn, final ToDoubleFunction<AnyError> mapFn) {
            if (testFn.test(value)) {
                return ok(mapFn.applyAsDouble(value));
            }
            return this;
        }

        @Override public DoubleResult flatMap(final DoubleFunction<DoubleResult> fn) { return this; }
        @Override public DoubleResult flatRecover(final Function<AnyError, DoubleResult> fn) {
            return fn.apply(value);
        }

        @Override public <Z> Z reduce(final Function<AnyError, Z> fnErr, final DoubleFunction<Z> fnOk) {
            return fnErr.apply(value);
        }
        @Override public void consume(final Consumer<AnyError> fnErr, final DoubleConsumer fnOk) {
            fnErr.accept(value);
        }
    }

    /**
     * An DoubleResult implementation that wraps an ok value, obtained by calling {@link DoubleResult#ok(double)}.
     * To access {@link Ok#value}, You should obtain an Ok by doing an instanceof check
     * <pre>{@code
     * final DoubleResult res = ...;
     * if (res instanceof Ok o) {
     *     final double value = o.value();
     * }
     * }</pre>
     * Note that this is safer than {@link DoubleResult#expectOk()} because that method throws an unchecked exception
     * if the result type is not an Ok while this method guarantees the type is Ok.  In most cases though,
     * you will use this pattern on the error case, typically returning res directly, and then calling
     * {@link DoubleResult#expectOk()} because Java 17 does not have exhaustive if statements and therefore can't
     * recognize the following doesn't need an else branch:
     * <pre>{@code
     * final double value;
     * {
     *     final DoubleResult res = ...;
     *     if (res instanceof Err e) {
     *          return e;
     *     } else if (res instanceof Ok o) {
     *         value = o.value();
     *     }
     *     // Compile error, value may be undefined.
     * }
     * }</pre>
     * This has been resolved in later versions of java.  We will likely remove the {@link DoubleResult#expectOk()}
     * methods when our minimum supported version can utilize the exhaustive test of the sealed type.
     */
    record Ok(double value) implements DoubleResult {

        @Override public boolean isOk() { return true; }

        @Override public double expectOk() { return value; }

        @Override public OptionalDouble opt() { return OptionalDouble.of(value); }

        @Override public DoubleResult map(final Function<AnyError, AnyError> fnErr, final DoubleUnaryOperator fnOk) {
            return ok(fnOk.applyAsDouble(value));
        }

        @Override public DoubleResult map(final DoubleUnaryOperator fn) { return ok(fn.applyAsDouble(value)); }
        @Override public IntResult mapToInt(DoubleToIntFunction fn) { return IntResult.ok(fn.applyAsInt(value)); }
        @Override public LongResult mapToLong(DoubleToLongFunction fn) { return LongResult.ok(fn.applyAsLong(value)); }
        @Override public <Z> Result<Z> mapToObj(DoubleFunction<Z> fn) { return Result.ok(fn.apply(value)); }
        @Override public Result<Double> boxed() { return Result.ok(value); }

        @Override public boolean filter(final DoublePredicate fn) { return fn.test(value); }

        @Override public boolean isOkAnd(DoublePredicate fn) { return fn.test(value); }

        @Override public DoubleResult ifOk(final DoubleConsumer fn) {
            fn.accept(value);
            return this;
        }

        @Override public DoubleResult failIf(final DoublePredicate testFn, final DoubleFunction<AnyError> mapFn) {
            if (testFn.test(value)) {
                return err(mapFn.apply(value));
            }
            return this;
        }

        @Override public DoubleResult flatRecover(final Function<AnyError, DoubleResult> fn) {
            return this;
        }
        @Override public DoubleResult flatMap(final DoubleFunction<DoubleResult> fn) { return fn.apply(value); }

        @Override public <Z> Z reduce(final Function<AnyError, Z> fnErr, final DoubleFunction<Z> fnOk) {
            return fnOk.apply(value);
        }

        @Override public void consume(final Consumer<AnyError> fnErr, final DoubleConsumer fnOk) {
            fnOk.accept(value);
        }
    }

    final class Errs {
        public static Function<DoubleResult, DoubleResult> map(final Function<AnyError, AnyError> fn) {
            return res -> res.mapErr(fn);
        }

        public static  Predicate<DoubleResult> filter(final Predicate<AnyError> fn) {
            return res -> res.filterErr(fn);
        }
        public static  Predicate<DoubleResult> isErrAnd(final Predicate<AnyError> fn) {
            return res -> res.isErrAnd(fn);
        }

        public static  Function<DoubleResult, DoubleResult> recoverIf(
                final Predicate<AnyError> testFn, final ToDoubleFunction<AnyError> mapFn) {
            return res -> res.recoverIf(testFn, mapFn);
        }

        public static <OK, Z> Function<DoubleResult, DoubleResult> flatRecover(
                final Function<AnyError, DoubleResult> fn) {
            return res -> res.flatRecover(fn);
        }

        public static  Consumer<DoubleResult> ifErr(final Consumer<AnyError> fn) {
            return res -> res.ifErr(fn);
        }

        private Errs() {
            throw new IllegalStateException("This is a static class and should never be instantiated");
        }
    }

    final class Oks {
        public static Function<DoubleResult, DoubleResult> map(final DoubleUnaryOperator fn) {
            return res -> res.map(fn);
        }

        public static  Predicate<DoubleResult> filter(final DoublePredicate fn) {
            return res -> res.filter(fn);
        }
        public static  Predicate<DoubleResult> isOkAnd(final DoublePredicate fn) {
            return res -> res.isOkAnd(fn);
        }

        public static  Function<DoubleResult, DoubleResult> failIf(
                final DoublePredicate testFn, final DoubleFunction<AnyError> mapFn) {
            return res -> res.failIf(testFn, mapFn);
        }

        public static Function<DoubleResult, DoubleResult> flatMap(final DoubleFunction<DoubleResult> fn) {
            return res -> res.flatMap(fn);
        }

        public static  Consumer<DoubleResult> ifOk(final DoubleConsumer fn) {
            return res -> res.ifOk(fn);
        }

        public static <OK> Function<DoubleResult, Result<OK>> mapToObj(DoubleFunction<OK> fn) {
            return res -> res.mapToObj(fn);
        }
        public static Function<DoubleResult, IntResult> mapToInt(DoubleToIntFunction fn) {
            return res -> res.mapToInt(fn);
        }
        public static Function<DoubleResult, LongResult> mapToLong(DoubleToLongFunction fn) {
            return res -> res.mapToLong(fn);
        }

        private Oks() {
            throw new IllegalStateException("This is a static class and should never be instantiated");
        }
    }
}
