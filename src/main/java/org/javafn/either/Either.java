package org.javafn.either;

import org.javafn.result.Result;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This type represents a value of one or another type, but never both.  `null` is a valid value,
 * however the null value will be of one type and the opposing type will be empty.
 * See {@link Left} and {@link Right} for a number of static methods useful in streams.
 * This class assigns no priority of one type over the other, whereas in other languages,
 * a Left type typically refers to an error and the Right a success.  This class is for
 * values which whose type may either of two types, with both being equally valid.
 * For representing success and errors, see the {@link Result} type which is effectively
 * the same implementation but with the added semantics.
 */
public abstract class Either<LEFT, RIGHT> {

    /**
     * Project an Either as a Left or a Right.  Similar to an optional,
     * operations on a projection either apply to the wrapped component,
     * or if the component is empty (i.e., it is the opposing type),
     * no operation is performed.
     * @param <T> the type of this projection
     * @param <O> the type of the opposite projection
     * @param <L> the type of the left element, which will be either T or O
     * @param <R> the type of the right element, which will either be T or O
     */
    public interface Projection<T, O, L, R> {
        /**
         * Get the element or throw a NoSuchElementException
         */
        T get();
        /**
         * Get the element, supplying a function to generate an exception
         * if this Either was projected to the wrong type.
         */
        T orElseThrow(Function<O, RuntimeException> exceptionMapper);

        /**
         * Get this element, or if this Either is projected to the wrong type, map the other element to this element's
         * type and return that instead.
         */
        T orElseMap(Function<O, T> fn);

        /**
         * Get this element wrapped in an optional or an empty optional if this Either was projected to the wrong type.
         */
        Optional<T> opt();

        /**
         * Apply the supplied predicate if this projection is not empty, otherwise return true.
         * The semantics for this can be thought of as a boolean OR: (this.isEmpty OR either(this.value)).
         */
        boolean filter(Predicate<T> fn);
        /**
         * Apply the function to the wrapped element or no op if this Either was projected to the wrong type.
         */
        Either<L, R> peek(Consumer<T> fn);

        /**
         * Apply the predicate to this element and map it to the other type if it returns true, or if this is
         * already the other type, return it unmodified.
         */
        Either<L, R> filterMap(Predicate<T> p, Function<T, O> fn);

        /**
         * Get the wrapped element as a single element stream, or if it's projected to the wrong type,
         * return an empty stream.
         */
        Stream<T> stream();
    }

    /**
     * A {@link Projection} of an Either as a Left value, which may or may not be present.
     */
    public interface Left<L, R> extends Projection<L, R, L, R> {
        /**
         * Map this left projection and return a new Either with a new type for the Left component,
         * or if this is a Right either, return a new Either with the same right element but a new type
         * for the left element.
         */
        <Z> Either<Z, R> map(Function<L, Z> fn);

        /**
         * Map this left projection by calling a function that itself returns an Either.
         * If the left is present, the function is called and the resulting either is returned.
         * If this is a right either, return a new either with the same right element but a new
         * type for the left element.
         */
        <Z> Either<Z, R> flatMap(Function<L, Either<Z, R>> fn);

        /**
         * Accept a list of L values and return a List of Either.left objects each wrapping a value from the input list.
         * @param l a list of bare left values
         * @return a list of Eithers, each wrapping a single left value
         * @param <LL> the left type
         * @param <RR> the right type
         */
        static <LL, RR> List<Either<LL, RR>> Wrap(List<LL> l) {
            return l.stream().map(Either::<LL, RR>ofLeft).collect(Collectors.toList());
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and calls {@link Left#map(Function)} with
         * the supplied function.
         */
        static <Z, LL, RR> Function<Either<LL, RR>, Either<Z, RR>> Map(Function<LL, Z> fn) {
            return either -> either.asLeft().map(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and calls {@link Left#flatMap(Function)}
         * with the supplied function.
         */
        static <LL, RR, ZZ> Function<Either<LL, RR>, Either<ZZ, RR>> FlatMap(Function<LL, Either<ZZ, RR>> fn) {
            return either -> either.asLeft().flatMap(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and returns the result of calling
         * {@link Projection#get()} on it.
         */
        static <LL, RR> Function<Either<LL, RR>, LL> Get() {
            return either -> either.asLeft().get();
        }
        /**
         * Return a function that accepts an either, projects it to the Left,
         * and calls {@link Projection#orElseMap(Function)} with the supplied function.
         */
        static <LL, RR> Function<Either<LL, RR>, LL> OrElseMap(Function<RR, LL> fn) {
            return either -> either.asLeft().orElseMap(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left,
         * and calls {@link Projection#orElseThrow(Function)} with the supplied exception supplier.
         */
        static <LL, RR> Function<Either<LL, RR>, LL> OrElseThrow(Function<RR, RuntimeException> exceptionMapper) {
            return either -> either.asLeft().orElseThrow(exceptionMapper);
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and calls {@link Projection#opt()}.
         */
        static <LL, RR> Function<Either<LL, RR>, Optional<LL>> Opt() {
            return either -> either.asLeft().opt();
        }
        /**
         * Return a function that accepts an either, projects it to the Left,
         * and calls {@link Projection#filter(Predicate)} with the supplied filter.
         */
        static <LL, RR> Predicate<Either<LL, RR>> Filter(Predicate<LL> fn) {
            return either -> either.asLeft().filter(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left,
         * and calls {@link Projection#peek(Consumer)} with the supplied consumer.
         */
        static <LL, RR> Consumer<Either<LL, RR>> Peek(Consumer<LL> fn) { return either -> either.asLeft().peek(fn); }
        /**
         * Return a function that accepts an either, projects it to the Left, and calls
         * {@link Projection#filterMap(Predicate, Function)} with the supplied predicate and function.
         */
        static <LL, RR> Function<Either<LL, RR>, Either<LL, RR>> FilterMap(Predicate<LL> p, Function<LL, RR> fn) {
            return either -> either.asLeft().filterMap(p, fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and calls {@link Projection#stream()}.
         */
        static <LL, RR> Function<Either<LL, RR>, Stream<LL>> Stream() { return either -> either.asLeft().stream(); }
    }

    /**
     * A {@link Projection} of an Either as a Right value, which may or may not be present.
     */
    public interface Right<L, R> extends Projection<R, L, L, R> {
        /**
         * Map this right projection and return a new Either with a new type for the right component,
         * or if this is a Left either, return a new Either with the same left element but a new type
         * for the right element.
         */
        <Z> Either<L, Z> map(Function<R, Z> fn);
        /**
         * Map this right projection by calling a function that itself returns an Either.
         * If the right is present, the function is called and the resulting either is returned.
         * If this is a left either, return a new either with the same left element but a new
         * type for the right element.
         */
        <Z> Either<L, Z> flatMap(Function<R, Either<L, Z>> fn);

        /**
         * Accept a list of R values and return a List of Either.right objects each wrapping a value from the input list.
         * @param r a list of bare right values
         * @return a list of Eithers, each wrapping a single right value
         * @param <LL> the left type
         * @param <RR> the right type
         */
        static <LL, RR> List<Either<LL, RR>> Wrap(List<RR> r) {
            return r.stream().map(Either::<LL, RR>ofRight).collect(Collectors.toList());
        }
        /**
         * Return a function that accepts an either, projects it to the Right, and calls {@link Right#map(Function)}
         * with the supplied function.
         */
        static <Z, LL, RR> Function<Either<LL, RR>, Either<LL, Z>> Map(Function<RR, Z> fn) {
            return either -> either.asRight().map(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Right, and calls {@link Right#flatMap(Function)}
         * with the supplied function.
         */
        static <LL, RR, ZZ> Function<Either<LL, RR>, Either<LL, ZZ>> FlatMap(Function<RR, Either<LL, ZZ>> fn) {
            return either -> either.asRight().flatMap(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Left, and returns the result of calling
         * {@link Projection#get()} on it.
         */
        static <LL, RR> Function<Either<LL, RR>, RR> Get() { return either -> either.asRight().get(); }
        /**
         * Return a function that accepts an either, projects it to the Right,
         * and calls {@link Projection#orElseThrow(Function)} with the supplied exception supplier.
         */
        static <LL, RR> Function<Either<LL, RR>, RR> OrElseThrow(Function<LL, RuntimeException> exceptionSupplier) {
            return either -> either.asRight().orElseThrow(exceptionSupplier);
        }
        /**
         * Return a function that accepts an either, projects it to the Right, and calls {@link Projection#opt()}.
         */
        static <LL, RR> Function<Either<LL, RR>, Optional<RR>> Opt() { return either -> either.asRight().opt(); }
        /**
         * Return a function that accepts an either, projects it to the Right,
         * and calls {@link Projection#filter(Predicate)} with the supplied predicate.
         */
        static <LL, RR> Predicate<Either<LL, RR>> Filter(Predicate<RR> fn) {
            return either -> either.asRight().filter(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Right,
         * and calls {@link Projection#peek(Consumer)} with the supplied consumer.
         */
        static <LL, RR> Consumer<Either<LL, RR>> Peek(Consumer<RR> fn) { return either -> either.asRight().peek(fn); }
        /**
         * Return a function that accepts an either, projects it to the Right,
         * and calls {@link Projection#orElseMap(Function)} with the supplied function.
         */
        static <LL, RR> Function<Either<LL, RR>, RR> OrElseMap(Function<LL, RR> fn) {
            return either -> either.asRight().orElseMap(fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Right, and calls
         * {@link Projection#filterMap(Predicate, Function)} with the supplied predicate and mapping function.
         */
        static <LL, RR> Function<Either<LL, RR>, Either<LL, RR>> FilterMap(Predicate<RR> p, Function<RR, LL> fn) {
            return either -> either.asRight().filterMap(p, fn);
        }
        /**
         * Return a function that accepts an either, projects it to the Right, and calls {@link Projection#stream()}.
         */
        static <LL, RR> Function<Either<LL, RR>, Stream<RR>> Stream() { return either -> either.asRight().stream(); }
    }

    /**
     * An implementation of Either and Projection representing a Left value.
     */
    private static final class LeftProjection<L, R> extends Either<L, R> implements Left<L, R> {
        final L leftValue;
        private LeftProjection(final Sealed _token, final L _leftValue) { super(_token); leftValue = _leftValue; }

        @Override public boolean isLeft() { return true; }
        @Override public boolean isRight() { return false; }
        @Override public L get() { return leftValue; }
        @Override public L orElseThrow(Function<R, RuntimeException> exceptionSupplier) { return leftValue; }
        @Override public L orElseMap(Function<R, L> fn) { return leftValue; }
        @Override public Optional<L> opt() { return Optional.of(leftValue); }
        @Override public boolean filter(Predicate<L> fn) { return fn.test(leftValue); }
        @Override public Either<L, R> peek(Consumer<L> fn) { fn.accept(leftValue); return this; }
        @Override public <Z> Either<Z, R>  map(Function<L, Z> fn) { return Either.ofLeft(fn.apply(leftValue)); }
        @Override public <Z> Either<Z, R> flatMap(Function<L, Either<Z, R>> fn) { return fn.apply(leftValue); }
        @Override public Either<L, R> filterMap(Predicate<L> p, Function<L, R> fn) {
            return p.test(leftValue) ? Either.ofRight(fn.apply(leftValue)) : this;
        }
        @Override public Stream<L> stream() { return Stream.of(leftValue); }

        @Override public Right<L, R> asRight() { return new EmptyRightProjection<>(this); }

        @Override public Left<L, R> asLeft() { return this; }
        @Override public Either<R, L> swap() { return Either.ofRight(leftValue); }
        @Override public <ML, MR> Either<ML, MR> mapEither(Function<L, ML> fnLeft, Function<R, MR> fnRight) {
            return Either.ofLeft(fnLeft.apply(leftValue));
        }
        @Override public Either<L, R> forEither(Consumer<L> fnLeft, Consumer<R> fnRight) {
            fnLeft.accept(leftValue);
            return this;
        }
        @Override public <T> T reduce(Function<L, T> fnLeft, Function<R, T> fnRight) { return fnLeft.apply(leftValue); }
        @Override public String toString() { return "Left[" + leftValue.toString() + "]"; }

        @Override public int hashCode() { return leftValue.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof LeftProjection) {
                return Objects.equals(leftValue, ((LeftProjection<?, ?>) other).leftValue);
            } else if (other instanceof EmptyRightProjection) {
                return Objects.equals(leftValue, ((EmptyRightProjection<?, ?>) other).leftEither.leftValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Either and Projection representing a Right value.
     */
    private static final class RightProjection<L, R> extends Either<L, R> implements Right<L, R> {
        private final R rightValue;
        private RightProjection(final Sealed _token, final R _rightValue) { super(_token); rightValue = _rightValue; }

        @Override public boolean isLeft() { return false; }
        @Override public boolean isRight() { return true; }
        @Override public R get() { return rightValue; }
        @Override public R orElseThrow(Function<L, RuntimeException> exceptionSupplier) { return rightValue; }
        @Override public R orElseMap(Function<L, R> fn) { return rightValue; }
        @Override public Optional<R> opt() { return Optional.of(rightValue); }
        @Override public boolean filter(Predicate<R> fn) { return fn.test(rightValue); }
        @Override public Either<L, R> peek(Consumer<R> fn) { fn.accept(rightValue); return this;}
        @Override public <Z> Either<L, Z> map(Function<R, Z> fn) { return Either.ofRight(fn.apply(rightValue)); }
        @Override public <Z> Either<L, Z> flatMap(Function<R, Either<L, Z>> fn) { return fn.apply(rightValue); }
        @Override public Either<L, R> filterMap(Predicate<R> p, Function<R, L> fn) {
            return p.test(rightValue) ? Either.ofLeft(fn.apply(rightValue)) : this;
        }
        @Override public Stream<R> stream() { return Stream.of(rightValue); }

        @Override public Right<L, R> asRight() { return this; }
        @Override public Left<L, R> asLeft() { return new EmptyLeftProjection<>(this); }

        @Override public Either<R, L> swap() { return Either.ofLeft(rightValue); }
        @Override public <ML, MR> Either<ML, MR> mapEither(Function<L, ML> fnLeft, Function<R, MR> fnRight)
        { return Either.ofRight(fnRight.apply(rightValue)); }
        @Override public Either<L, R> forEither(Consumer<L> fnLeft, Consumer<R> fnRight)
        { fnRight.accept(rightValue); return this; }
        @Override public <T> T reduce(Function<L, T> fnLeft, Function<R, T> fnRight)
        { return fnRight.apply(rightValue); }
        @Override public String toString() { return "Right[" + rightValue.toString() + "]"; }

        @Override public int hashCode() { return rightValue.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof RightProjection) {
                return Objects.equals(rightValue, ((RightProjection<?, ?>) other).rightValue);
            } else if (other instanceof EmptyLeftProjection) {
                return Objects.equals(rightValue, ((EmptyLeftProjection<?, ?>) other).rightEither.rightValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Projection representing the empty Left value of a Right Either
     */
    private static final class EmptyLeftProjection<L, R> implements Left<L, R> {
        private final RightProjection<L, R> rightEither;
        private EmptyLeftProjection(final RightProjection<L, R> _rightEither) { rightEither = _rightEither; }

        @Override public L get()
        { throw new NoSuchElementException("Calling get() on a left projection of a right either"); }
        @Override public L orElseThrow(Function<R, RuntimeException> exceptionSupplier)
        { throw exceptionSupplier.apply(rightEither.rightValue); }
        @Override public L orElseMap(Function<R, L> fn) { return fn.apply(rightEither.rightValue); }
        @Override public Optional<L> opt() { return Optional.empty(); }
        @Override public boolean filter(Predicate<L> fn) { return true; }
        @Override public Either<L, R> peek(Consumer<L> fn) { return rightEither; }
        @Override public <Z> Either<Z, R> map(Function<L, Z> fn) { return Either.ofRight(rightEither.rightValue); }
        @Override public <Z> Either<Z, R> flatMap(Function<L, Either<Z, R>> fn)
        { return Either.ofRight(rightEither.rightValue); }
        @Override public Either<L, R> filterMap(Predicate<L> p, Function<L, R> fn) { return rightEither; }
        @Override public Stream<L> stream() { return Stream.empty(); }
        @Override public String toString() { return rightEither.toString(); }

        @Override public int hashCode() { return rightEither.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof EmptyLeftProjection) {
                return Objects.equals(rightEither, ((EmptyLeftProjection<?, ?>) other).rightEither);
            } else if (other instanceof RightProjection) {
                return Objects.equals(rightEither.rightValue, ((RightProjection<?, ?>) other).rightValue);
            } else {
                return false;
            }
        }
    }

    /**
     * An implementation of Projection representing the empty Right value of a Left Either
     */
    private static final class EmptyRightProjection<L, R> implements Right<L, R> {
        private final LeftProjection<L, R> leftEither;
        private EmptyRightProjection(final LeftProjection<L, R> _leftEither) { leftEither = _leftEither; }

        @Override public R get()
        { throw new NoSuchElementException("Calling get() on a right projection of a left either"); }
        @Override public R orElseThrow(Function<L, RuntimeException> exceptionSupplier)
        { throw exceptionSupplier.apply(leftEither.leftValue); }
        @Override public R orElseMap(Function<L, R> fn) { return fn.apply(leftEither.leftValue); }
        @Override public Optional<R> opt() { return Optional.empty(); }
        @Override public boolean filter(Predicate<R> fn) { return true; }
        @Override public Either<L, R> peek(Consumer<R> fn) { return leftEither; }
        @Override public <Z> Either<L, Z> map(Function<R, Z> fn) { return Either.ofLeft(leftEither.leftValue); }
        @Override public <Z> Either<L, Z> flatMap(Function<R, Either<L, Z>> fn)
        { return Either.ofLeft(leftEither.leftValue); }
        @Override public Either<L, R> filterMap(Predicate<R> p, Function<R, L> fn) { return leftEither; }
        @Override public Stream<R> stream() { return Stream.empty(); }
        @Override public String toString() { return leftEither.toString(); }

        @Override public int hashCode() { return leftEither.hashCode(); }
        @Override public boolean equals(final Object other) {
            if (other instanceof EmptyRightProjection) {
                return Objects.equals(leftEither, ((EmptyRightProjection<?, ?>) other).leftEither);
            } else if (other instanceof LeftProjection) {
                return Objects.equals(leftEither.leftValue, ((LeftProjection<?, ?>) other).leftValue);
            } else {
                return false;
            }
        }
    }

    /**
     * Construct a new Left Either with the supplied value.
     */
    public static <L, R> Either<L, R> ofLeft(final L l)
    { return new LeftProjection<>(Either.ADDITIONAL_SUBCLASSES_NOT_ALLOWED, l); }
    /**
     * Construct a new Right Either with the supplied value.
     */
    public static <L, R> Either<L, R> ofRight(final R r)
    { return new RightProjection<>(Either.ADDITIONAL_SUBCLASSES_NOT_ALLOWED, r); }
    /**
     * Construct an Either with the same left and right type, deciding which based upon the isLeft boolean.
     */
    public static <E> Either<E, E> of(final boolean isLeft, final E e)
    { return isLeft ? Either.ofLeft(e) : Either.ofRight(e); }
    /**
     * Construct an Either with the same left and right type, deciding which based upon the supplied predicate.
     */
    public static <E> Either<E, E> of(final BooleanSupplier isLeft, final E e)
    { return isLeft.getAsBoolean() ? Either.ofLeft(e) : Either.ofRight(e); }
    /**
     * Construct an Either with the same left and right type, deciding which based upon the supplied predicate.
     */
    public static <E> Either<E, E> of(final Predicate<E> isLeft, final E e)
    { return isLeft.test(e) ? Either.ofLeft(e) : Either.ofRight(e); }

    /**
     * Return true if this is a left either, false otherwise.
     */
    public abstract boolean isLeft();
    /**
     * Return true if this is a right either, false otherwise
     */
    public abstract boolean isRight();

    /**
     * Get this Either as a left projection
     */
    public abstract Left<LEFT, RIGHT> asLeft();
    /**
     * Get this Either as a right projection
     */
    public abstract Right<LEFT, RIGHT> asRight();

    /**
     * Transpose the types of this Either and convert from a Left to a Right or vice versa
     */
    public abstract Either<RIGHT, LEFT> swap();

    /**
     * Execute the appropriate mapping function for this Either type and return a new Either with the same projection
     */
    public abstract <ML, MR> Either<ML, MR> mapEither(Function<LEFT, ML> fnLeft, Function<RIGHT, MR> fnRight);
    /**
     * Execute the appropriate consumer for this Either type and return this Either for chaining
     */
    public abstract Either<LEFT, RIGHT> forEither(Consumer<LEFT> fnLeft, Consumer<RIGHT> fnRight);

    /**
     * Execute the appropriate function for this Either type and return the generated value
     */
    public abstract <T> T reduce(Function<LEFT, T> fnLeft, Function<RIGHT, T> fnRight);

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
    /**
     * This class cannot be extended beyond the two subclasses defined in this file
     */
    private Either(final Sealed _token) {
        if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
            throw new IllegalArgumentException("Only the subclasses defined in the Either class may exist");
    }
}
