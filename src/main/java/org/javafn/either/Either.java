package org.javafn.either;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An algebraic sum type that represents one of two possible values, neither of which is "better" than the other.
 *
 * <pre>{@code
 * final Either<Km, In> dist = ...;
 * final Meters dm = dist.reduce(
 *      km -> new Meters(km / 1000),
 *      inches -> new Meters(inches * 0.0254));
 * }</pre>
 * This class is very nearly identical to the {@link org.javafn.result.Result} class with a few minor exceptions.
 * In order to reduce documentation drift, this class is not heavily documented.  Refer to the Result documentation,
 * treating Left values as Errs and Right values as Oks.
 */
public sealed interface Either<L, R> permits Either.Left, Either.Right {

    /** Create a new Either wrapping a Right value. */
	static <L, R> Either<L, R> right(final R right) {
		return new Right<>(right);
	}

	/** Create a new Either wrapping a Left value. */
	static <L, R> Either<L, R> left(final L left) {
		return new Left<>(left);
	}

	/**
	 * After performing an instanceof check, turn this Either into a concrete Left type.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either instanceof Left<?, ?> l) {
	 *     final Left<LType, RType> left = either.as(l);
	 *     final LType value = left.value();
	 * }
	 * }</pre>
	 * You may be inclined to cast your either and pass it to this method, for example
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either.isLeft()) {
	 *     final Left<LType, RType> left = either.as((Left<?, ?>) either);
	 *     final LType value = left.value();
	 * }
	 * }</pre>
	 * This is syntactically legal, but there are easier ways to do this.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either.isLeft()) {
	 *     final LType value = either.expectLeft();
	 * }
	 * }</pre>
	 * You should never use the cast approach and treat its legality as an oddity.
	 * This is the only safe way to obtain a Left object from an either, which defines several additional
	 * methods that can not be safely implemented on an either.
	 * @param thiz the same object this method is being called on after performing a pattern match
	 * @return this with the type parameters restored
	 * @throws IllegalArgumentException if the parameter is anything except the method receiver
	 * @see Left#value()
	 * @see Left#into()
	 */
	default Left<L, R> as(Left<?, ?> thiz) {
		throw new IllegalArgumentException("as(Left) called on non-Left instance; use instanceof Left and its pattern variable");
	}

	/**
	 * After performing an instanceof check, turn this Either into a concrete Right type.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either instanceof Right<?, ?> o) {
	 *     final Right<LType, String> right = either.as(o);
	 *     final String value = right.value();
	 * }
	 * }</pre>
	 * You may be inclined to cast your either and pass it to this method, for example
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either.isRight()) {
	 *     final Right<LType, String> right = either.as((Right<?, ?>) either);
	 *     final String value = right.value();
	 * }
	 * }</pre>
	 * This is syntactically legal, but there are easier ways to do this.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either.isRight()) {
	 *     final String value = either.expectRight();
	 * }
	 * }</pre>
	 * You should never use the cast approach and treat its legality as an oddity.
	 * This is the only safe way to obtain a Right object from an either, which defines several additional
	 * methods that can not be safely implemented on an either.
	 * @param thiz the same object this method is being called on after performing a pattern match
	 * @return this with the type parameters restored
	 * @throws IllegalArgumentException if the parameter is anything except the method receiver
	 * @see Right#value()
	 * @see Right#into()
	 */
	default Right<L, R> as(Right<?, ?> thiz) {
		throw new IllegalArgumentException("as(Right) called on non-Right instance; use instanceof Right and its pattern variable");
	}

	/**
	 * Return true if this is a left variant, false otherwise.  This function is useful in a stream pipeline.
	 * More general usage would involve an instanceof check with pattern matching followed by {@link #as(Left)}.
	 * @return true iff this is a left variant
	 */
	default boolean isLeft() { return false; }

	/**
	 * Return true if this is a right variant, false otherwise.  This function is useful in a stream pipeline.
	 * More general usage would involve an instanceof check with pattern matching followed by {@link #as(Right)}.
	 * @return true iff this is a right variant
	 */
	default boolean isRight() { return false; }

	/**
	 * Unsafe operation, only safe in a branch opposite a test against right.
	 * Access the left value or throw an exception.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either instanceof Right<?,?> o) {
	 *     ... // do something with right
	 * } else {
	 *     LType e = either.expectLeft();
	 * }
	 * }</pre>
	 * @return the left value in this Either
	 * @throws IllegalStateException if this Either is not actually a Left
	 */
	default L expectLeft() {
		throw new IllegalStateException("Expected Either to be a Left instance, but it was not");
	}

	/**
	 * Unsafe operation, only safe in a branch opposite a test against left.
	 * Access the right value or throw an exception.
	 * <pre>{@code
	 * final Either<LType, RType> either = ...;
	 * if (either instanceof Left<?,?> e) {
	 *     return either.as(e).into();
	 * }
	 * final String value = either.expectRight();
	 * }</pre>
	 * @return the right value in this Either
	 * @throws IllegalStateException if this Either is not actually a Right
	 */
	default R expectRight() {
		throw new IllegalStateException("Expected Either to be a Right instance, but it was not");
	}

	/**
	 * Get the non-null left value of this either wrapped in an optional, or an empty optional if this either
	 * is not a left or the wrapped left value is null.
	 */
	default Optional<L> optLeft() {
		return Optional.empty();
	}

	/**
	 * Get the non-null right value of this result wrapped in an optional, or an empty optional if this result
	 * is not a right or the wrapped right value is null.
	 */
	default Optional<R> optRight() {
		return Optional.empty();
	}

	/**
	 * Perform the appropriate mapping operation on this Either.
	 * @param fnLeft the function to apply if this is a Left
	 * @param fnRight the function to apply if this is a Right
	 * @return the result with the appropriate function applied
	 * @param <NEWL> the type of the left after applying the function
	 * @param <NEWR> the type of the right after applying the function
	 */
	<NEWL, NEWR> Either<NEWL, NEWR> map(Function<L, NEWL> fnLeft, Function<R, NEWR> fnRight);

	/**
	 * Apply the mapping function to the left value if this is a Left, otherwise return this
	 * @param fn the function to apply to a left value
	 * @return a new result with the left mapped or this unmodified if this is a right
	 * @param <Z> the new left type
	 */
	<Z> Either<Z, R> mapLeft(Function<L, Z> fn);

	/**
	 * Apply the mapping function to the right value if this is a Right, otherwise return this
	 * @param fn the function to apply to a right value
	 * @return a new result with the right mapped or this unmodified if this is a left
	 * @param <Z> the new right type
	 */
	<Z> Either<L, Z> mapRight(Function<R, Z> fn);

	/**
	 * Apply the supplied predicate to the wrapped left value or return true if this is a right.
	 * Semantically, {@code return (this instanceof Right || fn.test(left))}
	 * If you want {@code &&} semantics, use {@link #isLeft()}
	 * @param fn the predicate to apply to the left value
	 * @return true if this is not a left or the result of applying the predicate to the left value
	 */
	default boolean filterLeft(Predicate<L> fn) {
		return true;
	}

	/**
	 * Apply the supplied predicate to the wrapped right value or return true if this is a left.
	 * Semantically, {@code return (this instanceof Left || fn.test(right))}
	 * If you want {@code &&} semantics, use {@link #isRight()}
	 * @param fn the predicate to apply to the right value
	 * @return true if this is not a right or the result of applying the predicate to the right value
	 */
	default boolean filterRight(Predicate<R> fn) {
		return true;
	}

	/**
	 * Apply the supplied consumer to the wrapped left value, or if this is a right, perform no operation.
	 * <strong>This function is used to achieve side effects.
	 * You are strongly encouraged to document the side effect if it involves mutating external state.</strong>
	 * In the most innocuous case, you're writing to a log.
	 * In other cases, you could be modifying a collection, for example,
	 * Collecting a list of lefts before filtering them from the pipeline.
	 * @return this, unmodified
	 */
	default Either<L, R> ifLeft(Consumer<L> fn) {
		return this;
	}

	/**
	 * Apply the supplied consumer to the wrapped right value, or if this is a left, perform no operation.
	 * <strong>This function is used to achieve side effects.
	 * You are strongly encouraged to document the side effect if it involves mutating external state.</strong>
	 * In the most innocuous case, you're writing to a log.
	 * In other cases, you could be modifying a collection, for example,
	 * when comparing entries from a database and entries from some other source of truth,
	 * collecting a delta as a side effect of some primary operation.
	 * You could remove processed entries from a list, and then anything left over after the pipeline runs
	 * are things that are not present in the other source.
	 * @param fn the consumer to apply to the wrapped right value, performing side effects
	 * @return this, unmodified
	 */
	default Either<L, R> ifRight(Consumer<R> fn) {
		return this;
	}

	/**
	 * If this result is a left, apply the supplied predicate to determine if recovery is possible,
	 * and if that returns true, apply the supplied mapping function to the left value
	 * and return a right result with the new value.
	 * @param testFn the predicate to apply to the left value
	 * @param mapFn the mapping function to apply to the left value
	 * @return a new result with the left value mapped to a right if this is a left and the supplied predicate matches,
	 * otherwise return this unmodified
	 */
	default Either<L, R> toRightIf(Predicate<L> testFn, Function<L, R> mapFn) { return this; }

	/**
	 * If this result is a right, apply the supplied predicate, and if that returns true, apply the
	 * supplied mapping function to the right value and return a left result with the result.
	 * @param testFn the predicate to apply to the right value
	 * @param mapFn the mapping function to apply to the right value
	 * @return a new result with the right value mapped to a left if this is a right and the supplied predicate matches,
	 * otherwise return this unmodified
	 */
	default Either<L, R> toLeftIf(Predicate<R> testFn, Function<R, L> mapFn) { return this; }

	/**
	 * Apply the appropriate mapping function and return the resulting value.
	 * @param fnLeft the function to apply if this is a Left
	 * @param fnRight the function to apply if this is a Right
	 * @return a value of some common type, produced by applying the appropriate mapping function
	 * @param <Z> the result type
	 */
	<Z> Z reduce(Function<L, Z> fnLeft, Function<R, Z> fnRight);

	/**
	 * Swap the type parameters and convert this into the opposing type.
	 */
	Either<R, L> swap();

	record Left<L, R>(L value) implements Either<L, R> {

		@Override public Left<L, R> as(final Left<?, ?> thiz) {
			if (thiz != this)
				throw new IllegalArgumentException("Left.as: argument must be the pattern variable bound to this Left");
			return this;
		}

		@Override public boolean isLeft() { return true; }

		public <Z> Either<L, Z> into() { return left(value); }

		@Override public L expectLeft() { return value; }

		@Override public Optional<L> optLeft() { return Optional.ofNullable(value); }

		@Override public <P, Q> Either<P, Q> map(Function<L, P> fnLeft, Function<R, Q> fnRight) {
			return left(fnLeft.apply(value));
		}

		@Override public <Z> Either<L, Z> mapRight(final Function<R, Z> fn) { return left(value); }

		@Override public <Z> Either<Z, R> mapLeft(final Function<L, Z> fn) { return left(fn.apply(value)); }

		@Override public boolean filterLeft(final Predicate<L> fn) { return fn.test(value); }

		@Override public Either<L, R> ifLeft(final Consumer<L> fn) {
			fn.accept(value);
			return this;
		}

		@Override public Either<L, R> toRightIf(final Predicate<L> testFn, final Function<L, R> mapFn) {
			if (testFn.test(value)) {
				return right(mapFn.apply(value));
			} else {
				return this;
			}
		}

		@Override public <Z> Z reduce(Function<L, Z> fnLeft, Function<R, Z> fnRight) { return fnLeft.apply(value); }

		@Override public Either<R, L> swap() { return right(value); }
	}

	record Right<L, R>(R value) implements Either<L, R> {

		@Override
		public Right<L, R> as(final Right<?, ?> thiz) {
			if (thiz != this)
				throw new IllegalArgumentException("Right.as: argument must be the pattern variable bound to this Right");
			return this;
		}

		@Override public boolean isRight() { return true; }

		public <Z> Either<Z, R> into() { return right(value); }

		@Override public R expectRight() { return value; }

		@Override public Optional<R> optRight() { return Optional.ofNullable(value); }

		@Override public <P, Q> Either<P, Q> map(final Function<L, P> fnLeft, final Function<R, Q> fnRight) {
			return right(fnRight.apply(value));
		}

		@Override public <Z> Either<L, Z> mapRight(final Function<R, Z> fn) { return right(fn.apply(value)); }

		@Override public <Z> Either<Z, R> mapLeft(final Function<L, Z> fn) { return right(value); }

		@Override public boolean filterRight(final Predicate<R> fn) { return fn.test(value); }

		@Override public Either<L, R> ifRight(final Consumer<R> fn) {
			fn.accept(value);
			return this;
		}

		@Override public Either<L, R> toLeftIf(final Predicate<R> testFn, final Function<R, L> mapFn) {
			if (testFn.test(value)) {
				return left(mapFn.apply(value));
			} else {
				return this;
			}
		}

		@Override public <Z> Z reduce(final Function<L, Z> fnLeft, final Function<R, Z> fnRight) {
			return fnRight.apply(value);
		}

		@Override public Either<R, L> swap() { return left(value); }
	}

	final class Lefts {
		public static <L, R, Z> Function<Either<L, R>, Either<Z, R>> map(final Function<L, Z> fn) {
			return res -> res.mapLeft(fn);
		}

		public static <L, R> Predicate<Either<L, R>> filter(final Predicate<L> fn) {
			return res -> res.filterLeft(fn);
		}

		public static <L, R> Function<Either<L, R>, Either<L, R>> toRightIf(
				final Predicate<L> testFn, final Function<L, R> mapFn) {
			return res -> res.toRightIf(testFn, mapFn);
		}

		public static <L, R> Function<Either<L, R>, Either<R, L>> swap() {
			return Either::swap;
		}

		public static <L, R> Consumer<Either<L, R>> ifLeft(final Consumer<L> fn) {
			return res -> res.ifLeft(fn);
		}

		private Lefts() {
			throw new IllegalStateException("This is a static class and should never be instantiated");
		}
	}

	final class Rights {
		public static <L, R, Z> Function<Either<L, R>, Either<L, Z>> map(final Function<R, Z> fn) {
			return res -> res.mapRight(fn);
		}

		public static <L, R> Predicate<Either<L, R>> filter(final Predicate<R> fn) {
			return res -> res.filterRight(fn);
		}

		public static <L, R> Function<Either<L, R>, Either<L, R>> toLeftIf(
				final Predicate<R> testFn, final Function<R, L> mapFn) {
			return res -> res.toLeftIf(testFn, mapFn);
		}

		public static <L, R> Function<Either<L, R>, Either<R, L>> swap() {
			return Either::swap;
		}

		public static <L, R> Consumer<Either<L, R>> ifRight(final Consumer<R> fn) {
			return res -> res.ifRight(fn);
		}

		private Rights() {
			throw new IllegalStateException("This is a static class and should never be instantiated");
		}
	}
}
