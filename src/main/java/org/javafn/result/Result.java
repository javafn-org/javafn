package org.javafn.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * An algebraic sum type that represents the possibly unsuccessful execution of an operation.
 * Similar to an Either in many languages, a Result wraps a value of type ERR or of type OK,
 * but never both.  'null' is a valid value, but the Result will either be an err or an ok,
 * and the other value will be empty (similar to Optional.empty()).
 * <pre>{@code
 * final var theUrl = "http://localhost:8080/example";
 * Result.<AnyError, String>ok(theUrl)
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
 * public Result<String, Long> testIt() {
 *     final Integer i; {
 *         final Result<AnyError, Integer> res = doCompute();
 *         if (res instanceof Err<?, ?> e) {
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
 * @param <ERR> the type of value stored if this is an err
 * @param <OK>  the type of value stored if this is an ok
 * @see Result.Err
 * @see Result.Ok
 */
public sealed interface Result<ERR, OK> permits Result.Err, Result.Ok {
     /** Create a new Result wrapping a successful value. */
    static <ERR, OK> Result<ERR, OK> ok(final OK ok) {
        return new Ok<>(ok);
    }
    /**
     * Create a new Result indicating success with no associated value.
     * A Void typed Ok result is similar to {@code Optional<ErrorType>} with the added semantics
     * that a present value indicates an error and lack of a value indicates success, which is not clear
     * purely from the API when an Optional is used.
     */
    static <ERR> Result<ERR, Void> ok() {
        return new Ok<>(null);
    }

    /** Create a new Result wrapping an error value. */
    static <ERR, OK> Result<ERR, OK> err(final ERR err) {
        return new Err<>(err);
    }

    /**
     * Collect a stream of {@link Result}s into a {@link ResultBatch}.
     * @return a collector that generates a ResultBatch
     */
    static <ERR, OK> Collector<Result<ERR, OK>, ?, ResultBatch<ERR, OK>> batch() {
        return ResultBatch.collector();
    }

    /**
     * After performing an instanceof check, turn this Result into a concrete Err type.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res instanceof Err<?, ?> e) {
     *     final Err<AnyError, String> err = res.as(e);
     *     final AnyError value = err.value();
     * }
     * }</pre>
     * You may be inclined to cast your result and pass it to this method, for example
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isErr()) {
     *     final Err<AnyError, String> err = res.as((Err<?, ?>) res);
     *     final AnyError value = err.value();
     * }
     * }</pre>
     * This is syntactically legal, but there are easier ways to do this.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isErr()) {
     *     final AnyError value = res.expectErr();
     * }
     * }</pre>
     * You should never use the cast approach and treat its legality as an oddity.
     * This is the only safe way to obtain an Err object from a result, which defines several additional
     * methods that can not be safely implemented on a result.
     * @param thiz the same object this method is being called on after performing a pattern match
     * @return this with the type parameters restored
     * @throws IllegalArgumentException if the parameter is anything except the method receiver
     * @see Err#value()
     * @see Err#into()
     */
    default Err<ERR, OK> as(Err<?, ?> thiz) {
        throw new IllegalArgumentException("as(Err) called on non-Err instance; use instanceof Err and its pattern variable");
    }

    /**
     * After performing an instanceof check, turn this Result into a concrete Ok type.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res instanceof Ok<?, ?> o) {
     *     final Ok<AnyError, String> ok = res.as(o);
     *     final String value = ok.value();
     * }
     * }</pre>
     * You may be inclined to cast your result and pass it to this method, for example
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isOk()) {
     *     final Ok<AnyError, String> ok = res.as((Ok<?, ?>) res);
     *     final String value = ok.value();
     * }
     * }</pre>
     * This is syntactically legal, but there are easier ways to do this.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
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
     * @see Ok#into()
     */
    default Ok<ERR, OK> as(Ok<?, ?> thiz) {
        throw new IllegalArgumentException("as(Ok) called on non-Ok instance; use instanceof Ok and its pattern variable");
    }

    /**
     * Return true if this is an err variant, false otherwise.  This function is useful in a stream pipeline.
     * More general usage would involve an instanceof check with pattern matching followed by {@link #as(Err)}.
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
     * Unsafe operation, only safe in a branch opposite a test against ok.
     * Access the error value or throw an exception.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res instanceof Ok<?,?> o) {
     *     ... // do something with ok
     * } else {
     *     AnyError e = res.expectErr();
     * }
     * }</pre>
     * @return the error value in this Result
     * @throws IllegalStateException if this Result is not actually an Err
     */
    default ERR expectErr() {
        throw new IllegalStateException("Expected Result to be an Err instance, but it was not");
    }

    /**
     * Unsafe operation, only safe in a branch opposite a test against err.
     * Access the ok value or throw an exception.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res instanceof Err<?,?> e) {
     *     return res.as(e).into();
     * }
     * final String value = res.expectOk();
     * }</pre>
     * @return the ok value in this Result
     * @throws IllegalStateException if this Result is not actually an Ok
     */
    default OK expectOk() {
        throw new IllegalStateException("Expected Result to be an Ok instance, but it was not");
    }

    /**
     * Get the non-null error value of this result wrapped in an optional, or an empty optional if this result
     * is not an err or the wrapped err value is null.
     */
    default Optional<ERR> optErr() {
        return Optional.empty();
    }

    /**
     * Get the non-null ok value of this result wrapped in an optional, or an empty optional if this result
     * is not an ok or the wrapped ok value is null.
     */
    default Optional<OK> opt() {
        return Optional.empty();
    }

    /**
     * Perform the appropriate mapping operation on this Result.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return the result with the appropriate function applied
     * @param <NEWERR> the type of the error after applying the function
     * @param <NEWOK> the type of the ok after applying the function
     */
    <NEWERR, NEWOK> Result<NEWERR, NEWOK> map(Function<ERR, NEWERR> fnErr, Function<OK, NEWOK> fnOk);

    /**
     * Apply the mapping function to the error value if this is an Err, otherwise return this
     * @param fn the function to apply to an error value
     * @return a new result with the error mapped or this unmodified if this is an ok
     * @param <Z> the new error type
     */
    <Z> Result<Z, OK> mapErr(Function<ERR, Z> fn);

    /**
     * Apply the mapping function to the ok value if this is an Ok, otherwise return this
     * @param fn the function to apply to an ok value
     * @return a new result with the ok mapped or this unmodified if this is an err
     * @param <Z> the new ok type
     */
    <Z> Result<ERR, Z> map(Function<OK, Z> fn);

    /**
     * Apply the supplied predicate to the wrapped error value or return true if this is an ok.
     * Semantically, {@code return (this instanceof Ok || fn.test(err))}
     * If you want {@code &&} semantics, use {@link #isErrAnd(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return true if this is not an error or the result of applying the predicate to the error value
     */
    default boolean filterErr(Predicate<ERR> fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped ok value or return true if this is an err.
     * Semantically, {@code return (this instanceof Err || fn.test(ok))}
     * If you want {@code &&} semantics, use {@link #isOkAnd(Predicate)}
     * @param fn the predicate to apply to the ok value
     * @return true if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean filter(Predicate<OK> fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped error value or return false if this is an ok.
     * Semantically, {@code return (this instanceof Err && fn.test(err))}
     * If you want {@code ||} semantics, use {@link #filterErr(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return false if this is not an error or the result of applying the predicate to the error value
     */
    default boolean isErrAnd(Predicate<ERR> fn) { return false; }

    /**
     * Apply the supplied predicate to the wrapped ok value or return false if this is an error.
     * Semantically, {@code return (this instanceof Ok && fn.test(ok))}
     * If you want {@code ||} semantics, use {@link #filter(Predicate)}
     * @param fn the predicate to apply to the ok value
     * @return false if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean isOkAnd(Predicate<OK> fn) { return false; }

    /**
     * Apply the supplied consumer to the wrapped err value, or if this is an ok, perform no operation.
     * <strong>This function is used to achieve side effects.
     * You are strongly encouraged to document the side effect if it involves mutating external state.</strong>
     * In the most innocuous case, you're writing to a log.
     * In other cases, you could be modifying a collection, for example,
     * Collecting a list of errors before filtering them from the pipeline.
     * @return this, unmodified
     */
    default Result<ERR, OK> ifErr(Consumer<ERR> fn) {
        return this;
    }

    /**
     * Apply the supplied consumer to the wrapped ok value, or if this is an error, perform no operation.
     * <strong>This function is used to achieve side effects.
     * You are strongly encouraged to document the side effect if it involves mutating external state.</strong>
     * In the most innocuous case, you're writing to a log.
     * In other cases, you could be modifying a collection, for example,
     * when comparing entries from a database and entries from some other source of truth,
     * collecting a delta as a side effect of some primary operation.
     * You could remove processed entries from a list, and then anything left over after the pipeline runs
     * are things that are not present in the other source.
     * @param fn the consumer to apply to the wrapped ok value, performing side effects
     * @return this, unmodified
     */
    default Result<ERR, OK> ifOk(Consumer<OK> fn) {
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
    default Result<ERR, OK> recoverIf(Predicate<ERR> testFn, Function<ERR, OK> mapFn) { return this; }

    /**
     * If this result is an ok, apply the supplied predicate, and if that returns true, apply the
     * supplied mapping function to the ok value and return an err result with the result.
     * @param testFn the predicate to apply to the ok value
     * @param mapFn the mapping function to apply to the ok value
     * @return a new result with the ok value mapped to an err if this is an ok and the supplied predicate matches,
     * otherwise return this unmodified
     */
    default Result<ERR, OK> failIf(Predicate<OK> testFn, Function<OK, ERR> mapFn) { return this; }

    /**
     * Apply the supplied mapping function, which itself returns a Result, to this Result's err value if it is an err.
     * If the mapping function returns an Err, return that err rather than an <pre>{@code Err<Err<e,o>,o>}</pre>.
     * In other words, if the supplied mapping function would result in
     * <pre>{@code
     * Result<Result<AnyError, Foo>, Foo> res2 = res1.mapOk(...);
     * }</pre>
     * this function "flattens" it to
     * <pre>{@code
     * Result<AnyError, Foo> res2 = res1.flatRecover(...);
     * }</pre>
     * Comparable to {@link #flatMap(Function)}, except calling the function on the Err value and returning a flat Result.
     * @param fn a function that returns a result with the same ok type as this Result
     * @return this, if this Result is already an ok, otherwise, the result of applying the function to the err value
     * @param <NEWERR> the type of the err after mapping
     */
    <NEWERR> Result<NEWERR, OK> flatRecover(Function<ERR, Result<NEWERR, OK>> fn);

    /**
     * Apply the supplied mapping function, which itself returns a Result, to this Result's ok value if it is an ok.
     * If the mapping function returns an Err, turn this into an Err rather than an <pre>{@code Ok<e,Err<e, o>>}</pre>.
     * In other words, if the supplied mapping function would result in
     * <pre>{@code
     * Result<AnyError, Result<AnyError, Foo>> res2 = res1.mapOk(...);
     * }</pre>
     * this function "flattens" it to
     * <pre>{@code
     * Result<AnyError, Foo> res2 = res1.flatMap(...);
     * }</pre>
     * Comparable to {@link #flatRecover(Function)}, except calling the function on the Ok value and returning a flat Result.
     * @param fn a function that returns a result with the same error type as this Result
     * @return this, if this Result is already an Err, otherwise, the result of applying the function to the ok value
     * @param <NEWOK> the type of the ok after mapping
     */
    <NEWOK> Result<ERR, NEWOK> flatMap(Function<OK, Result<ERR, NEWOK>> fn);

    /**
     * Apply the appropriate mapping function and return the resulting value.
     * @param fnErr the function to apply if this is an Err
     * @param fnOk the function to apply if this is an Ok
     * @return a value of some common type, produced by applying the appropriate mapping function
     * @param <Z> the result type
     */
    <Z> Z reduce(Function<ERR, Z> fnErr, Function<OK, Z> fnOk);

    void use(Consumer<ERR> fnErr, Consumer<OK> fnOk);

    /**
     * Swap the type parameters and convert this into the opposing type.
     */
    Result<OK, ERR> swap();

    /**
     * A Result implementation that wraps an error value, obtained by calling {@link Result#err(Object)}.
     * To access {@link Err#value}, You should obtain an Err by doing an instanceof check
     * <pre>{@code
     * final Result<AnyError, String> res = ...
     * if (res instanceof Err<?, ?> e) {
     *     final AnyError value = res // Result<AnyError, String>
     *          .as(e)                // Err<AnyError, String>
     *          .value();             // AnyError
     * }
     * }</pre>
     */
    record Err<ERR, OK>(ERR value) implements Result<ERR, OK> {

        @Override public Err<ERR, OK> as(final Err<?, ?> thiz) {
            if (thiz != this)
                throw new IllegalArgumentException("Err.as: argument must be the pattern variable bound to this Err");
            return this;
        }

        @Override public boolean isErr() { return true; }

        /**
         * Coerce the OK type to a new type and return this otherwise unmodified.
         * This function is not defined on Result.  You should obtain an Err by doing an instanceof check
         * <pre>{@code
         * public Result<AnyError, Foo> demoFunction() {
         *     final Result<AnyError, Bar> res = someOperation();
         *     if (res instanceof Err<?, ?> e) {
         *         return res      // Result<AnyError, Bar>
         *              .as(e)     // Err<AnyError, Bar>
         *              .into();   // Result<AnyError, Foo>
         *     }
         *     final Bar bar = res.expectOk();
         * }
         * }</pre>
         *
         * @return this Err with the OK type coerced to a new type
         */
        public <Z> Result<ERR, Z> into() { return err(value); }

        @Override public ERR expectErr() { return value; }

        @Override public Optional<ERR> optErr() { return Optional.ofNullable(value); }

        @Override public <P, Q> Result<P, Q> map(Function<ERR, P> fnErr, Function<OK, Q> fnOk) {
            return err(fnErr.apply(value));
        }

        @Override public <Z> Result<ERR, Z> map(final Function<OK, Z> fn) { return into(); }

        @Override public <Z> Result<Z, OK> mapErr(final Function<ERR, Z> fn) { return err(fn.apply(value)); }

        @Override public boolean filterErr(final Predicate<ERR> fn) { return fn.test(value); }

        @Override public boolean isErrAnd(Predicate<ERR> fn) { return fn.test(value); }

        @Override public Result<ERR, OK> ifErr(final Consumer<ERR> fn) {
            fn.accept(value);
            return this;
        }

        @Override public Result<ERR, OK> recoverIf(final Predicate<ERR> testFn, final Function<ERR, OK> mapFn) {
            if (testFn.test(value)) {
                return ok(mapFn.apply(value));
            }
            return this;
        }

        @Override public <Z> Result<ERR, Z> flatMap(final Function<OK, Result<ERR, Z>> fn) { return into(); }
        @Override public <NEWERR> Result<NEWERR, OK> flatRecover(final Function<ERR, Result<NEWERR, OK>> fn) {
            return fn.apply(value);
        }

        @Override public <Z> Z reduce(final Function<ERR, Z> fnErr, final Function<OK, Z> fnOk) {
            return fnErr.apply(value);
        }
        @Override public void use(final Consumer<ERR> fnErr, final Consumer<OK> fnOk) {
            fnErr.accept(value);
        }

        @Override public Result<OK, ERR> swap() { return ok(value); }
    }

    /**
     * A Result implementation that wraps an ok value, obtained by calling {@link Result#ok(Object)}.
     * To access {@link Ok#value}, You should obtain an Ok by doing an instanceof check
     * <pre>{@code
     * final Result<AnyError, Foo> res = ...;
     * if (res instanceof Ok<?, ?> o) {
     *     final Foo value = res    // Result<AnyError, Foo>
     *          .as(o)              // Ok<AnyError, Foo>
     *          .value();           // Foo
     * }
     * }</pre>
     * Note that this is safer than {@link Result#expectOk()} because that method throws an unchecked exception
     * if the result type is not an Ok while this method guarantees the type is Ok.  In most cases though,
     * you will use this pattern on the error case, typically returning {@link Err#into()}, and then calling
     * {@link Result#expectOk()} because Java 17 does not have exhaustive if statements and therefore can't
     * recognize the following doesn't need an else branch:
     * <pre>{@code
     * final Result<AnyError, Foo> res = ...;
     * final Foo value;
     * if (res instanceof Err<?, ?> e) {
     *      return res.as(e).into();
     * } else if (res instanceof Ok<?, ?>) {
     *     value = res.as(o).value();
     * }
     * // Compile error, value may be undefined.
     * }</pre>
     * This has been resolved in later versions of java.  We will likely remove the {@link Result#expectOk()}
     * methods when our minimum supported version can utilize the exhaustive test of the sealed type.
     */
    record Ok<ERR, OK>(OK value) implements Result<ERR, OK> {

        @Override
        public Ok<ERR, OK> as(final Ok<?, ?> thiz) {
            if (thiz != this)
                throw new IllegalArgumentException("Ok.as: argument must be the pattern variable bound to this Ok");
            return this;
        }

        @Override public boolean isOk() { return true; }

        /**
         * Coerce the ERR type to a new type and return this otherwise unmodified.
         * This function is not defined on Result.  You should obtain an Ok by doing an instanceof check
         * <pre>{@code
         * public Result<AnyError, Foo> demoFunction() {
         *     final Result<Exception, Foo> res = someOperation();
         *     if (res instanceof Ok<?, ?> o) {
         *         return res       // Result<Exception, Foo>
         *              .as(o)      // Ok<Exception, Foo>
         *              .into();    // Result<AnyError, Foo>
         *     }
         *     return AnyError.from(res.expectErr());
         * }
         * }</pre>
         * Note that there are better ways of achieving what's shown in the demo function above, for example
         * <pre>{@code
         * public Result<AnyError, Foo> demoFunction() {
         *     return someOperation().mapErr(AnyError::from);
         * }
         * }</pre>
         * This function is less useful on the Ok type, but it's included for symmetry.
         *
         * @return this Ok with the ERR type coerced to a new type
         */
        public <Z> Result<Z, OK> into() { return ok(value); }

        @Override public OK expectOk() { return value; }

        @Override public Optional<OK> opt() { return Optional.ofNullable(value); }

        @Override public <P, Q> Result<P, Q> map(final Function<ERR, P> fnErr, final Function<OK, Q> fnOk) {
            return ok(fnOk.apply(value));
        }

        @Override public <Z> Result<ERR, Z> map(final Function<OK, Z> fn) { return ok(fn.apply(value)); }

        @Override public <Z> Result<Z, OK> mapErr(final Function<ERR, Z> fn) { return into(); }

        @Override public boolean filter(final Predicate<OK> fn) { return fn.test(value); }

        @Override public boolean isOkAnd(Predicate<OK> fn) { return fn.test(value); }

        @Override public Result<ERR, OK> ifOk(final Consumer<OK> fn) {
            fn.accept(value);
            return this;
        }

        @Override public Result<ERR, OK> failIf(final Predicate<OK> testFn, final Function<OK, ERR> mapFn) {
            if (testFn.test(value)) {
                return err(mapFn.apply(value));
            }
            return this;
        }

        @Override public <NEWERR> Result<NEWERR, OK> flatRecover(final Function<ERR, Result<NEWERR, OK>> fn) {
            return into();
        }
        @Override public <Z> Result<ERR, Z> flatMap(final Function<OK, Result<ERR, Z>> fn) { return fn.apply(value); }

        @Override public <Z> Z reduce(final Function<ERR, Z> fnErr, final Function<OK, Z> fnOk) {
            return fnOk.apply(value);
        }

        @Override public void use(final Consumer<ERR> fnErr, final Consumer<OK> fnOk) {
            fnOk.accept(value);
        }

        @Override public Result<OK, ERR> swap() { return err(value); }
    }

    /**
     * Companion methods that return functions, for use in stream pipelines.
     * <pre>{@code
     * someStream
     *     .map(Errs.map(e -> fn(e))
     *     .filter(Errs.filter(e -> e == null))
     *     .map(Errs.recoverIf(
     *         e -> e instanceof NumberFormatException,
     *         e -> -1)
     *     .forEach(Errs.ifErr(System.out::println));
     * }</pre>
     * Without these methods, the preceding would look like
     * <pre>{@code
     * someStream
     *     .map(res -> res.mapErr(e -> fn(e)))
     *     .filter(res -> res.filterErr(e -> e == null))
     *     .map(res -> res.recoverIf(
     *         e -> e instanceof NumberFormatException,
     *         e -> -1)
     *     .forEach(res -> res.ifErr(System.out::println));
     * }</pre>
     * Namely, the text {@code res -> res} is repeated to the point of becoming noise.
     */
    final class Errs {
        public static <OK> Function<Result<String, OK>, Result<AnyError, OK>> any() {
            return res -> res.mapErr(AnyError::from);
        }
        public static <ERR, OK, Z> Function<Result<ERR, OK>, Result<Z, OK>> map(final Function<ERR, Z> fn) {
            return res -> res.mapErr(fn);
        }

        public static <ERR, OK> Predicate<Result<ERR, OK>> filter(final Predicate<ERR> fn) {
            return res -> res.filterErr(fn);
        }
        public static <ERR, OK> Predicate<Result<ERR, OK>> isErrAnd(final Predicate<ERR> fn) {
            return res -> res.isErrAnd(fn);
        }

        public static <ERR, OK> Function<Result<ERR, OK>, Result<ERR, OK>> recoverIf(
                final Predicate<ERR> testFn, final Function<ERR, OK> mapFn) {
            return res -> res.recoverIf(testFn, mapFn);
        }

        public static <ERR, OK, Z> Function<Result<ERR, OK>, Result<Z, OK>> flatRecover(
                final Function<ERR, Result<Z, OK>> fn) {
            return res -> res.flatRecover(fn);
        }

        public static <ERR, OK> Function<Result<ERR, OK>, Result<OK, ERR>> swap() {
            return Result::swap;
        }

        public static <ERR, OK> Consumer<Result<ERR, OK>> ifErr(final Consumer<ERR> fn) {
            return res -> res.ifErr(fn);
        }

        private Errs() {
            throw new IllegalStateException("This is a static class and should never be instantiated");
        }
    }

    /**
     * Companion methods that return functions, for use in stream pipelines.
     * <pre>{@code
     * someStream
     *     .map(Oks.map(o -> fn(o))
     *     .filter(Oks.filter(o -> o == null))
     *     .map(Oks.failIf(
     *         o -> o.isEmpty(),
     *         e -> AnyError.fail("empty string"))
     *     .peek(Oks.ifOk(System.out::println));
     * }</pre>
     * Without these methods, the preceding would look like
     * <pre>{@code
     * someStream
     *     .map(res -> res.mapOk(o -> fn(o)))
     *     .filter(res -> res.filterOk(o -> o == null))
     *     .map(res -> res.failIf(
     *         o -> o.isEmpty(),
     *         o -> AnyError.fail("empty string"))
     *     .peek(res -> res.ifOk(System.out::println));
     * }</pre>
     * Namely, the text {@code res -> res} is repeated to the point of becoming noise.
     */
    final class Oks {
        public static <ERR, OK, Z> Function<Result<ERR, OK>, Result<ERR, Z>> map(final Function<OK, Z> fn) {
            return res -> res.map(fn);
        }

        public static <ERR, OK> Predicate<Result<ERR, OK>> filter(final Predicate<OK> fn) {
            return res -> res.filter(fn);
        }
        public static <ERR, OK> Predicate<Result<ERR, OK>> isOkAnd(final Predicate<OK> fn) {
            return res -> res.isOkAnd(fn);
        }

        public static <ERR, OK> Function<Result<ERR, OK>, Result<ERR, OK>> failIf(
                final Predicate<OK> testFn, final Function<OK, ERR> mapFn) {
            return res -> res.failIf(testFn, mapFn);
        }

        public static <ERR, OK, Z> Function<Result<ERR, OK>, Result<ERR, Z>> flatMap(final Function<OK, Result<ERR, Z>> fn) {
            return res -> res.flatMap(fn);
        }

        public static <ERR, OK> Function<Result<ERR, OK>, Result<OK, ERR>> swap() {
            return Result::swap;
        }

        public static <ERR, OK> Consumer<Result<ERR, OK>> ifOk(final Consumer<OK> fn) {
            return res -> res.ifOk(fn);
        }

        private Oks() {
            throw new IllegalStateException("This is a static class and should never be instantiated");
        }
    }
}
