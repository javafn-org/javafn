package org.javafn.result;

import org.javafn.either.Either;
import org.javafn.result.ResultCollection.ResultCollector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An algebraic type that represents the possibly unsuccessful execution of a function.
 * Similar to an Either in many languages, a Result wraps a value of type ERR or of type OK,
 * but never both.  'null' is a valid value, but the Result will either be an err or an ok,
 * and the other value will be empty (similar to Optional.empty()).
 * <pre>{@code
 * Result.tryGet(() -> new URL(theUrl))
 *       .asOk().flatMap(Result.TryMap(URL::openConnection))
 *       .asOk().flatMap(Result.TryMap(URLConnection::getInputStream))
 *       .asOk().map(is -> new BufferedReader(new InputStreamReader(is)))
 *       .asOk().map(BufferedReader::lines)
 *       .asErr().peek(ex -> System.err.println("An exception occurred trying to fetch the url."))
 *       .asErr().peek(Exception::printStackTrace)
 *       .asOk().opt()
 *       .ifPresent(lines -> System.out.println("URL contents from demoResult: "
 *              + lines.collect(Collectors.joining("\n"))));
 * }</pre>
 * This class is heavily inspired by Rust's Result type
 * <a href="https://doc.rust-lang.org/std/result/">https://doc.rust-lang.org/std/result/</a>
 * and Scala's Either type (prior to v2.13)
 * <a href='https://www.scala-lang.org/api/2.12.11/scala/util/Either.html'>
 *     https://www.scala-lang.org/api/2.12.11/scala/util/Either.html
 * </a>.
 * @see Either
 * @see Projection
 * @see Err
 * @see Ok
 * @param <ERR> the type of value stored if this is an err
 * @param <OK> the type of value stored if this is an ok
 */
public abstract class Result<ERR, OK> {

    /**
     * An implementation of java's Collector that can be used with a stream of Result objects to collect them into
     * a ResultCollection object.
     * <pre>{@code
     *     System.out.println(Stream.<Result<Exception, Void>>generate(() ->
     *         rand.nextBoolean()
     *                 ? Result.err(new RuntimeException("Simulate a failure");
     *                 : Result.ok(null);
     *     )
     *             .limit(100)
     *             .collect(Result.collector())
     *             .<String>reduce(
     *                     errs -> errs.stream().map(Exception::getMessage).collect(Collectors.joining("\n")),
     *                     oks -> "There were " + oks.size() + " ok elements and no errors."));
     * }</pre>
     */
    public static <EE, OO> ResultCollector<EE, OO> collector() { return new ResultCollector<>(); }

    /**
     * Project a Result as an Err or an Ok.  Similar to an optional,
     * operations on a projection result apply to the wrapped component,
     * or if the component is empty (i.e., it is the opposing type),
     * no operation is performed.
     * <pre>{@code
     * Result.<String, Integer>err("Some error message")
     *         .asOk().peek(i -> System.out.println("This will not be printed"))
     *         .asErr().peek(msg -> System.err.println("This will be printed: " + msg);
     * }</pre>
     * <pre>{@code
     * Result.<String, Integer>ok(42)
     *         .asOk().peek(i -> System.out.println("This will be printed, and the value for i is " + i))
     *         .asErr().peek(msg -> System.err.println("This will not be printed.");
     * }</pre>
     * Do not attempt to call get unless you know for sure you're projecting to the correct type.
     * <pre>{@code
     * final String msg = Result.<String, Integer>ok(42)
     *         .asErr().get(); // will throw exception
     * }</pre>
     * <pre>{@code
     * final int status = Result.<String, Integer>err("Some error message")
     *         .asOk().get(); // will throw exception
     * }</pre>
     * <pre>{@code
     * Result<String, Integer> res = Result.err("Some error message")
     *         .asOk().get(); // will throw exception
     * if (res.isErr()) {
     *     String msg = res.asErr().get(); // safe
     * }
     * if (res.isOk()) {
     *     int status = res.asOk().get(); // still safe (if block is not executed)
     * }
     * }</pre>
     * Prefer to use the OrElse* functions for a more fluent approach.
     * <pre>{@code
     * final int status = Result.<String, Integer>err("Some error message")
     *         .asOk().orElseMap(msg -> 404);
     * }</pre>
     * <pre>{@code
     * final int status = Result.<String, Integer>ok(42)
     *         .asOk().orElseThrow(msg -> new IllegalStateException("This is a programming error"));
     * }</pre>
     * Also, note that map, flatMap, and into functions also exists, but they have to be defined specifically for
     * each projection for the types to be correct.  These are defined in {@link Ok} and {@link Err}.
     * @see Ok
     * @see Err
     * @see Ok#map(Function) 
     * @see Ok#flatMap(Function)
     * @see Ok#into()
     * @see Err#map(Function)
     * @see Err#flatMap(Function)
     * @see Err#into()
     * @param <THIS> the type of this projection
     * @param <OTHER> the type of the opposite projection
     * @param <E> the type of the err element, which will be result THIS or OTHER
     * @param <O> the type of the ok element, which will result be THIS or OTHER
     */
    public interface Projection<THIS, OTHER, E, O> {
        /**
         * Get the element or throw a NoSuchElementException
         */
        THIS get();
        /**
         * Get the element, supplying a function to generate an exception
         * if this Either was projected to the wrong type.
         */
        THIS orElseThrow(Function<OTHER, RuntimeException> exceptionMapper);

        /**
         * Get this element, or if this Result is projected to the wrong type, map the other element to this element's
         * type and return that instead.
         */
        THIS orElseMap(Function<OTHER, THIS> fn);

        /**
         * Get this element, or if this Result is projected to the wrong type, return the supplied element instead,
         * ignoring the value from the correct projection.
         */
        THIS orElse(THIS instead);

        /**
         * Get this element wrapped in an optional or an empty optional if this Result was projected to the wrong type.
         */
        Optional<THIS> opt();

        /**
         * Apply the supplied predicate if this projection is not empty, otherwise return true.
         * The semantics for this can be thought of as a boolean OR: (this.isEmpty OR either(this.value)).
         */
        boolean filter(Predicate<THIS> fn);
        /**
         * Apply the function to the wrapped element or no op if this Result was projected to the wrong type.
         */
        Result<E, O> peek(Consumer<THIS> fn);

        /**
         * Apply the predicate to this element and map it to the other type if it returns true, or if this is
         * already the other type, return it unmodified.
         */
        Result<E, O> filterMap(Predicate<THIS> p, Function<THIS, OTHER> fn);
    }

    /**
     * A {@link Projection} of a Result as an Err value, which may or may not be present.
     * */
    public interface Err<E, O> extends Projection<E, O, E, O> {
        /**
         * Map this err projection and return a new Result with a new type for the Err component,
         * or if this is an Ok result, return a new Result with the same ok element but a new type
         * for the err element.
         */
        <Z> Result<Z, O> map(Function<E, Z> fn);

        /**
         * Coerce the type signature, changing the Ok type without providing a mapping function.
         * This operation can only succeed on a correct projection and will throw a
         * NoSuchElementException if attempted on an Ok Result.
         * <pre>{@code
         * public Result<ErrorType, DesiredType> doThing() {
         *      final Result<ErrorType, IntermediateType> res = doPartial();
         *      if (res.isErr()) {
         *          // Because it's definitely an error, it's safe to change the Ok type from
         *          // IntermediateType to DesiredType without mapping.
         *          return res.asErr().into();
         *      }
         *      final IntermediateType it = res.asOk().get();
         *      return doTheRest(it);
         * }
         * }</pre>
         * This is syntactic sugar for creating a new err wrapping the err value of the old:
         * <pre>{@code
         * public Result<ErrorType, DesiredType> doThing() {
         *      final Result<ErrorType, IntermediateType> res = doPartial();
         *      if (res.isErr()) {
         *          return Result.err(res.asErr().get());
         *      }
         *      final IntermediateType it = res.asOk().get();
         *      return doTheRest(it);
         * }
         * }</pre>
         * If the compiler is unable to infer arguments, they must be manually supplied,
         * however using this method, we only need to supply the new ok type rather than the
         * type of the Result.
         * <pre>{@code
         * public Result<ErrorType, DesiredType> doThing() {
         *      final Result<ErrorType, IntermediateType> res = doPartial();
         *      if (res.isErr()) {
         *          return Result.<ErrorType, DesiredType>err(res.asErr().get());
         *          return res.asErr().<DesiredType>into();
         *      }
         *      ...
         * }
         * }</pre>
         * I propose the following pattern for usage.  Ideally we'd have something like Rust's `?` operator,
         * but since we don't, this meets my cleanliness check.
         * It prevents the result from cluttering up scope, and it makes the
         * intermediate type more prominent and the short-lived result less so.
         * <pre>{@code
         * public Result<ErrorType, DesiredType> doThing() {
         *      final IntermediateType it; {
         *          final Result<ErrorType, IntermediateType> res = doPartial();
         *          if (res.isErr()) return res.asErr().into();
         *          it = res.asOk().get();
         *      }
         *      return doTheRest(it);
         * }
         * }</pre>
         */
        <Z> Result<E, Z> into();
        /**
         * Similar to {@link #into()}, except the return value is a list containing this result with a coerced Ok type.
         * @return a list containing this element with the Ok type updated.
         */
        <Z> List<Result<E, Z>> intoList();
        /**
         * Similar to {@link #into()}, except the return value is a stream containing this result with a coerced Ok type.
         * @return a stream containing this element with the Ok type updated.
         */
        <Z> Stream<Result<E, Z>> intoStream();
        /**
         * Map this err projection by calling a function that itself returns a Result.
         * If the err is present, the function is called and the resulting result is returned.
         * If this is an ok result, return a new result with the same ok element but a new
         * type for the err element.
         */
        <Z> Result<Z, O> flatMap(Function<E, Result<Z, O>> fn);

        /**
         * Accept a list of err values and return a List of Result.err objects each
         * wrapping a value from the input list.
         * @param e a list of bare err values
         * @return a list of Results, each wrapping a single err value
         * @param <EE> the error type
         * @param <OO> the ok type
         */
        static <EE, OO> List<Result<EE, OO>> Wrap(List<EE> e)
        { return e.stream().map(Result::<EE, OO>err).collect(Collectors.toList()); }

        /**
         * Return a function that accepts a result, projects it to the Err, and calls {@link Err#map(Function)}
         * with the supplied function.
         */
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<Z, OO>> Map(Function<EE, Z> fn) {
            return result -> result.asErr().map(fn);
        }

        /**
         * Return a function that accepts a result, projects it as an err, and calls {@link Err#into()}.
         */
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<EE, Z>> Into() {
            return result -> result.asErr().into();
        }
        /**
         * Return a function that accepts a result, projects it as an err, and calls {@link Err#into()},
         * providing an explicit type for when the compiler is unable to infer it.
         */
        @SuppressWarnings("unused")
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<EE, Z>> Into(final Class<Z> clazz)
        { return result -> result.asErr().into(); }
        /**
         * Return a function that accepts a result, projects it as an err, and calls {@link Err#flatMap(Function)}
         * with the supplied function.
         */
        static <EE, OO, ZZ, RIN extends Result<EE, OO>> Function<RIN, Result<ZZ, OO>> FlatMap(Function<EE, Result<ZZ, OO>> fn)
        { return result -> result.asErr().flatMap(fn); }

        /**
         * Return a function that accepts a result, projects it as an Err,
         * and returns the result of calling {@link Err#get()} on it.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, EE> Get() {
            return result -> result.asErr().get();
        }
        /**
         * Return a function that accepts a result, projects it to the Err,
         * and calls {@link Projection#orElseMap(Function)} with the supplied function.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, EE> OrElseMap(Function<OO, EE> fn)
        { return result -> result.asErr().orElseMap(fn); }
        /**
         * Return a function that accepts a result, projects it to the Err,
         * and calls {@link Projection#orElseThrow(Function)} with the supplied exception supplier.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, EE> OrElseThrow(Function<OO, RuntimeException> exceptionMapper)
        { return result -> result.asErr().orElseThrow(exceptionMapper); }
        /**
         * Return a function that accepts a result, projects it to the Err, and calls {@link Projection#opt()}.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, Optional<EE>> Opt() {
            return result -> result.asErr().opt();
        }
        /**
         * Return a function that accepts a result, projects it to the Err,
         * and calls {@link Projection#filter(Predicate)} with the supplied filter.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Predicate<RIN> Filter(Predicate<EE> fn)
        { return result -> result.asErr().filter(fn); }
        /**
         * Return a function that accepts a result, projects it to the Err, and calls {@link Projection#peek(Consumer)}
         * with the supplied consumer.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Consumer<RIN> Peek(Consumer<EE> fn) { return result -> result.asErr().peek(fn); }
        /**
         * Return a function that accepts a result, projects it to the Err,
         * and calls {@link Projection#filterMap(Predicate, Function)} with the supplied predicate and function.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<EE, OO>>
        FilterMap(Predicate<EE> p, Function<EE, OO> fn)
        { return result -> result.asErr().filterMap(p, fn); }
    }

    /**
     * A {@link Projection} of a Result as an Ok value, which may or may not be present.
     */
    public interface Ok<E, O> extends Projection<O, E, E, O> {
        /**
         * Map this ok projection and return a new Result with a new type for the ok component,
         * or if this is an Err result, return a new Result with the same err element but a new type
         * for the ok element.
         */
        <Z> Result<E, Z> map(Function<O, Z> fn);
        IntResult<E> mapToInt(ToIntFunction<O> fn);
        LongResult<E> mapToLong(ToLongFunction<O> fn);
        DoubleResult<E> mapToDouble(ToDoubleFunction<O> fn);
        VoidResult<E> mapToVoid(Consumer<O> fn);

        /**
         * Coerce the type signature, changing the Err type without providing a mapping function.
         * This operation can only succeed on a correct projection and will throw a
         * NoSuchElementException if attempted on an Err Result.
         * @see Err#into()
         */
        <Z> Result<Z, O> into();
        /**
         * Similar to {@link #into()}, except the return value is a list containing this result with a coerced Err type.
         * @return a list containing this element with the Err type updated.
         */
        <Z> List<Result<Z, O>> intoList();
        /**
         * Similar to {@link #into()}, except the return value is a stream containing this result with a coerced Err type.
         * @return a stream containing this element with the Err type updated.
         */
        <Z> Stream<Result<Z, O>> intoStream();
        /**
         * Map this ok projection by calling a function that itself returns a Result.
         * If the ok is present, the function is called and the resulting result is returned.
         * If this is an err result, return a new result with the same err element but a new
         * type for the ok element.
         */
        <Z> Result<E, Z> flatMap(Function<O, Result<E, Z>> fn);
        IntResult<E> flatMapToInt(Function<O, IntResult<E>> fn);
        LongResult<E> flatMapToLong(Function<O, LongResult<E>> fn);
        DoubleResult<E> flatMapToDouble(Function<O, DoubleResult<E>> fn);
        VoidResult<E> flatMapToVoid(Function<O, VoidResult<E>> fn);

        /**
         * Accept a list of ok values and return a List of Result.ok objects each wrapping a value from the input list.
         * @param e a list of bare ok values
         * @return a list of Results, each wrapping a single ok value
         * @param <EE> the error type
         * @param <OO> the ok type
         */
        static <EE, OO> List<Result<EE, OO>> Wrap(List<OO> e)
        { return e.stream().map(Result::<EE, OO>ok).collect(Collectors.toList()); }

        /**
         * Return a function that accepts a result, projects it to the Ok, and calls {@link Ok#map(Function)} with the
         * supplied function.
         */
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<EE, Z>> Map(Function<OO, Z> fn)
        { return result -> result.asOk().map(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, VoidResult<EE>> MapToVoid(Consumer<OO> fn)
        { return result -> result.asOk().mapToVoid(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, IntResult<EE>> MapToInt(ToIntFunction<OO> fn)
        { return result -> result.asOk().mapToInt(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, LongResult<EE>> MapToLong(ToLongFunction<OO> fn)
        { return result -> result.asOk().mapToLong(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, DoubleResult<EE>> MapToDouble(ToDoubleFunction<OO> fn)
        { return result -> result.asOk().mapToDouble(fn); }
        /**
         * Return a function that accepts a result, projects it as an ok, and calls {@link Ok#into()}.
         */
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<Z, OO>> Into() { return result -> result.asOk().into(); }
        /**
         * Return a function that accepts a result, projects it as an ok, and calls {@link Ok#into()},
         * providing an explicit type for when the compiler is unable to infer it.
         */
        @SuppressWarnings("unused")
        static <Z, EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<Z, OO>> Into(final Class<Z> clazz)
        { return result -> result.asOk().into(); }
        /**
         * Return a function that accepts a result, projects it as an ok, and calls {@link Ok#flatMap(Function)} with
         * the supplied function.
         */
        static <EE, OO, ZZ, RIN extends Result<EE, OO>> Function<RIN, Result<EE, ZZ>>
        FlatMap(Function<OO, Result<EE, ZZ>> fn)
        { return result -> result.asOk().flatMap(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, VoidResult<EE>>
        FlatMapToVoid(Function<OO, VoidResult<EE>> fn)
        { return result -> result.asOk().flatMapToVoid(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, IntResult<EE>>
        FlatMapToInt(Function<OO, IntResult<EE>> fn)
        { return result -> result.asOk().flatMapToInt(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, LongResult<EE>>
        FlatMapToLong(Function<OO, LongResult<EE>> fn)
        { return result -> result.asOk().flatMapToLong(fn); }
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, DoubleResult<EE>>
        FlatMapToDouble(Function<OO, DoubleResult<EE>> fn)
        { return result -> result.asOk().flatMapToDouble(fn); }
        /**
         * Return a function that accepts a result, projects it as an Ok, and returns the result of calling
         * {@link Ok#get()} on it.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, OO> Get() { return result -> result.asOk().get(); }
        /**
         * Return a function that accepts a result, projects it to the Ok, and calls
         * {@link Projection#orElseMap(Function)} with the supplied function.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, OO> OrElseMap(Function<EE, OO> fn)
        { return result -> result.asOk().orElseMap(fn); }
        /**
         * Return a function that accepts a result, projects it to the Ok,
         * and calls {@link Projection#orElseThrow(Function)} with the supplied exception supplier.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, OO> OrElseThrow(Function<EE, RuntimeException> exceptionMapper)
        { return result -> result.asOk().orElseThrow(exceptionMapper); }
        /**
         * Return a function that accepts a result, projects it to the Ok, and calls {@link Projection#opt()}.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, Optional<OO>> Opt() { return result -> result.asOk().opt(); }
        /**
         * Return a function that accepts a result, projects it to the Ok,
         * and calls {@link Projection#filter(Predicate)} with the supplied predicate.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Predicate<RIN> Filter(Predicate<OO> fn)
        { return result -> result.asOk().filter(fn); }
        /**
         * Return a function that accepts a result, projects it to the Ok, and calls {@link Projection#peek(Consumer)}
         * with the supplied consumer.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Consumer<RIN> Peek(Consumer<OO> fn) { return result -> result.asOk().peek(fn); }
        /**
         * Return a function that accepts a result, projects it to the Ok, and calls
         * {@link Projection#filterMap(Predicate, Function)} with the supplied predicate and mapping function.
         */
        static <EE, OO, RIN extends Result<EE, OO>> Function<RIN, Result<EE, OO>>
        FilterMap(Predicate<OO> p, Function<OO, EE> fn)
        { return result -> result.asOk().filterMap(p, fn); }
    }

    /**
     * An implementation of Result and Projection representing an Err value.
     */
    public static final class ErrProjection<E, O> extends Result<E, O> implements Err<E, O> {
        final E errValue;
        private ErrProjection(final Sealed _token, final E _errValue) { super(_token, true); errValue = _errValue; }

        @Override public E get() { return errValue; }
        @Override public E orElseThrow(final Function<O, RuntimeException> unused) { return errValue; }
        @Override public E orElse(final E unused) { return errValue; }
        @Override public Optional<E> opt() { return Optional.of(errValue); }
        @Override public boolean filter(final Predicate<E> fn) { return fn.test(errValue); }
        @Override public Result<E, O> peek(final Consumer<E> fn) { fn.accept(errValue); return this; }
        @Override public <Z> Result<Z, O> map(final Function<E, Z> fn) { return err(fn.apply(errValue)); }
        @Override public <Z> Result<E, Z> into() { return err(errValue); }
        @Override public <Z> List<Result<E, Z>> intoList() { return Collections.singletonList(err(errValue)); }
        @Override public <Z> Stream<Result<E, Z>> intoStream() { return Stream.of(err(errValue)); }
        @Override public <Z> Result<Z, O> flatMap(final Function<E, Result<Z, O>> fn) { return fn.apply(errValue); }
        @Override public E orElseMap(Function<O, E> unused) { return errValue; }
        @Override public Result<E, O> filterMap(final Predicate<E> p, final Function<E, O> fn)
        { return p.test(errValue) ? ok(fn.apply(errValue)) : this; }

        @Override public Ok<E, O> asOk() { return new EmptyOkProjection<>(this); }

        @Override public Err<E, O> asErr() { return this; }
        @Override public Result<O, E> swap() { return ok(errValue); }
        @Override public <MERR, MOK> Result<MERR, MOK> mapResult(
                final Function<E, MERR> fnErr, final Function<O, MOK> unused)
        { return err(fnErr.apply(errValue)); }
        @Override public Result<E, O> peek(final Consumer<E> fnErr, final Consumer<O> unused)
        { fnErr.accept(errValue); return this; }
        @Override public <T> T reduce(final Function<E, T> fnErr, final Function<O, T> unused)
        { return fnErr.apply(errValue); }
        @Override public String toString() { return "Err[" + errValue.toString() + "]"; }

        @Override public int hashCode() { return errValue.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof ErrProjection) {
                return Objects.equals(errValue, ((ErrProjection<?, ?>) other).errValue);
            } else if (other instanceof EmptyOkProjection) {
                return Objects.equals(errValue, ((EmptyOkProjection<?, ?>) other).errResult.errValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Result and Projection representing an Ok value.
     */
    public static final class OkProjection<E, O> extends Result<E, O> implements Ok<E, O> {
        private final O okValue;
        private OkProjection(final Sealed _token, final O _okValue) { super(_token, false); okValue = _okValue; }

        @Override public O get() { return okValue; }
        @Override public O orElseThrow(final Function<E, RuntimeException> unused) { return okValue; }
        @Override public O orElse(final O unused) { return okValue; }
        @Override public Optional<O> opt() { return Optional.of(okValue); }
        @Override public boolean filter(final Predicate<O> fn) { return fn.test(okValue); }
        @Override public Result<E, O> peek(final Consumer<O> fn) { fn.accept(okValue); return this; }
        @Override public <Z> Result<E, Z> map(final Function<O, Z> fn) { return ok(fn.apply(okValue)); }
        @Override public IntResult<E> mapToInt(final ToIntFunction<O> fn) { return IntResult.ok(fn.applyAsInt(okValue)); }
        @Override public LongResult<E> mapToLong(final ToLongFunction<O> fn) { return LongResult.ok(fn.applyAsLong(okValue)); }
        @Override public DoubleResult<E> mapToDouble(final ToDoubleFunction<O> fn) { return DoubleResult.ok(fn.applyAsDouble(okValue)); }
        @Override public VoidResult<E> mapToVoid(final Consumer<O> fn) { fn.accept(okValue); return VoidResult.ok(); }
        @Override public <Z> Result<Z, O> into() { return ok(okValue); }
        @Override public <Z> List<Result<Z, O>> intoList() { return Collections.singletonList(ok(okValue)); }
        @Override public <Z> Stream<Result<Z, O>> intoStream() { return Stream.of(ok(okValue)); }
        @Override public <Z> Result<E, Z> flatMap(final Function<O, Result<E, Z>> fn) { return fn.apply(okValue); }
        @Override public VoidResult<E> flatMapToVoid(final Function<O, VoidResult<E>> fn) { return fn.apply(okValue); }
        @Override public IntResult<E> flatMapToInt(final Function<O, IntResult<E>> fn) { return fn.apply(okValue); }
        @Override public LongResult<E> flatMapToLong(final Function<O, LongResult<E>> fn) { return fn.apply(okValue); }
        @Override public DoubleResult<E> flatMapToDouble(final Function<O, DoubleResult<E>> fn) { return fn.apply(okValue); }
        @Override public O orElseMap(Function<E, O> unused) { return okValue; }
        @Override public Result<E, O> filterMap(final Predicate<O> p, final Function<O, E> fn)
        { return p.test(okValue) ? err(fn.apply(okValue)) : this; }

        @Override public Ok<E, O> asOk() { return this; }
        @Override public Err<E, O> asErr() { return new EmptyErrProjection<>(this); }

        @Override public Result<O, E> swap() { return err(okValue); }
        @Override public <MERR, MOK> Result<MERR, MOK> mapResult(
                final Function<E, MERR> unused, final Function<O, MOK> fnOk)
        { return ok(fnOk.apply(okValue)); }
        @Override public Result<E, O> peek(final Consumer<E> unused, final Consumer<O> fnOk)
        { fnOk.accept(okValue); return this; }
        @Override public <T> T reduce(final Function<E, T> unused, final Function<O, T> fnOk)
        { return fnOk.apply(okValue); }
        @Override public String toString() { return "Ok[" + okValue.toString() + "]"; }

        @Override public int hashCode() { return okValue.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof OkProjection) {
                return Objects.equals(okValue, ((OkProjection<?, ?>) other).okValue);
            } else if (other instanceof EmptyErrProjection) {
                return Objects.equals(okValue, ((EmptyErrProjection<?, ?>) other).okResult.okValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Projection representing the empty Err value of an Ok Result
     */
    private static final class EmptyErrProjection<E, O> implements Err<E, O> {
        private final OkProjection<E, O> okResult;
        private EmptyErrProjection(final OkProjection<E, O> _okResult) { okResult = _okResult; }

        @Override public E get() { throw new NoSuchElementException("Trying to get an Err from an Ok Projection."); }
        @Override public E orElseThrow(final Function<O, RuntimeException> exceptionMapper)
        { throw exceptionMapper.apply(okResult.okValue); }
        @Override public E orElse(final E instead) { return instead; }
        @Override public Optional<E> opt() { return Optional.empty(); }
        @Override public boolean filter(final Predicate<E> unused) { return true; }
        @Override public Result<E, O> peek(final Consumer<E> unused) { return okResult; }
        @Override public <Z> Result<Z, O> map(final Function<E, Z> unused) { return ok(okResult.okValue); }
        @Override public <Z> Result<E, Z> into()
        { throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
        @Override public <Z> List<Result<E, Z>> intoList()
        { throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
        @Override public <Z> Stream<Result<E, Z>> intoStream()
        { throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
        @Override public <Z> Result<Z, O> flatMap(final Function<E, Result<Z, O>> unused)
        { return ok(okResult.okValue); }
        @Override public E orElseMap(final Function<O, E> fn) { return fn.apply(okResult.okValue); }
        @Override public Result<E, O> filterMap(final Predicate<E> unusedP, final Function<E, O> unusedFn)
        { return okResult; }
        @Override public String toString() { return okResult.toString(); }

        @Override public int hashCode() { return okResult.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof EmptyErrProjection) {
                return Objects.equals(okResult, ((EmptyErrProjection<?, ?>) other).okResult);
            } else if (other instanceof OkProjection) {
                return Objects.equals(okResult.okValue, ((OkProjection<?, ?>) other).okValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Projection representing the empty Ok value of an Err Result.
     */
    private static final class EmptyOkProjection<E, O> implements Ok<E, O> {
        private final ErrProjection<E, O> errResult;
        private EmptyOkProjection(final ErrProjection<E, O> _errResult) { errResult = _errResult; }

        @Override public O get() { throw new NoSuchElementException("Trying to get an Ok result from an Err Result."); }
        @Override public O orElseThrow(final Function<E, RuntimeException> exceptionMapper)
        { throw exceptionMapper.apply(errResult.errValue); }
        @Override public O orElse(final O instead) { return instead; }
        @Override public Optional<O> opt() { return Optional.empty(); }
        @Override public boolean filter(final Predicate<O> unused) { return true; }
        @Override public Result<E, O> peek(final Consumer<O> unused) { return errResult; }
        @Override public <Z> Result<E, Z> map(final Function<O, Z> unused) { return err(errResult.errValue); }
        @Override public IntResult<E> mapToInt(final ToIntFunction<O> fn) { return IntResult.err(errResult.errValue); }
        @Override public LongResult<E> mapToLong(final ToLongFunction<O> fn) { return LongResult.err(errResult.errValue); }
        @Override public DoubleResult<E> mapToDouble(final ToDoubleFunction<O> fn) { return DoubleResult.err(errResult.errValue); }
        @Override public VoidResult<E> mapToVoid(final Consumer<O> fn) { return VoidResult.err(errResult.errValue); }
        @Override public <Z> Result<Z, O> into()
        { throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
        @Override public <Z> List<Result<Z, O>> intoList()
        { throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
        @Override public <Z> Stream<Result<Z, O>> intoStream()
        { throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
        @Override public <Z> Result<E, Z> flatMap(final Function<O, Result<E, Z>> unused)
        { return err(errResult.errValue); }
        @Override public VoidResult<E> flatMapToVoid(final Function<O, VoidResult<E>> unused)
        { return VoidResult.err(errResult.errValue); }
        @Override public IntResult<E> flatMapToInt(final Function<O, IntResult<E>> fn)
        { return IntResult.err(errResult.errValue); }
        @Override public LongResult<E> flatMapToLong(final Function<O, LongResult<E>> fn)
        { return LongResult.err(errResult.errValue); }
        @Override public DoubleResult<E> flatMapToDouble(final Function<O, DoubleResult<E>> fn)
        { return DoubleResult.err(errResult.errValue); }
        @Override public O orElseMap(final Function<E, O> fn) { return fn.apply(errResult.errValue); }
        @Override public Result<E, O> filterMap(final Predicate<O> unusedP, final Function<O, E> unusedFn)
        { return errResult; }
        @Override public String toString() { return errResult.toString(); }

        @Override public int hashCode() { return errResult.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof EmptyOkProjection) {
                return Objects.equals(errResult, ((EmptyOkProjection<?, ?>) other).errResult);
            } else if (other instanceof ErrProjection) {
                return Objects.equals(errResult.errValue, ((ErrProjection<?, ?>) other).errValue);
            } else {
                return false;
            }
        }
    }

    /**
     * Construct a new Err Result with the supplied value.
     * <pre>{@code
     * final Result<String, UUID> res = Result.err("The supplied id is not a valid UUID");
     * }</pre>
     * @param err The error value from which a Result will be created
     * @return a new err variant Result
     */
    public static <EE, OO> ErrProjection<EE, OO> err(final EE err)
    { return new ErrProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, err); }

    /**
     * Construct a new Err Result whose type is a list of error values.
     * This is useful for APIs that allow for a number of error conditions, but only one is generated,
     * or a handful are generated in one call.  The supplied values are forwarded to {@link Arrays#asList(Object[])}.
     * <pre>{@code
     * final Result<List<String>, UUID> res = Result.errList(
     *         "The supplied uuid is not the right length",
     *         "The supplied uuid includes invalid characters");
     * }</pre>
     * If no arguments are supplied, the result will be an Err type wrapping an empty list, as if calling
     * <pre>{@code
     * final Result<List<String>, UUID> res = Result.err(Collections.emptyList());
     * }</pre>
     */
    @SafeVarargs
    public static <EE, OO> ErrProjection<List<EE>, OO> errList(final EE... errs)
    { return new ErrProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, Arrays.asList(errs)); }

    /**
     * Construct a new Ok Result with the supplied value.
     * <pre>{@code
     * final Result<String, UUID> res = Result.ok(UUID.randomUUID());
     * }</pre>
     * @param ok The ok value from which a Result will be created
     * @return a new ok variant Result
     */
    public static <EE, OO> OkProjection<EE, OO> ok(final OO ok)
    { return new OkProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, ok); }

    @SafeVarargs
    public static <EE, OO> OkProjection<EE, List<OO>> okList(final OO... oks)
    { return new OkProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, Arrays.asList(oks)); }

    /**
     * Construct a new Ok Result from the supplied optional if present, or an Err Result
     * from the errSupplier if the optional is empty.
     * <pre>{@code
     * final Optional<UUID> uuid = maybeGetId();
     * final Result<String, UUID> res = Result.okOrElse(uuid, () -> "Could not get id.");
     * }</pre>
     * @param maybeOk An optional that may or may not be empty
     * @param errSupplier A Supplier that generates an error value if maybeOk is empty
     * @return a new Result that will be an ok containing the optional's value or if the optional is empty,
     * a Result that will be an err containing the value returned from the errSupplier
      */
    public static <EE, OO> Result<EE, OO> okOrElse(final Optional<OO> maybeOk, final Supplier<EE> errSupplier)
    { return maybeOk.<Result<EE, OO>>map(Result::ok).orElseGet(() -> err(errSupplier.get())); }

    /**
     * Return true if this is an err result, false otherwise.
     */
    public boolean isErr() { return isErr; }
    /**
     * Return true if this is an ok result, false otherwise
     */
    public boolean isOk() { return isOk; }

    /**
     * Get this Result as a err projection
     * <pre>{@code
     * final Result<String, UUID> res = Result.okOrElse(maybeGetId(), () -> "Could not get id.");
     * final Result<Integer, UUID> res2 = res.asErr().map(msg -> toStatusCode(msg));
     * }</pre>
     */
    public abstract Err<ERR, OK> asErr();
    /**
     * Get this Result as a ok projection
     * <pre>{@code
     * final Result<String, UUID> res = Result.okOrElse(maybeGetId(), () -> "Could not get id.");
     * final Result<String, String> res2 = res.asOk().map(id -> id.toString());
     * }</pre>
     */
    public abstract Ok<ERR, OK> asOk();

    /**
     * Transpose the types of this Result and convert from an Err to an Ok or vice versa
     * <pre>{@code
     * Result<String, UUID> res1 = Result.okOrElse(maybeGetId(), () -> "Could not get id.");
     * Result<UUID, String> res2 = res1.swap();
     * }</pre>
     * Can be supplied directly in a stream
     * <pre>{@code
     * someStreamOfResults
     *          .map(Result::swap)
     *          .forEach(...);
     * }</pre>
     */
    public abstract Result<OK, ERR> swap();

    /**
     * Execute the appropriate mapping function for this Result type and return a new Result with the same projection
     * <pre>{@code
     * Result<String, UUID> res1 = Result.okOrElse(maybeGetId(), () -> "Could not get id.");
     * Result<Integer, long[]> res2 = res1.mapResult(
     *     msg -> toStatusCode(msg),
     *     uuid -> { id.getMostSignificantBits(), id.getLeastSignificantBits() }
     * );
     * }</pre>
     */
    public abstract <NEWERR, NEWOK> Result<NEWERR, NEWOK> mapResult(
            Function<ERR, NEWERR> fnErr, Function<OK, NEWOK> fnOk);

    /**
     * Return a function that will accept a result and execute the appropriate mapping function for the supplied Result
     * type and return a new Result with the same projection.
     * <pre>{@code
     * Result<String, UUID> res1 = Result.okOrElse(maybeGetId(), () -> "Could not get id.");
     * Result<Integer, long[]> res2 = res1.mapResult(
     * someStreamOfResults
     *     .map(Result.MapResult(
     *             msg -> toStatusCode(msg),
     *             uuid -> { id.getMostSignificantBits(), id.getLeastSignificantBits() }))
     *     .forEach(...);
     * }</pre>
     */
    public static <E, O, NEWERR, NEWOK> Function<Result<E, O>, Result<NEWERR, NEWOK>> MapResult(
            final Function<E, NEWERR> fnErr, final Function<O, NEWOK> fnOk)
    { return res -> res.mapResult(fnErr, fnOk); }

    /**
     * Execute the appropriate consumer for this Result type and return this Result for chaining
     * <pre>{@code
     * Result<Void, Void> res = doSomeOperation();
     * res.peek(
     *     () -> System.out.println("This will only print if the result is an err."),
     *     () -> System.out.println("This will only print if the result is an ok.")
     * );
     * }</pre>
     */
    public abstract Result<ERR, OK> peek(Consumer<ERR> fnErr, Consumer<OK> fnOk);

    /**
     * Return a function that will accept a result and execute the appropriate consumer for the supplied Result
     * and return this Result for chaining
     * <pre>{@code
     * someStreamOfResults
     *      .peek(Result.Peek(
     *              err -> System.out.println("This will only print if the result is an err."),
     *              ok -> System.out.println("This will only print if the result is an ok.")));
     * }</pre>
     */
    public static <E, O> Function<Result<E, O>, Result<E, O>> Peek(Consumer<E> fnErr, Consumer<O> fnOk)
    { return res -> res.peek(fnErr, fnOk); }

    /**
     * Execute the appropriate function for this Result type and return the generated value.
     * Consider this as one of several safe alternatives to projecting and calling get.
     * <pre>{@code
     * Result<Void, Void> res = doSomeOperation();
     * System.out.println(res.reduce(
     *     voidErr -> "This will only print if the result is an err.",
     *     voidOk -> "This will only print if the result is an ok."
     * );
     * }</pre>
     * @return the value of type T produced by the appropriate function depending on this Result's variant
     */
    public abstract <T> T reduce(Function<ERR, T> fnErr, Function<OK, T> fnOk);

    /**
     * Return a function that will accept a result and execute the appropriate function and return the generated value.
     * This function is intended to be used in a stream to avoid naming the result variable.
     * <pre>{@code
     * someStreamOfResults
     *      .map(Result.Reduce(
     *              err -> "This was an err",
     *              ok -> "This was an ok"))
     *      .foreach(System.out::println);
     * }</pre>
     */
    public static <E, O, T> Function<Result<E, O>, T> Reduce(final Function<E, T> fnErr, final Function<O, T> fnOk)
    { return res -> res.reduce(fnErr, fnOk); }

    /**
     * Wrap this result in a list.  No guarantees are made regarding the type, thread safety, or mutability
     * of the returned list.
     * @return a list with this Result as the only element
     */
    public List<Result<ERR, OK>> list() { return Collections.singletonList(this); }

    /**
     * Wrap this result in a stream.
     * @return a stream with this Result as the only element
     */
    public Stream<Result<ERR, OK>> stream() { return Stream.of(this); }

    public final boolean isErr;
    public final boolean isOk;

    /**
     * In Scala terminology, a 'sealed' class is one that has been subclassed, but no additional subclasses can
     * be created.  I'm achieving this effect in Java by declaring an instance of a private type,
     * which is required to instantiate any subclasses.
     * The only way a subclass may exist is to be defined as a nested member of this outer class.
     * There are easier ways of doing this, but this approach is expected to cause the least confusion
     * to anyone trying to understand what's going on.
     */
    private static final class Sealed {}
    private static final Sealed ADDITIONAL_SUBCLASSES_NOT_ALLOWED = new Sealed();
    /** This class cannot be extended beyond the two subclasses defined in this file */
    private Result(final Sealed _token, final boolean _isErr) {
        if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
            throw new IllegalArgumentException("Only the subclasses defined in the Result class may exist");
        isErr = _isErr;
        isOk = !_isErr;
    }
}
