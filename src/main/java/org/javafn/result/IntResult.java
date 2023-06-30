package org.javafn.result;

import org.javafn.result.IntResultCollection.IntResultCollector;
import org.javafn.result.Result.ErrProjection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class IntResult<ERR> {

	public static <EE> IntResultCollector<EE> collector() { return new IntResultCollector<>(); }

	public interface Err<E> {
		E get();
		E orElseThrow(IntFunction<RuntimeException> exceptionMapper);
		E orElseMap(IntFunction<E> fn);
		Optional<E> opt();
		boolean filter(Predicate<E> fn);
		IntResult<E> peek(Consumer<E> fn);
		IntResult<E> filterMap(Predicate<E> p, ToIntFunction<E> fn);
		<Z> IntResult<Z> map(Function<E, Z> fn);

		<Z> Result<E, Z> into();
		<Z> List<Result<E, Z>> intoList();
		<Z> Stream<Result<E, Z>> intoStream();
		<Z> IntResult<Z> flatMap(Function<E, IntResult<Z>> fn);
		static <EE> List<IntResult<EE>> Wrap(List<EE> e)
		{ return e.stream().map(IntResult::err).collect(Collectors.toList()); }

		static <Z, EE> Function<IntResult<EE>, IntResult<Z>> Map(Function<EE, Z> fn) {
			return result -> result.asErr().map(fn);
		}
		static <Z, EE> Function<IntResult<EE>, Result<EE, Z>> Into() {
			return result -> Result.err(result.asErr().get());
		}
		@SuppressWarnings("unused")
		static <Z, EE> Function<IntResult<EE>, Result<EE, Z>> Into(final Class<Z> clazz)
		{ return result -> result.asErr().into(); }
		static <EE, ZZ> Function<IntResult<EE>, IntResult<ZZ>> FlatMap(Function<EE, IntResult<ZZ>> fn)
		{ return result -> result.asErr().flatMap(fn); }
		static <EE> Function<IntResult<EE>, EE> Get() {
			return result -> result.asErr().get();
		}
		static <EE> Function<IntResult<EE>, EE> OrElseMap(IntFunction<EE> fn)
		{ return result -> result.asErr().orElseMap(fn); }
		static <EE> Function<IntResult<EE>, EE> OrElseThrow(IntFunction<RuntimeException> exceptionMapper)
		{ return result -> result.asErr().orElseThrow(exceptionMapper); }
		static <EE> Function<IntResult<EE>, Optional<EE>> Opt() {
			return result -> result.asErr().opt();
		}
		static <EE> Predicate<IntResult<EE>> Filter(Predicate<EE> fn)
		{ return result -> result.asErr().filter(fn); }
		static <EE> Consumer<IntResult<EE>> Peek(Consumer<EE> fn) { return result -> result.asErr().peek(fn); }
		static <EE> Function<IntResult<EE>, IntResult<EE>> FilterMap(Predicate<EE> p, ToIntFunction<EE> fn)
		{ return result -> result.asErr().filterMap(p, fn); }
	}

	/**
	 * A {@link Result.Projection} of a Result as an Ok value, which may or may not be present.
	 */
	public interface Ok<E> {
		int get();
		int orElseThrow(Function<E, RuntimeException> exceptionMapper);
		int orElseMap(ToIntFunction<E> fn);
		OptionalInt opt();
		boolean filter(IntPredicate fn);
		IntResult<E> peek(IntConsumer fn);
		IntResult<E> filterMap(IntPredicate p, IntFunction<E> fn);
		IntResult<E> map(IntUnaryOperator fn);
		LongResult<E> mapToLong(IntToLongFunction fn);
		DoubleResult<E> mapToDouble(IntToDoubleFunction fn);
		<Z> Result<E, Z> mapToObj(IntFunction<Z> fn);
		<Z> IntResult<Z> into();
		<Z> List<IntResult<Z>> intoList();
		<Z> Stream<IntResult<Z>> intoStream();
		IntResult<E> flatMap(IntFunction<IntResult<E>> fn);

		static <EE> Function<IntResult<EE>, IntResult<EE>> Map(IntUnaryOperator fn)
		{ return result -> result.asOk().map(fn); }
		static <Z, EE> Function<IntResult<EE>, IntResult<Z>> Into() { return result -> result.asOk().into(); }
		@SuppressWarnings("unused")
		static <Z, EE> Function<IntResult<EE>, IntResult<Z>> Into(final Class<Z> clazz)
		{ return result -> result.asOk().into(); }
		static <EE> Function<IntResult<EE>, IntResult<EE>> FlatMap(IntFunction<IntResult<EE>> fn)
		{ return result -> result.asOk().flatMap(fn); }
		static <EE> ToIntFunction<IntResult<EE>> Get() { return result -> result.asOk().get(); }
		static <EE> ToIntFunction<IntResult<EE>> OrElseMap(ToIntFunction<EE> fn)
		{ return result -> result.asOk().orElseMap(fn); }
		static <EE> ToIntFunction<IntResult<EE>> OrElseThrow(Function<EE, RuntimeException> exceptionMapper)
		{ return result -> result.asOk().orElseThrow(exceptionMapper); }
		static <EE> Function<IntResult<EE>, OptionalInt> Opt() { return result -> result.asOk().opt(); }
		static <EE> Predicate<IntResult<EE>> Filter(IntPredicate fn)
		{ return result -> result.asOk().filter(fn); }
		static <EE> Consumer<IntResult<EE>> Peek(IntConsumer fn) { return result -> result.asOk().peek(fn); }
		static <EE> Function<IntResult<EE>, IntResult<EE>> FilterMap(IntPredicate p, IntFunction<EE> fn)
		{ return result -> result.asOk().filterMap(p, fn); }
	}

	/**
	 * An implementation of Result and Projection representing an Err value.
	 */
	public static final class ErrProjection<E> extends IntResult<E> implements Err<E> {
		final E errValue;
		private ErrProjection(final Sealed _token, final E _errValue) { super(_token, true); errValue = _errValue; }

		@Override public E get() { return errValue; }
		@Override public E orElseThrow(final IntFunction<RuntimeException> unused) { return errValue; }
		@Override public Optional<E> opt() { return Optional.of(errValue); }
		@Override public boolean filter(final Predicate<E> fn) { return fn.test(errValue); }
		@Override public IntResult<E> peek(final Consumer<E> fn) { fn.accept(errValue); return this; }
		@Override public <Z> IntResult<Z> map(final Function<E, Z> fn) { return err(fn.apply(errValue)); }
		@Override public <Z> Result<E, Z> into() { return Result.err(errValue); }
		@Override public <Z> List<Result<E, Z>> intoList() { return Collections.singletonList(Result.err(errValue)); }
		@Override public <Z> Stream<Result<E, Z>> intoStream() { return Stream.of(Result.err(errValue)); }
		@Override public <Z> IntResult<Z> flatMap(final Function<E, IntResult<Z>> fn) { return fn.apply(errValue); }
		@Override public E orElseMap(IntFunction<E> unused) { return errValue; }
		@Override public IntResult<E> filterMap(final Predicate<E> p, final ToIntFunction<E> fn)
		{ return p.test(errValue) ? ok(fn.applyAsInt(errValue)) : this; }

		@Override public Ok<E> asOk() { return new EmptyOkProjection<>(this); }

		@Override public Err<E> asErr() { return this; }
		@Override public Result<Integer, E> swap() { return Result.ok(errValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> fnErr, final IntFunction<MOK> unused)
		{ return Result.err(fnErr.apply(errValue)); }
		@Override public IntResult<E> peek(final Consumer<E> fnErr, final IntConsumer unused)
		{ fnErr.accept(errValue); return this; }
		@Override public <T> T reduce(final Function<E, T> fnErr, final IntFunction<T> unused)
		{ return fnErr.apply(errValue); }
		@Override public String toString() { return "Err[" + errValue.toString() + "]"; }

		@Override public int hashCode() { return errValue.hashCode(); }
		@Override public boolean equals(final Object other) {
			if (other instanceof ErrProjection) {
				return Objects.equals(errValue, ((ErrProjection<?>) other).errValue);
			} else if (other instanceof EmptyOkProjection) {
				return Objects.equals(errValue, ((EmptyOkProjection<?>) other).errResult.errValue);
			} else {
				return false;
			}
		}
	}

	public static final class OkProjection<E> extends IntResult<E> implements Ok<E> {
		private final int okValue;
		private OkProjection(final Sealed _token, final int _okValue) { super(_token, false); okValue = _okValue; }

		@Override public int get() { return okValue; }
		@Override public int orElseThrow(final Function<E, RuntimeException> unused) { return okValue; }
		@Override public int orElseMap(ToIntFunction<E> unused) { return okValue; }
		@Override public OptionalInt opt() { return OptionalInt.of(okValue); }
		@Override public boolean filter(final IntPredicate fn) { return fn.test(okValue); }
		@Override public IntResult<E> peek(final IntConsumer fn) { fn.accept(okValue); return this; }
		@Override public IntResult<E> map(final IntUnaryOperator fn) { return ok(fn.applyAsInt(okValue)); }
		@Override public LongResult<E> mapToLong(final IntToLongFunction fn) { return LongResult.ok(fn.applyAsLong(okValue)); }
		@Override public DoubleResult<E> mapToDouble(final IntToDoubleFunction fn) { return DoubleResult.ok(fn.applyAsDouble(okValue)); }
		@Override public <Z> Result<E, Z> mapToObj(final IntFunction<Z> fn) { return Result.ok(fn.apply(okValue)); }
		@Override public <Z> IntResult<Z> into() { return ok(okValue); }
		@Override public <Z> List<IntResult<Z>> intoList() { return Collections.singletonList(ok(okValue)); }
		@Override public <Z> Stream<IntResult<Z>> intoStream() { return Stream.of(ok(okValue)); }
		@Override public IntResult<E> flatMap(final IntFunction<IntResult<E>> fn) { return fn.apply(okValue); }
		@Override public IntResult<E> filterMap(final IntPredicate p, final IntFunction<E> fn)
		{ return p.test(okValue) ? err(fn.apply(okValue)) : this; }

		@Override public Ok<E> asOk() { return this; }
		@Override public Err<E> asErr() { return new EmptyErrProjection<>(this); }

		@Override public Result<Integer, E> swap() { return Result.err(okValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> unused, final IntFunction<MOK> fnOk)
		{ return Result.ok(fnOk.apply(okValue)); }
		@Override public IntResult<E> peek(final Consumer<E> unused, final IntConsumer fnOk)
		{ fnOk.accept(okValue); return this; }
		@Override public <T> T reduce(final Function<E, T> unused, final IntFunction<T> fnOk)
		{ return fnOk.apply(okValue); }
		@Override public String toString() { return "Ok[" + okValue + "]"; }

		@Override public int hashCode() { return Integer.hashCode(okValue); }
		@Override public boolean equals(final Object other) {
			if (other instanceof OkProjection) {
				return Objects.equals(okValue, ((OkProjection<?>) other).okValue);
			} else if (other instanceof EmptyErrProjection) {
				return Objects.equals(okValue, ((EmptyErrProjection<?>) other).okResult.okValue);
			} else {
				return false;
			}
		}
	}

	/**
	 * An implementation of Projection representing the empty Err value of an Ok Result
	 */
	private static final class EmptyErrProjection<E> implements Err<E> {
		private final OkProjection<E> okResult;
		private EmptyErrProjection(final OkProjection<E> _okResult) { okResult = _okResult; }

		@Override public E get() { throw new NoSuchElementException("Trying to get an Err from an Ok Projection."); }
		@Override public E orElseThrow(final IntFunction<RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(okResult.okValue); }
		@Override public E orElseMap(final IntFunction<E> fn) { return fn.apply(okResult.okValue); }
		@Override public Optional<E> opt() { return Optional.empty(); }
		@Override public boolean filter(final Predicate<E> unused) { return true; }
		@Override public IntResult<E> peek(final Consumer<E> unused) { return okResult; }
		@Override public <Z> IntResult<Z> map(final Function<E, Z> unused) { return ok(okResult.okValue); }
		@Override public <Z> Result<E, Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> List<Result<E, Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> Stream<Result<E, Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> IntResult<Z> flatMap(final Function<E, IntResult<Z>> fn) { return ok(okResult.okValue); }
		@Override public IntResult<E> filterMap(final Predicate<E> unusedP, final ToIntFunction<E> unusedFn)
		{ return okResult; }
		@Override public String toString() { return okResult.toString(); }

		@Override public int hashCode() { return okResult.hashCode(); }
		@Override public boolean equals(final Object other) {
			if (other instanceof EmptyErrProjection) {
				return Objects.equals(okResult, ((EmptyErrProjection<?>) other).okResult);
			} else if (other instanceof OkProjection) {
				return Objects.equals(okResult.okValue, ((OkProjection<?>) other).okValue);
			} else {
				return false;
			}
		}
	}

	private static final class EmptyOkProjection<E> implements Ok<E> {
		private final ErrProjection<E> errResult;
		private EmptyOkProjection(final ErrProjection<E> _errResult) { errResult = _errResult; }

		@Override public int get() { throw new NoSuchElementException("Trying to get an Ok result from an Err Result."); }
		@Override public int orElseThrow(final Function<E, RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(errResult.errValue); }
		@Override public int orElseMap(final ToIntFunction<E> fn) { return fn.applyAsInt(errResult.errValue); }
		@Override public OptionalInt opt() { return OptionalInt.empty(); }
		@Override public boolean filter(final IntPredicate unused) { return true; }
		@Override public IntResult<E> peek(final IntConsumer unused) { return errResult; }
		@Override public IntResult<E> map(final IntUnaryOperator unused) { return err(errResult.errValue); }
		@Override public LongResult<E> mapToLong(final IntToLongFunction fn) { return LongResult.err(errResult.errValue); }
		@Override public DoubleResult<E> mapToDouble(final IntToDoubleFunction fn) { return DoubleResult.err(errResult.errValue); }
		@Override public <Z> Result<E, Z> mapToObj(final IntFunction<Z> unused) { return Result.err(errResult.errValue); }
		@Override public <Z> IntResult<Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> List<IntResult<Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> Stream<IntResult<Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public IntResult<E> flatMap(final IntFunction<IntResult<E>> unused) { return err(errResult.errValue); }
		@Override public IntResult<E> filterMap(final IntPredicate unusedP, final IntFunction<E> unusedFn)
		{ return errResult; }
		@Override public String toString() { return errResult.toString(); }

		@Override public int hashCode() { return errResult.hashCode(); }
		@Override public boolean equals(final Object other) {
			if (other instanceof EmptyOkProjection) {
				return Objects.equals(errResult, ((EmptyOkProjection<?>) other).errResult);
			} else if (other instanceof ErrProjection) {
				return Objects.equals(errResult.errValue, ((ErrProjection<?>) other).errValue);
			} else {
				return false;
			}
		}
	}

	public static <EE> ErrProjection<EE> err(final EE err)
	{ return new ErrProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, err); }

	@SafeVarargs
	public static <EE, OO> ErrProjection<List<EE>> errList(final EE... errs)
	{ return new ErrProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, Arrays.asList(errs)); }

	public static <EE> OkProjection<EE> ok(final int ok)
	{ return new OkProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, ok); }

	public static <EE> IntResult<EE> okOrElse(final OptionalInt maybeOk, final Supplier<EE> errSupplier)
	{ return maybeOk.isPresent() ? ok(maybeOk.getAsInt()) : err(errSupplier.get()); }

	public boolean isErr() { return isErr; }
	public boolean isOk() { return isOk; }

	public abstract Err<ERR> asErr();
	public abstract Ok<ERR> asOk();

	public abstract Result<Integer, ERR> swap();

	public abstract <NEWERR, NEWOK> Result<NEWERR, NEWOK> mapResult(
			Function<ERR, NEWERR> fnErr, IntFunction<NEWOK> fnOk);
	public abstract IntResult<ERR> peek(Consumer<ERR> fnErr, IntConsumer fnOk);

	public abstract <T> T reduce(Function<ERR, T> fnErr, IntFunction<T> fnOk);

	public List<IntResult<ERR>> list() { return Collections.singletonList(this); }

	public Stream<IntResult<ERR>> stream() { return Stream.of(this); }

	public final boolean isErr;
	public final boolean isOk;

	private static final class Sealed {}
	private static final Sealed ADDITIONAL_SUBCLASSES_NOT_ALLOWED = new Sealed();
	/** This class cannot be extended beyond the two subclasses defined in this file */
	private IntResult(final Sealed _token, final boolean _isErr) {
		if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
			throw new IllegalArgumentException("Only the subclasses defined in the IntResult class may exist");
		isErr = _isErr;
		isOk = !_isErr;
	}
}
