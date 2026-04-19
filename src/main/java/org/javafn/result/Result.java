package org.javafn.result;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An algebraic sum type that represents the possibly unsuccessful execution of an operation.
 * Similar to an Either in many languages, a Result wraps a value of type ERR or of type OK,
 * but never both.  'null' is a valid value, but the Result will either be an err or an ok,
 * and the other value will be empty (similar to Optional.empty()).
 * <pre>{@code
 * Result.<Exception, String>ok(theUrl)
 *      .flatMap(u -> Try.get(() -> new URL(u)))
 *      .flatMap(Try.Map(URL::openConnection))
 *      .flatMap(Try.Map(URLConnection::getInputStream))
 *      .mapOk(is -> new BufferedReader(new InputStreamReader(is)))
 *      .mapOk(BufferedReader::lines)
 *      .ifErr(ex -> System.err.println("An exception occurred trying to fetch the url."))
 *      .ifErr(Exception::printStackTrace)
 *      .optOk()
 *      .ifPresent(lines -> System.out.println("URL contents from demoResult: "
 *              + lines.collect(Collectors.joining("\n"))));
 * }</pre>
 * This class is heavily inspired by Rust's Result type
 * <a href="https://doc.rust-lang.org/std/result/">https://doc.rust-lang.org/std/result/</a>
 * </a>.
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

    /** Create a new Result wrapping an error value. */
    static <ERR, OK> Result<ERR, OK> err(final ERR err) {
        return new Err<>(err);
    }

    /**
     * After performing an instanceof check, turn this Result into a concrete Err type.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res instanceof Err<?, ?> e) {
     *     final Err<AnyError, String> err = res.as(e);
     *     final AnyError v = err.get();
     * }
     * }</pre>
     * You may be inclined to cast your result and pass it to this method, for example
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isErr()) {
     *     final Err<AnyError, String> err = res.as((Err<?, ?>) res);
     *     final AnyError v = err.get();
     * }
     * }</pre>
     * This is syntactically legal, but there are easier ways to do this.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isErr()) {
     *     final AnyError v = res.expectErr();
     * }
     * }</pre>
     * You should never use the cast approach and treat its legality as an oddity.
     * This is the only safe way to obtain an Err object from a result, which defines several additional
     * methods that can not be safely implemented on a result.
     * @param thiz the same object this method is being called on after performing a pattern match
     * @return this with the type parameters restored
     * @throws IllegalArgumentException if the parameter is anything except the method receiver
     * @see Err#get()
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
     *     final String v = ok.get();
     * }
     * }</pre>
     * You may be inclined to cast your result and pass it to this method, for example
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isOk()) {
     *     final Ok<AnyError, String> ok = res.as((Ok<?, ?>) res);
     *     final String v = ok.get();
     * }
     * }</pre>
     * This is syntactically legal, but there are easier ways to do this.
     * <pre>{@code
     * final Result<AnyError, String> res = ...;
     * if (res.isOk()) {
     *     final String v = err.expectOk();
     * }
     * }</pre>
     * You should never use the cast approach and treat its legality as an oddity.
     * This is the only safe way to obtain an Ok object from a result, which defines several additional
     * methods that can not be safely implemented on a result.
     * @param thiz the same object this method is being called on after performing a pattern match
     * @return this with the type parameters restored
     * @throws IllegalArgumentException if the parameter is anything except the method receiver
     * @see Ok#get()
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

//    /** Map the error into an AnyError if this is an err */
//    Result<AnyError, OK> any();

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
     * final String v = res.expectOk();
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
    default Optional<OK> optOk() {
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
    <Z> Result<ERR, Z> mapOk(Function<OK, Z> fn);

    /**
     * Apply the supplied predicate to the wrapped error value or return true if this is an ok.
     * Semantically, {@code return (this instanceof OK || fn.test(err))}
     * If you want {@code &&} semantics, use {@link #isErr()}
     * @param fn the predicate to apply to the error value
     * @return true if this is not an error or the result of applying the predicate to the error value
     */
    default boolean filterErr(Predicate<ERR> fn) {
        return true;
    }

    /**
     * Apply the supplied predicate to the wrapped ok value or return true if this is an err.
     * Semantically, {@code return (this instanceof Err || fn.test(ok))}
     * If you want {@code &&} semantics, use {@link #isOk()}
     * @param fn the predicate to apply to the ok value
     * @return true if this is not an ok or the result of applying the predicate to the ok value
     */
    default boolean filterOk(Predicate<OK> fn) {
        return true;
    }

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
     * Apply the supplied mapping function, which itself returns a Result, to this Result's ok value if it is an ok.
     * If the mapping function returns an Err, turn this into an Err rather than an <pre>{@code Ok<Result<...>>}</pre>.
     * In other words, if the supplied mapping function would result in
     * <pre>{@code
     * Result<AnyError, Result<AnyError, Foo>> res2 = res1.mapOk(...);
     * }</pre>
     * this function "flattens" it to
     * <pre>{@code
     * Result<AnyError, Foo> res2 = res1.flatMap(...);
     * }</pre>
     * There is no symmetric function for errors.
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

    /**
     * Swap the type parameters and convert this into the opposing type.
     */
    Result<OK, ERR> swap();

    /**
     * A Result implementation that wraps an error value, obtained by calling {@link Result#err(Object)}.
     */
    final class Err<ERR, OK> implements Result<ERR, OK> {
        private final ERR v;

        private Err(final ERR _err) {v = _err;}

        @Override
        public Err<ERR, OK> as(final Err<?, ?> thiz) {
            if (thiz != this)
                throw new IllegalArgumentException("Err.as: argument must be the pattern variable bound to this Err");
            return this;
        }

        @Override public boolean isErr() { return true; }

//        @Override Result<AnyError, OK> any() {
//            if (v instanceof String s) {
//                return err(AnyError.from(s));
//            } else if (v instanceof Exception e) {
//                return err(AnyError.from(e));
//            } else if (v instanceof List<?> l) {
//                return err(AnyError.from(l));
//            } else if (v instanceof Enum<?> e) {
//                return err(AnyError.from(e));
//            }
//            return err(AnyError.from(v));
//        }

        /**
         * Get the value contained within this Err.  This function is not defined on Result.  You should obtain an
         * Err by doing an instanceof check
         * <pre>{@code
         * if (res instanceof Err<?, ?> e) {
         *     final AnyError v = res.as(e).get();
         * }
         * }</pre>
         * @return the value wrapped in this Err result
         */
        public ERR get() { return v; }

        /**
         * Coerce the OK type to a new type and return this otherwise unmodified.
         * This function is not defined on Result.  You should obtain an Err by doing an instanceof check
         * <pre>{@code
         * public Result<AnyErr, Foo> demoFunction() {
         *     final Result<AnyErr, Bar> res = someOperation();
         *     if (res instanceof Err<?, ?> e) {
         *         return res.as(e).into();
         *     }
         *     final Bar bar = res.expectOk();
         * }
         * }</pre>
         * @return the value wrapped in this Err result
         */
        public <Z> Result<ERR, Z> into() { return err(v); }

        @Override public ERR expectErr() { return v; }

        @Override public Optional<ERR> optErr() { return Optional.ofNullable(v); }

        @Override
        public <P, Q> Result<P, Q> map(Function<ERR, P> fnErr, Function<OK, Q> fnOk) {
            return err(fnErr.apply(v));
        }

        @Override public <Z> Result<ERR, Z> mapOk(final Function<OK, Z> fn) { return err(v); }

        @Override public <Z> Result<Z, OK> mapErr(final Function<ERR, Z> fn) { return err(fn.apply(v)); }

        @Override public boolean filterErr(final Predicate<ERR> fn) { return fn.test(v); }

        @Override
        public Result<ERR, OK> ifErr(final Consumer<ERR> fn) {
            fn.accept(v);
            return this;
        }

        @Override
        public Result<ERR, OK> recoverIf(final Predicate<ERR> testFn, final Function<ERR, OK> mapFn) {
            if (testFn.test(v)) {
                return ok(mapFn.apply(v));
            } else {
                return this;
            }
        }

        @Override public <Z> Result<ERR, Z> flatMap(final Function<OK, Result<ERR, Z>> fn) { return err(v); }

        @Override public <Z> Z reduce(Function<ERR, Z> fnErr, Function<OK, Z> fnOk) { return fnErr.apply(v); }

        @Override public Result<OK, ERR> swap() { return ok(v); }
    }

    final class Ok<ERR, OK> implements Result<ERR, OK> {
        private final OK v;

        private Ok(final OK _ok) { v = _ok; }

        @Override
        public Ok<ERR, OK> as(final Ok<?, ?> thiz) {
            if (thiz != this) throw new IllegalArgumentException("Ok.as: argument must be the pattern variable bound to this Ok");
            return this;
        }

        /**
         * Get the value contained within this Ok.  This function is not defined on Result.  You should obtain an
         * Ok by doing an instanceof check
         * <pre>{@code
         * if (res instanceof Ok<?, ?> o) {
         *     final Foo v = res.as(o).get();
         * }
         * }</pre>
         * @return the value wrapped in this Ok result
         */
        public OK get() {
            return v;
        }

        /**
         * Coerce the ERR type to a new type and return this otherwise unmodified.
         * This function is not defined on Result.  You should obtain an Ok by doing an instanceof check
         * <pre>{@code
         * public Result<AnyErr, Foo> demoFunction() {
         *     final Result<Exception, Foo> res = someOperation();
         *     if (res instanceof Ok<?, ?> o) {
         *         return res.as(o).into();
         *     }
         *     return AnyError.from(res.expectErr());
         * }
         * }</pre>
         * Note that there are better ways of achieving what's shown in the demo function above, for example
         * <pre>{@code
         * public Result<AnyErr, Foo> demoFunction() {
         *     return someOperation().mapErr(AnyError::from);
         * }
         * }</pre>
         * This function is less useful on the Ok type, but it's included for symmetry.
         * @return the value wrapped in this Ok result
         */
        public <Z> Result<Z, OK> into() {
            return ok(v);
        }

        @Override public OK expectOk() { return v; }

        @Override public Optional<OK> optOk() { return Optional.ofNullable(v); }

        @Override
        public <P, Q> Result<P, Q> map(final Function<ERR, P> fnErr, final Function<OK, Q> fnOk) {
            return ok(fnOk.apply(v));
        }

        @Override public <Z> Result<ERR, Z> mapOk(final Function<OK, Z> fn) { return ok(fn.apply(v)); }

        @Override public <Z> Result<Z, OK> mapErr(final Function<ERR, Z> fn) { return ok(v); }

        @Override public boolean filterOk(final Predicate<OK> fn) { return fn.test(v); }

        @Override
        public Result<ERR, OK> ifOk(final Consumer<OK> fn) {
            fn.accept(v);
            return this;
        }

        @Override
        public Result<ERR, OK> failIf(final Predicate<OK> testFn, final Function<OK, ERR> mapFn) {
            if (testFn.test(v)) {
                return err(mapFn.apply(v));
            } else {
                return this;
            }
        }

        @Override public <Z> Result<ERR, Z> flatMap(final Function<OK, Result<ERR, Z>> fn) { return fn.apply(v); }

        @Override public <Z> Z reduce(final Function<ERR, Z> fnErr, final Function<OK, Z> fnOk) {
            return fnOk.apply(v);
        }

        @Override public Result<OK, ERR> swap() { return err(v); }
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
     *     .peek(Errs.ifErr(System.out::println));
     * }</pre>
     * Without these methods, the preceding would look like
     * <pre>{@code
     * someStream
     *     .map(res -> res.mapErr(e -> fn(e)))
     *     .filter(res -> res.filterErr(e -> e == null))
     *     .map(res -> res.recoverIf(
     *         e -> e instanceof NumberFormatException,
     *         e -> -1)
     *     .peek(res -> res.ifErr(System.out::println));
     * }</pre>
     * Namely, the text {@code res -> res} is repeated to the point of becoming noise.
     */
    final class Errs {
        public <ERR, OK, Z> Function<Result<ERR, OK>, Result<Z, OK>> map(final Function<ERR, Z> fn) {
            return res -> res.mapErr(fn);
        }

        public <ERR, OK> Predicate<Result<ERR, OK>> filter(final Predicate<ERR> fn) {
            return res -> res.filterErr(fn);
        }

        public <ERR, OK> Function<Result<ERR, OK>, Result<ERR, OK>> recoverIf(
                final Predicate<ERR> testFn, final Function<ERR, OK> mapFn) {
            return res -> res.recoverIf(testFn, mapFn);
        }

        public <ERR, OK> Function<Result<ERR, OK>, Result<OK, ERR>> swap() {
            return Result::swap;
        }

        public <ERR, OK> Consumer<Result<ERR, OK>> ifErr(final Consumer<ERR> fn) {
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
        public <ERR, OK, Z> Function<Result<ERR, OK>, Result<ERR, Z>> map(final Function<OK, Z> fn) {
            return res -> res.mapOk(fn);
        }

        public <ERR, OK> Predicate<Result<ERR, OK>> filter(final Predicate<OK> fn) {
            return res -> res.filterOk(fn);
        }

        public <ERR, OK> Function<Result<ERR, OK>, Result<ERR, OK>> failIf(
                final Predicate<OK> testFn, final Function<OK, ERR> mapFn) {
            return res -> res.failIf(testFn, mapFn);
        }

        public <ERR, OK, Z> Function<Result<ERR, OK>, Result<ERR, Z>> flatMap(final Function<OK, Result<ERR, Z>> fn) {
            return res -> res.flatMap(fn);
        }

        public <ERR, OK> Function<Result<ERR, OK>, Result<OK, ERR>> swap() {
            return Result::swap;
        }

        public <ERR, OK> Consumer<Result<ERR, OK>> ifOk(final Consumer<OK> fn) {
            return res -> res.ifOk(fn);
        }

        private Oks() {
            throw new IllegalStateException("This is a static class and should never be instantiated");
        }
    }

    static void main(String[] args) {
        final Result<String, Long> res = testIt();
    }

    static void testIt2() {
        final String theUrl = "localhost";
        Result.<Exception, String>ok(theUrl)
                .flatMap(u -> Try.get(() -> new URL(u)))
                .flatMap(Try.Map(URL::openConnection))
                .flatMap(Try.Map(URLConnection::getInputStream))
                .mapOk(is -> new BufferedReader(new InputStreamReader(is)))
                .mapOk(BufferedReader::lines)
                .ifErr(ex -> System.err.println("An exception occurred trying to fetch the url."))
                .ifErr(Exception::printStackTrace)
                .optOk()
                .ifPresent(lines -> System.out.println("URL contents from demoResult: "
                        + lines.collect(Collectors.joining("\n"))));
    }

    static Result<String, Long> testIt() {

        final Integer i;
        {
            final var res1 = Result.<String, Integer>ok(67);
            if (res1 instanceof Err<?, ?> e) {
                return res1.as(e).into();
            }
            i = res1.expectOk();
        }

        return ok(i.longValue());
    }
}
