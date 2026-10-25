package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Expect;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

/**
 * An algebraic sum type that represents the possibly unsuccessful execution of an operation.
 * Similar to an Either in many languages, a Result wraps a value of type AnyError or of type OK,
 * but never both.  'null' is a valid value, but the Result will either be an err or an ok,
 * and the other value will be empty (similar to Optional.empty()).
 * <pre>{@code
 * final var theUrl = "http://localhost:8080/example";
 * Result.ok(theUrl)
 *      .flatMap(Try.toMap(URL::new))
 *          .flatMap(Try.toMap(URL::openConnection))
 *          .flatMap(Try.toMap(URLConnection::getInputStream))
 *          .map(is -> new BufferedReader(new InputStreamReader(is)))
 *          .map(BufferedReader::lines)
 *          .ifErr(ex -> System.err.println("An exception occurred trying to fetch the url.  " + ex.message()))
 *          .ifOk(lines -> System.out.println("URL contents from demoResult: "
 *                  + lines.collect(Collectors.joining("\n"))));
 * }</pre>
 * This class is heavily inspired by Rust's Result type
 * <a href="https://doc.rust-lang.org/std/result/">https://doc.rust-lang.org/std/result/</a>.
 * One notable limitation is the lack of the {@code ?} operator.  There's nothing we can do about that.
 * The preferred pattern is to fluently chain operations on the result, however there are times when
 * an escape hatch is absolutely necessary.  Our preferred pattern is this:
 * <pre>{@code
 * public Result<Long> testIt() {
 *     final Integer i; {
 *         final Result<Integer> res = doCompute();
 *         if (res instanceof Err<?> e) {
 *             return res.as(e).into();
 *         }
 *         i = res.expectOk();
 *     }
 *     return ok(i.longValue());
 * }
 * }</pre>
 * Notice that the result variable is contained within a scope block.  This is not required,
 * but it keeps scope clean.  The variable of interest is defined outside of the block and assigned
 * at the end of the block.  The error is handled with a return and the ok is handled with an assignment.
 *
 * @param <OK>  the type of value stored if this is an ok
 * @see Result.Err
 * @see Result.Ok
 */
public sealed interface Result<OK> permits Result.Err, Result.Ok {
     /** Create a new Result wrapping a successful value. */
    static <OK> @NonNull Result<OK> ok(@Nullable final OK ok) {
        return new Ok<>(ok);
    }
    /**
     * Create a new Result indicating success with no associated value.
     * A Void typed Ok result is similar to {@code Optional<ErrorType>} with the added semantics
     * that a present value indicates an error and lack of a value indicates success, which is not clear
     * purely from the API when an Optional is used.
     */
    static @NonNull Result<Void> ok() {
        return new Ok<>(null);
    }

    /** Create a new Result wrapping an error value. */
    static <OK> @NonNull Result<OK> err(@NonNull final AnyError err) {
        return new Err<>(err);
    }
    /** Create a new Result wrapping an error message. */
    static <OK> @NonNull Result<OK> err(@NonNull final String errorMessage) {
        return new Err<>(AnyError.from(errorMessage));
    }
    /** Create a new Result wrapping a list of error messages. */
    static <OK> @NonNull Result<OK> err(@NonNull final List<String> errorMessages) {
        return new Err<>(AnyError.from(errorMessages));
    }
    /** Create a new Result wrapping an exception. */
    static <E extends Exception, OK> @NonNull Result<OK> err(@NonNull final E e) {return new Err<>(AnyError.from(e));}
    static <E extends Exception, OK> @NonNull Result<OK> err(@NonNull final E e, @Nullable final String errorMessage) {
        return new Err<>(AnyError.from(e, errorMessage));
    }
    static <E extends Enum<E>, OK> @NonNull Result<OK> err(@NonNull final E e) { return new Err<>(AnyError.from(e)); }
    static <E extends Enum<E>, OK> @NonNull Result<OK> err(@NonNull final E e, @Nullable final String errorMessage) {
        return new Err<>(AnyError.from(e, errorMessage));
    }
    /**
     * Collect a stream of {@link Result}s into a {@link ResultBatch}.
     * @return a collector that generates a ResultBatch
     */
    static <OK> @NonNull Collector<Result<OK>, ?, ResultBatch<OK>> batch() {
        return ResultBatch.collector();
    }

    /**
     * After performing an instanceof check, which loses type information,
     * turn this Result into a concrete Ok type with the generics restored.
     * <pre>{@code
     * final Result<String> res = ...;
     * if (res instanceof Ok<?> o) {
     *     // note that res == o
     *     final Ok<String> ok = res.as(o);
     *     final String value = ok.value();
     * }
     * }</pre>
     * You may be inclined to cast your result and pass it to this method, for example
     * <pre>{@code
     * final Result<String> res = ...;
     * if (res.isOk()) {
     *     // DO NOT DO IT THIS WAY!
     *     final Ok<String> ok = res.as((Ok<?>) res);
     *     final String value = ok.value();
     * }
     * }</pre>
     * This is syntactically legal, but there are easier ways to do this.
     * <pre>{@code
     * final Result<String> res = ...;
     * if (res.isOk()) {
     *     final String value = res.expectOk();
     * }
     * }</pre>
     * You should never use the cast approach and treat its legality as an oddity.
     * This is the only safe way to obtain an Ok object from a result, which defines several additional
     * methods that can not be safely implemented on a result.
     * @param thiz the same object this method is being called on after performing a pattern match
     * @return this with the type parameters restored
     * @throws IllegalArgumentException if the parameter is anything except the method receiver
     * @see Ok#value()
     */
    default @NonNull Ok<OK> as(@NonNull Ok<?> thiz) {
        throw new IllegalArgumentException("as(Ok) called on non-Ok instance; use instanceof Ok and its pattern variable");
    }

    /**
     * Return true if this is an err variant, false otherwise.  This function is useful in a stream pipeline.
     * More general usage would involve an instanceof check with pattern matching.
     * @return true iff this is an err variant
     */
    default boolean isErr() { return false; }

    /**
     * Return true if this is an ok variant, false otherwise.  This function is useful in a stream pipeline.
     * More general usage would involve an instanceof check with pattern matching followed by {@link #as(Ok)}.
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
     * Unsafe operation, only safe in a branch opposite a test against err
     * or inside a branch that checked {@link #isOk()}.
     * Access the ok value or throw an exception.
     * <pre>{@code
     * final Result<String> res = ...;
     * if (res instanceof Err<?> e) {
     *     return e.into();
     * }
     * final String value = res.expectOk();
     * }</pre>
     * @return the ok value in this Result
     * @throws IllegalStateException if this Result is not actually an Ok
     */
    default @Nullable OK expectOk() {
        throw new IllegalStateException("Expected Result to be an Ok instance, but it was not");
    }

    /**
     * Get the non-null error value of this result wrapped in an optional, or an empty optional if this result
     * is not an err or the wrapped err value is null.
     */
    default @NonNull Optional<AnyError> optErr() {
        return Optional.empty();
    }

    /**
     * Get the non-null ok value of this result wrapped in an optional, or an empty optional if this result
     * is not an ok or the wrapped ok value is null.
     */
    default @NonNull Optional<OK> opt() {
        return Optional.empty();
    }

    /**
     * Perform the appropriate mapping operation on this Result.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return the result with the appropriate function applied
     * @param <NEWOK> the type of the ok after applying the function
     */
    <NEWOK> @NonNull Result<NEWOK> map(@NonNull Function<AnyError, AnyError> fnErr, @NonNull Function<OK, NEWOK> fnOk);

    /**
     * Apply the mapping function to the error value if this is an Err, otherwise return this
     * @param fn the function to apply to an error value
     * @return a new result with the error mapped or this unmodified if this is an ok
     */
    default @NonNull Result<OK> mapErr(@NonNull Function<AnyError, AnyError> fn) {
        return this;
    }

    /**
     * Apply the mapping function to the ok value if this is an Ok, otherwise return this
     * @param fn the function to apply to an ok value
     * @return a new result with the ok mapped or this unmodified if this is an err
     * @param <Z> the new ok type
     */
    <Z> @NonNull Result<Z> map(@NonNull Function<OK, Z> fn);
    @NonNull IntResult mapToInt(@NonNull ToIntFunction<OK> fn);
    @NonNull LongResult mapToLong(@NonNull ToLongFunction<OK> fn);
    @NonNull DoubleResult mapToDouble(@NonNull ToDoubleFunction<OK> fn);

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
     * Semantically, {@code return (this instanceof Err || fn.test(ok))}
     * If you want {@code &&} semantics, use {@link #isOkAnd(Predicate)}
     * @param fn the predicate to apply to the ok value
     * @return true if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean filter(@NonNull Predicate<OK> fn) {
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
     * Semantically, {@code return (this instanceof Ok && fn.test(ok))}
     * If you want {@code ||} semantics, use {@link #filter(Predicate)}
     * @param fn the predicate to apply to the ok value
     * @return false if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean isOkAnd(@NonNull Predicate<OK> fn) { return false; }

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
    default @NonNull Result<OK> ifErr(@NonNull Consumer<AnyError> fn) {
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
    default @NonNull Result<OK> ifOk(@NonNull Consumer<OK> fn) {
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
    default @NonNull Result<OK> recoverIf(@NonNull Predicate<AnyError> testFn, @NonNull Function<AnyError, OK> mapFn) { return this; }

    /**
     * If this result is an ok, apply the supplied predicate, and if that returns true, apply the
     * supplied mapping function to the ok value and return an err result with the result.
     * @param testFn the predicate to apply to the ok value
     * @param mapFn the mapping function to apply to the ok value
     * @return a new result with the ok value mapped to an err if this is an ok and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default @NonNull Result<OK> failIf(@NonNull Predicate<OK> testFn, @NonNull Function<OK, AnyError> mapFn) { return this; }

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
     * Comparable to {@link #flatMap(Function)}, except calling the function on the Err value and returning a flat Result.
     * @param fn a function that returns a result with the same ok type as this Result
     * @return this, if this Result is already an ok, otherwise, the result of applying the function to the err value
     */
    @NonNull Result<OK> flatRecover(@NonNull Function<AnyError, Result<OK>> fn);

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
    <NEWOK> @NonNull Result<NEWOK> flatMap(@NonNull Function<OK, Result<NEWOK>> fn);

    /**
     * Apply the appropriate mapping function and return the resulting value.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return a value of some common type, produced by applying the appropriate mapping function
     * @param <Z> the result type
     */
    <Z> @Nullable Z reduce(@NonNull Function<AnyError, Z> fnErr, @NonNull Function<OK, Z> fnOk);

    /**
     * Call the appropriate side effect producing function and do not return anything.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     */
    void consume(Consumer<AnyError> fnErr, Consumer<OK> fnOk);

    /**
     * A Result implementation that wraps an error value, obtained by calling {@link Result#err(AnyError)}.
     * To access {@link Err#value}, You should obtain an Err by doing an instanceof check
     * <pre>{@code
     * final Result<String> res = ...
     * if (res instanceof Err<?> e) {
     *     final AnyError value = e.value();
     * }
     * }</pre>
     */
    record Err<OK>(@NonNull AnyError value) implements Result<OK> {

        @Override public boolean isErr() { return true; }

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
        public <Z> @NonNull Result<Z> into() { return err(value); }

        public @NonNull IntResult intoInt() { return IntResult.err(value); }
        public @NonNull LongResult intoLong() { return LongResult.err(value); }
        public @NonNull DoubleResult intoDouble() { return DoubleResult.err(value); }

        @Override public @NonNull AnyError expectErr() { return value; }

        @Override public @NonNull Optional<AnyError> optErr() { return Optional.of(value); }

        @Override public <Q> @NonNull Result<Q> map(@NonNull final Function<AnyError, AnyError> fnErr, @NonNull final Function<OK, Q> fnOk) {
            return err(fnErr.apply(value));
        }

        @Override public <Z> @NonNull Result<Z> map(@NonNull final Function<OK, Z> fn) { return into(); }
        @Override public @NonNull IntResult mapToInt(@NonNull final ToIntFunction<OK> fn) { return IntResult.err(value); }
        @Override public @NonNull LongResult mapToLong(@NonNull final ToLongFunction<OK> fn) { return LongResult.err(value); }
        @Override public @NonNull DoubleResult mapToDouble(@NonNull final ToDoubleFunction<OK> fn) { return DoubleResult.err(value); }

        @Override public @NonNull Result<OK> mapErr(@NonNull final Function<AnyError, AnyError> fn) { return err(fn.apply(value)); }

        @Override public boolean filterErr(@NonNull final Predicate<AnyError> fn) { return fn.test(value); }

        @Override public boolean isErrAnd(@NonNull final Predicate<AnyError> fn) { return fn.test(value); }

        @Override public @NonNull Result<OK> ifErr(@NonNull final Consumer<AnyError> fn) {
            fn.accept(value);
            return this;
        }

        @Override public @NonNull Result<OK> recoverIf(@NonNull final Predicate<AnyError> testFn, @NonNull final Function<AnyError, OK> mapFn) {
            if (testFn.test(value)) {
                return ok(mapFn.apply(value));
            }
            return this;
        }

        @Override public <Z> @NonNull Result<Z> flatMap(@NonNull final Function<OK, Result<Z>> fn) { return into(); }
        @Override public @NonNull Result<OK> flatRecover(@NonNull final Function<AnyError, Result<OK>> fn) {
            return fn.apply(value);
        }

        @Override public <Z> @Nullable Z reduce(@NonNull final Function<AnyError, Z> fnErr, @NonNull final Function<OK, Z> fnOk) {
            return fnErr.apply(value);
        }
        @Override public void consume(@NonNull final Consumer<AnyError> fnErr, @NonNull final Consumer<OK> fnOk) {
            fnErr.accept(value);
        }
    }

    /**
     * A Result implementation that wraps an ok value, obtained by calling {@link Result#ok(Object)}.
     * To access {@link Ok#value}, You should obtain an Ok by doing an instanceof check
     * <pre>{@code
     * final Result<Foo> res = ...; // Foo is known, but Ok is not
     * if (res instanceof Ok<?> o) {// Ok is known but Foo is lost
     *     final Foo value = res    // Result<Foo>
     *          .as(o)              // Ok<Foo>
     *          .value();           // Foo
     * }
     * }</pre>
     * Note that this is safer than {@link Result#expectOk()} because that method throws an unchecked exception
     * if the result type is not an Ok while this method guarantees the type is Ok.  In most cases though,
     * you will use this pattern on the error case, typically returning {@link Err#into()}, and then calling
     * {@link Result#expectOk()} because Java 17 does not have exhaustive if statements and therefore can't
     * recognize the following doesn't need an else branch:
     * <pre>{@code
     * final Foo value;
     * {
     *     final Result<Foo> res = ...;
     *     if (res instanceof Err<?> e) {
     *          return e.into();
     *     } else if (res instanceof Ok<?>) {
     *         value = res.as(o).value();
     *     }
     *     // Compile error, value may be undefined.
     * }
     * }</pre>
     * This has been resolved in later versions of java.  We will likely remove the {@link Result#expectOk()}
     * methods when our minimum supported version can utilize the exhaustive test of the sealed type.
     */
    record Ok<OK>(@Nullable OK value) implements Result<OK> {

        @Override
        public @NonNull Ok<OK> as(@NonNull final Ok<?> thiz) {
            if (thiz != this)
                throw new IllegalArgumentException("Ok.as: argument must be the pattern variable bound to this Ok");
            return this;
        }

        @Override public boolean isOk() { return true; }

        @Override public @Nullable OK expectOk() { return value; }

        @Override public @NonNull Optional<OK> opt() { return Optional.ofNullable(value); }

        @Override public <Q> @NonNull Result<Q> map(@NonNull final Function<AnyError, AnyError> fnErr, @NonNull final Function<OK, Q> fnOk) {
            return ok(fnOk.apply(value));
        }

        @Override public <Z> @NonNull Result<Z> map(@NonNull final Function<OK, Z> fn) { return ok(fn.apply(value)); }
        @Override public @NonNull IntResult mapToInt(@NonNull final ToIntFunction<OK> fn) { return IntResult.ok(fn.applyAsInt(value)); }
        @Override public @NonNull LongResult mapToLong(@NonNull final ToLongFunction<OK> fn) { return LongResult.ok(fn.applyAsLong(value)); }
        @Override public @NonNull DoubleResult mapToDouble(@NonNull final ToDoubleFunction<OK> fn) { return DoubleResult.ok(fn.applyAsDouble(value)); }

        @Override public boolean filter(@NonNull final Predicate<OK> fn) { return fn.test(value); }

        @Override public boolean isOkAnd(@NonNull final Predicate<OK> fn) { return fn.test(value); }

        @Override public @NonNull Result<OK> ifOk(@NonNull final Consumer<OK> fn) {
            fn.accept(value);
            return this;
        }

        @Override public @NonNull Result<OK> failIf(@NonNull final Predicate<OK> testFn, @NonNull final Function<OK, AnyError> mapFn) {
            if (testFn.test(value)) {
                return err(mapFn.apply(value));
            }
            return this;
        }

        @Override public @NonNull Result<OK> flatRecover(@NonNull final Function<AnyError, Result<OK>> fn) {
            return this;
        }
        @Override public <Z> @NonNull Result<Z> flatMap(@NonNull final Function<OK, Result<Z>> fn) { return fn.apply(value); }

        @Override public <Z> @Nullable Z reduce(@NonNull final Function<AnyError, Z> fnErr, @NonNull final Function<OK, Z> fnOk) {
            return fnOk.apply(value);
        }

        @Override public void consume(@NonNull final Consumer<AnyError> fnErr, @NonNull final Consumer<OK> fnOk) {
            fnOk.accept(value);
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
        public static <OK> @NonNull Function<Result<OK>, Result<OK>> map(@NonNull final Function<AnyError, AnyError> fn) {
            return res -> res.mapErr(fn);
        }

        public static <OK> @NonNull Predicate<Result<OK>> filter(@NonNull final Predicate<AnyError> fn) {
            return res -> res.filterErr(fn);
        }
        public static <OK> @NonNull Predicate<Result<OK>> isErrAnd(@NonNull final Predicate<AnyError> fn) {
            return res -> res.isErrAnd(fn);
        }

        public static <OK> @NonNull Function<Result<OK>, Result<OK>> recoverIf(
                @NonNull final Predicate<AnyError> testFn, @NonNull final Function<AnyError, OK> mapFn) {
            return res -> res.recoverIf(testFn, mapFn);
        }

        public static <OK> @NonNull Function<Result<OK>, Result<OK>> flatRecover(@NonNull final Function<AnyError, Result<OK>> fn) {
            return res -> res.flatRecover(fn);
        }

        public static <OK> @NonNull Consumer<Result<OK>> ifErr(@NonNull final Consumer<AnyError> fn) {
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
        public static <OK, Z> @NonNull Function<Result<OK>, Result<Z>> map(@NonNull final Function<OK, Z> fn) {
            return res -> res.map(fn);
        }

        public static <OK> @NonNull Predicate<Result<OK>> filter(@NonNull final Predicate<OK> fn) {
            return res -> res.filter(fn);
        }
        public static <OK> @NonNull Predicate<Result<OK>> isOkAnd(@NonNull final Predicate<OK> fn) {
            return res -> res.isOkAnd(fn);
        }

        public static <OK> @NonNull Function<Result<OK>, Result<OK>> failIf(
                @NonNull final Predicate<OK> testFn, @NonNull final Function<OK, AnyError> mapFn) {
            return res -> res.failIf(testFn, mapFn);
        }

        public static <OK, Z> @NonNull Function<Result<OK>, Result<Z>> flatMap(@NonNull final Function<OK, Result<Z>> fn) {
            return res -> res.flatMap(fn);
        }

        public static <OK> @NonNull Consumer<Result<OK>> ifOk(@NonNull final Consumer<OK> fn) {
            return res -> res.ifOk(fn);
        }
        public static <OK> @NonNull Function<Result<OK>, IntResult> mapToInt(@NonNull ToIntFunction<OK> fn) {
            return res -> res.mapToInt(fn);
        }
        public static <OK> @NonNull Function<Result<OK>, LongResult> mapToLong(@NonNull ToLongFunction<OK> fn) {
            return res -> res.mapToLong(fn);
        }
        public static <OK> @NonNull Function<Result<OK>, DoubleResult> mapToLong(@NonNull ToDoubleFunction<OK> fn) {
            return res -> res.mapToDouble(fn);
        }

        private Oks() {Expect.staticConstructorNotCalled(Oks.class);}
    }
}
