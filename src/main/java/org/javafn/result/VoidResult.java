package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Expect;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

/**
 * An algebraic sum type that represents the possibly unsuccessful execution of an operation
 * that does not return a value on success.
 * <pre>{@code
 * someVoidOperation()
 *      .ifErr(ex -> System.err.println("An error occurred: " + ex.message()))
 *      .ifOk(() -> System.out.println("Operation completed successfully"));
 * }</pre>
 * This class is heavily inspired by Rust's Result type
 * <a href="https://doc.rust-lang.org/std/result/">https://doc.rust-lang.org/std/result/</a>.
 * One notable limitation is the lack of the {@code ?} operator.  There's nothing we can do about that.
 * The preferred pattern is to fluently chain operations on the result, however there are times when
 * an escape hatch is absolutely necessary.  Our preferred pattern is this:
 * <pre>{@code
 * {
 *     final VoidResult res = doCompute();
 *     if (res instanceof Err e) {
 *         return res.as(e).into();
 *     }
 * }
 * }</pre>
 * Notice that the result variable is contained within a scope block.  This is not required,
 * but it keeps scope clean.
 *
 * @see VoidResult.Err
 * @see VoidResult.Ok
 */
public sealed interface VoidResult permits VoidResult.Err, VoidResult.Ok {
    /**
     * Create a new Result indicating success with no associated value.
     */
    static @NonNull Ok ok() {return new Ok();}

    /** Create a new Result wrapping an error value. */
    static  @NonNull Err err(@NonNull final AnyError err) {
        return new Err(err);
    }
    /** Create a new Result wrapping an error message. */
    static  @NonNull Err err(@NonNull final String errorMessage) {
        return new Err(AnyError.from(errorMessage));
    }
    /** Create a new Result wrapping a list of error messages. */
    static  @NonNull Err err(@NonNull final List<String> errorMessages) {
        return new Err(AnyError.from(errorMessages));
    }
    /** Create a new Result wrapping an exception. */
    static <E extends Exception> @NonNull Err err(@NonNull final E e) {return new Err(AnyError.from(e));}
    static <E extends Exception> @NonNull Err err(@NonNull final E e, @Nullable final String errorMessage) {
        return new Err(AnyError.from(e, errorMessage));
    }
    static <E extends Enum<E>> @NonNull Err err(@NonNull final E e) { return new Err(AnyError.from(e)); }
    static <E extends Enum<E>> @NonNull Err err(@NonNull final E e, @Nullable final String errorMessage) {
        return new Err(AnyError.from(e, errorMessage));
    }
    /**
     * Collect a stream of {@link VoidResult}s into a {@link ResultBatch}.
     * @return a collector that generates a ResultBatch
     */
    static  @NonNull Collector<VoidResult, ?, VoidResultBatch> batch() {
        return VoidResultBatch.collector();
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
     * final Result<String> res = ...;
     * if (res instanceof Ok<?> o) {
     *     ... // do something with ok
     * } else {
     *     AnyError e = res.expectErr();
     * }
     * }</pre>
     * @return the error value in this Result
     * @throws IllegalStateException if this Result is not actually an Err
     */
    default @NonNull AnyError expectErr() {
        throw new IllegalStateException("Expected Result to be an Err instance, but it was not");
    }

    /**
     * Get the non-null error value of this result wrapped in an optional, or an empty optional if this result
     * is not an err or the wrapped err value is null.
     */
    default @NonNull Optional<AnyError> optErr() {
        return Optional.empty();
    }

    /**
     * Perform the appropriate mapping operation on this Result.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the supplier to apply if this is an Ok
     * @return a Result with the appropriate function applied
     * @param <NEWOK> the type of the ok after applying the function
     */
    <NEWOK> @NonNull Result<NEWOK> map(@NonNull Function<AnyError, AnyError> fnErr, @NonNull Supplier<NEWOK> fnOk);

    /**
     * Apply the mapping function to the error value if this is an Err, otherwise return this
     * @param fn the function to apply to an error value
     * @return a new result with the error mapped or this unmodified if this is an ok
     */
    default @NonNull VoidResult mapErr(@NonNull Function<AnyError, AnyError> fn) {
        return this;
    }

    /**
     * Apply the mapping function to the ok value if this is an Ok, otherwise return this
     * @param fn the function to apply to an ok value
     * @return a new result with the ok mapped or this unmodified if this is an err
     * @param <Z> the new ok type
     */
    <Z> @NonNull Result<Z> map(@NonNull Supplier<Z> fn);
    @NonNull IntResult mapToInt(@NonNull IntSupplier fn);
    @NonNull LongResult mapToLong(@NonNull LongSupplier fn);
    @NonNull DoubleResult mapToDouble(@NonNull DoubleSupplier fn);

    /**
     * Apply the supplied predicate to the wrapped error value or return true if this is an ok.
     * Semantically, {@code return (this instanceof Ok || fn.test(err))}
     * If you want {@code &&} semantics, use {@link #isErrAnd(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return true if this is not an error or the result of applying the predicate to the error value
     */
    default boolean filterErr(@NonNull Predicate<AnyError> fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped ok value or return true if this is an err.
     * Semantically, {@code return (this instanceof Err || fn.getAsBoolean)}
     * If you want {@code &&} semantics, use {@link #isOkAnd(BooleanSupplier)}
     * @param fn the predicate to apply to the ok value
     * @return true if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean filter(@NonNull BooleanSupplier fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped error value or return false if this is an ok.
     * Semantically, {@code return (this instanceof Err && fn.test(err))}
     * If you want {@code ||} semantics, use {@link #filterErr(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return false if this is not an error or the result of applying the predicate to the error value
     */
    default boolean isErrAnd(@NonNull Predicate<AnyError> fn) { return false; }

    /**
     * Apply the supplied predicate to the wrapped ok value or return false if this is an error.
     * Semantically, {@code return (this instanceof Ok && fn.getAsBoolean())}
     * If you want {@code ||} semantics, use {@link #filter(BooleanSupplier)}
     * @param fn the predicate to apply to the ok value
     * @return false if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean isOkAnd(@NonNull BooleanSupplier fn) { return false; }

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
    default @NonNull VoidResult ifErr(@NonNull Consumer<AnyError> fn) {
        return this;
    }

    /**
     * Execute the supplied runnable, or if this is an error, perform no operation.
     * <strong>
     *     This function is used to achieve side effects.
     *     You are strongly encouraged to document the side effect if it involves mutating external state.
     * </strong>
     * @param fn the runnable to execute
     * @return this, unmodified
     */
    default @NonNull VoidResult ifOk(@NonNull Runnable fn) {
        return this;
    }

    /**
     * If this result is an err, apply the supplied predicate to determine if recovery is possible,
     * and if that returns true, return an ok VoidResult.
     * @param testFn the predicate to apply to the err value
     * @return a new result with the err value mapped to an ok if this is an err and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default @NonNull VoidResult recoverIf(@NonNull Predicate<AnyError> testFn) { return this; }

    /**
     * If this result is an ok, apply the supplied predicate, and if that returns true, apply the
     * supplied mapping function to the ok value and return an err result with the result.
     * @param testFn the predicate to apply to the ok value
     * @param mapFn the mapping function to apply to the ok value
     * @return a new result with the ok value mapped to an err if this is an ok and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default @NonNull VoidResult failIf(@NonNull BooleanSupplier testFn, @NonNull Supplier<AnyError> mapFn) { return this; }

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
     * Comparable to {@link #flatMap(Supplier)}, except calling the function on the Err value and returning a flat Result.
     * @param fn a function that returns a result with the same ok type as this Result
     * @return this, if this Result is already an ok, otherwise, the result of applying the function to the err value
     */
    @NonNull
    VoidResult flatRecover(@NonNull Function<AnyError, VoidResult> fn);

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
     * @param <NEWOK> the type of the ok after mapping
     */
    <NEWOK> @NonNull Result<NEWOK> flatMap(@NonNull Supplier<Result<NEWOK>> fn);

    /**
     * Apply the appropriate mapping function and return the resulting value.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return a value of some common type, produced by applying the appropriate mapping function
     * @param <Z> the result type
     */
    <Z> @Nullable Z reduce(@NonNull Function<AnyError, Z> fnErr, @NonNull Supplier<Z> fnOk);

    /**
     * Call the appropriate side effect producing function and do not return anything.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     */
    void consume(Consumer<AnyError> fnErr, Runnable fnOk);

    /**
     * A Result implementation that wraps an error value, obtained by calling {@link VoidResult#err(AnyError)}.
     * To access {@link Err#value}, You should obtain an Err by doing an instanceof check
     * <pre>{@code
     * final Result<String> res = ...
     * if (res instanceof Err<?> e) {
     *     final AnyError value = e.value();
     * }
     * }</pre>
     */
    record Err(@NonNull AnyError value) implements VoidResult {

        @Override public boolean isErr() { return true; }

        public @NonNull String message() { return value.message(); }
        public @NonNull Exception except() { return value.except(); }
        public @NonNull RuntimeException exceptUnchecked() { return value.exceptUnchecked(); }
        public @NonNull AnyError join(final @NonNull AnyError that) { return value.join(that); }

        /**
         * Coerce the OK type to a new type and return this otherwise unmodified.
         * This function is not defined on Result.  You should obtain an Err by doing an instanceof check
         * <pre>{@code
         * public Result<Foo> demoFunction() {
         *     final Result<Bar> res = someOperation();
         *     if (res instanceof Err<?, ?> e) {
         *         return e.into();
         *     }
         *     final Bar bar = res.expectOk();
         * }
         * }</pre>
         *
         * @return this Err with the OK type coerced to a new type
         */
        public <Z> @NonNull Result.Err<Z> into() { return Result.err(value); }

        public @NonNull IntResult intoInt() { return IntResult.err(value); }
        public @NonNull LongResult intoLong() { return LongResult.err(value); }
        public @NonNull DoubleResult intoDouble() { return DoubleResult.err(value); }

        @Override public @NonNull AnyError expectErr() { return value; }

        @Override public @NonNull Optional<AnyError> optErr() { return Optional.of(value); }

        @Override public <Q> @NonNull Result<Q> map(@NonNull final Function<AnyError, AnyError> fnErr, @NonNull final Supplier<Q> fnOk) {
            return Result.err(fnErr.apply(value));
        }

        @Override public <Z> @NonNull Result<Z> map(@NonNull final Supplier<Z> fn) { return into(); }
        @Override public @NonNull IntResult mapToInt(@NonNull final IntSupplier fn) { return IntResult.err(value); }
        @Override public @NonNull LongResult mapToLong(@NonNull final LongSupplier fn) { return LongResult.err(value); }
        @Override public @NonNull DoubleResult mapToDouble(@NonNull final DoubleSupplier fn) { return DoubleResult.err(value); }

        @Override public @NonNull VoidResult mapErr(@NonNull final Function<AnyError, AnyError> fn) { return err(fn.apply(value)); }

        @Override public boolean filterErr(@NonNull final Predicate<AnyError> fn) { return fn.test(value); }

        @Override public boolean isErrAnd(@NonNull final Predicate<AnyError> fn) { return fn.test(value); }

        @Override public @NonNull VoidResult ifErr(@NonNull final Consumer<AnyError> fn) {
            fn.accept(value);
            return this;
        }

        @Override public @NonNull VoidResult recoverIf(@NonNull final Predicate<AnyError> testFn) {
            if (testFn.test(value)) {
                return ok();
            }
            return this;
        }

        @Override public <Z> @NonNull Result<Z> flatMap(@NonNull final Supplier<Result<Z>> fn) { return into(); }
        @Override public  @NonNull VoidResult flatRecover(@NonNull final Function<AnyError, VoidResult> fn) {
            return fn.apply(value);
        }

        @Override public <Z> @Nullable Z reduce(@NonNull final Function<AnyError, Z> fnErr, @NonNull final Supplier<Z> fnOk) {
            return fnErr.apply(value);
        }
        @Override public void consume(@NonNull final Consumer<AnyError> fnErr, @NonNull final Runnable fnOk) {
            fnErr.accept(value);
        }
    }

    /**
     * A VoidResult implementation that carries no value.
     */
    record Ok() implements VoidResult {

        @Override public boolean isOk() { return true; }

        @Override public <Q> @NonNull Result<Q> map(@NonNull final Function<AnyError, AnyError> fnErr, @NonNull final Supplier<Q> fnOk) {
            return Result.ok(fnOk.get());
        }

        @Override public <Z> @NonNull Result<Z> map(@NonNull final Supplier<Z> fn) { return Result.ok(fn.get()); }
        @Override public @NonNull IntResult mapToInt(@NonNull final IntSupplier fn) { return IntResult.ok(fn.getAsInt()); }
        @Override public @NonNull LongResult mapToLong(@NonNull final LongSupplier fn) { return LongResult.ok(fn.getAsLong()); }
        @Override public @NonNull DoubleResult mapToDouble(@NonNull final DoubleSupplier fn) { return DoubleResult.ok(fn.getAsDouble()); }

        @Override public boolean filter(@NonNull final BooleanSupplier fn) { return fn.getAsBoolean(); }

        @Override public boolean isOkAnd(@NonNull final BooleanSupplier fn) { return fn.getAsBoolean(); }

        @Override public @NonNull VoidResult ifOk(@NonNull final Runnable fn) {
            fn.run();
            return this;
        }

        @Override public @NonNull VoidResult failIf(@NonNull final BooleanSupplier testFn, @NonNull final Supplier<AnyError> mapFn) {
            if (testFn.getAsBoolean()) {
                return err(mapFn.get());
            }
            return this;
        }

        @Override public @NonNull VoidResult flatRecover(@NonNull final Function<AnyError, VoidResult> fn) {
            return this;
        }
        @Override public <Z> @NonNull Result<Z> flatMap(@NonNull final Supplier<Result<Z>> fn) { return fn.get(); }

        @Override public <Z> @Nullable Z reduce(@NonNull final Function<AnyError, Z> fnErr, @NonNull final Supplier<Z> fnOk) {
            return fnOk.get();
        }

        @Override public void consume(@NonNull final Consumer<AnyError> fnErr, @NonNull final Runnable fnOk) {
            fnOk.run();
        }
    }

    /**
     * Companion methods that return functions, for use in stream pipelines.
     * <pre>{@code
     * someStream
     *     .map(Errs.map(e -> fn(e))
     *     .filter(Errs.filter(not(e -> e == null)))
     *     .map(Errs.recoverIf(
     *         e -> e instanceof NumberFormatException,
     *         e -> -1)
     *     .forEach(Errs.ifErr(System.out::println));
     * }</pre>
     * Without these methods, the preceding would look like
     * <pre>{@code
     * someStream
     *     .map(res -> res.mapErr(e -> fn(e)))
     *     .filter(not(res -> res.filterErr(e -> e == null)))
     *     .map(res -> res.recoverIf(
     *         e -> e instanceof NumberFormatException,
     *         e -> -1)
     *     .forEach(res -> res.ifErr(System.out::println));
     * }</pre>
     * Namely, the text {@code res -> res} is repeated to the point of becoming noise.
     */
    final class Errs {
        public static  @NonNull Function<VoidResult, VoidResult> map(@NonNull final Function<AnyError, AnyError> fn) {
            return res -> res.mapErr(fn);
        }

        public static  @NonNull Predicate<VoidResult> filter(@NonNull final Predicate<AnyError> fn) {
            return res -> res.filterErr(fn);
        }
        public static  @NonNull Predicate<VoidResult> isErrAnd(@NonNull final Predicate<AnyError> fn) {
            return res -> res.isErrAnd(fn);
        }

        public static  @NonNull Function<VoidResult, VoidResult> recoverIf(
                @NonNull final Predicate<AnyError> testFn) {
            return res -> res.recoverIf(testFn);
        }

        public static  @NonNull Function<VoidResult, VoidResult> flatRecover(@NonNull final Function<AnyError, VoidResult> fn) {
            return res -> res.flatRecover(fn);
        }

        public static  @NonNull Consumer<VoidResult> ifErr(@NonNull final Consumer<AnyError> fn) {
            return res -> res.ifErr(fn);
        }

        private Errs() {Expect.staticConstructorNotCalled(Errs.class);}
    }

    /**
     * Companion methods that return functions, for use in stream pipelines.
     * <pre>{@code
     * someStream
     *     .map(Oks.map(o -> fn(o))
     *     .filter(Oks.filter(not(o -> o == null)))
     *     .map(Oks.failIf(
     *         o -> o.isEmpty(),
     *         e -> AnyError.fail("empty string"))
     *     .peek(Oks.ifOk(System.out::println));
     * }</pre>
     * Without these methods, the preceding would look like
     * <pre>{@code
     * someStream
     *     .map(res -> res.mapOk(o -> fn(o)))
     *     .filter(not(res -> res.filterOk(o -> o == null)))
     *     .map(res -> res.failIf(
     *         o -> o.isEmpty(),
     *         o -> AnyError.fail("empty string"))
     *     .peek(res -> res.ifOk(System.out::println));
     * }</pre>
     * Namely, the text {@code res -> res} is repeated to the point of becoming noise.
     */
    final class Oks {
        public static <OK, Z> @NonNull Function<VoidResult, Result<Z>> map(@NonNull final Supplier<Z> fn) {
            return res -> res.map(fn);
        }

        public static  @NonNull Predicate<VoidResult> filter(@NonNull final BooleanSupplier fn) {
            return res -> res.filter(fn);
        }
        public static  @NonNull Predicate<VoidResult> isOkAnd(@NonNull final BooleanSupplier fn) {
            return res -> res.isOkAnd(fn);
        }

        public static  @NonNull Function<VoidResult, VoidResult> failIf(
                @NonNull final BooleanSupplier testFn, @NonNull final Supplier<AnyError> mapFn) {
            return res -> res.failIf(testFn, mapFn);
        }

        public static <OK, Z> @NonNull Function<VoidResult, Result<Z>> flatMap(@NonNull final Supplier<Result<Z>> fn) {
            return res -> res.flatMap(fn);
        }

        public static  @NonNull Consumer<VoidResult> ifOk(@NonNull final Runnable fn) {
            return res -> res.ifOk(fn);
        }
        public static  @NonNull Function<VoidResult, IntResult> mapToInt(@NonNull IntSupplier fn) {
            return res -> res.mapToInt(fn);
        }
        public static  @NonNull Function<VoidResult, LongResult> mapToLong(@NonNull LongSupplier fn) {
            return res -> res.mapToLong(fn);
        }
        public static  @NonNull Function<VoidResult, DoubleResult> mapToDouble(@NonNull DoubleSupplier fn) {
            return res -> res.mapToDouble(fn);
        }

        private Oks() {Expect.staticConstructorNotCalled(Oks.class);}
    }
}
