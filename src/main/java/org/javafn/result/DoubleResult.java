package org.javafn.result;


import org.javafn.result.DoubleResultCollection.DoubleResultCollector;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class DoubleResult<ERR> {

	public static <EE> DoubleResultCollector<EE> collector() { return new DoubleResultCollector<>(); }

	public interface Err<E> {
		E get();
		E orElseThrow(DoubleFunction<RuntimeException> exceptionMapper);
		E orElseMap(DoubleFunction<E> fn);
		Optional<E> opt();
		boolean filter(Predicate<E> fn);
		DoubleResult<E> peek(Consumer<E> fn);
		DoubleResult<E> filterMap(Predicate<E> p, ToDoubleFunction<E> fn);
		<Z> DoubleResult<Z> map(Function<E, Z> fn);

		<Z> Result<E, Z> into();
		<Z> List<Result<E, Z>> intoList();
		<Z> Stream<Result<E, Z>> intoStream();
		<Z> DoubleResult<Z> flatMap(Function<E, DoubleResult<Z>> fn);
		static <EE> List<DoubleResult<EE>> Wrap(List<EE> e)
		{ return e.stream().map(DoubleResult::err).collect(Collectors.toList()); }

		static <Z, EE> Function<DoubleResult<EE>, DoubleResult<Z>> Map(Function<EE, Z> fn) {
			return result -> result.asErr().map(fn);
		}
		static <Z, EE> Function<DoubleResult<EE>, Result<EE, Z>> Into() {
			return result -> Result.err(result.asErr().get());
		}
		@SuppressWarnings("unused")
		static <Z, EE> Function<DoubleResult<EE>, Result<EE, Z>> Into(final Class<Z> clazz)
		{ return result -> result.asErr().into(); }
		static <EE, ZZ> Function<DoubleResult<EE>, DoubleResult<ZZ>> FlatMap(Function<EE, DoubleResult<ZZ>> fn)
		{ return result -> result.asErr().flatMap(fn); }
		static <EE> Function<DoubleResult<EE>, EE> Get() {
			return result -> result.asErr().get();
		}
		static <EE> Function<DoubleResult<EE>, EE> OrElseMap(DoubleFunction<EE> fn)
		{ return result -> result.asErr().orElseMap(fn); }
		static <EE> Function<DoubleResult<EE>, EE> OrElseThrow(DoubleFunction<RuntimeException> exceptionMapper)
		{ return result -> result.asErr().orElseThrow(exceptionMapper); }
		static <EE> Function<DoubleResult<EE>, Optional<EE>> Opt() {
			return result -> result.asErr().opt();
		}
		static <EE> Predicate<DoubleResult<EE>> Filter(Predicate<EE> fn)
		{ return result -> result.asErr().filter(fn); }
		static <EE> Consumer<DoubleResult<EE>> Peek(Consumer<EE> fn) { return result -> result.asErr().peek(fn); }
		static <EE> Function<DoubleResult<EE>, DoubleResult<EE>> FilterMap(Predicate<EE> p, ToDoubleFunction<EE> fn)
		{ return result -> result.asErr().filterMap(p, fn); }
	}

	/**
	 * A {@link Result.Projection} of a Result as an Ok value, which may or may not be present.
	 */
	public interface Ok<E> {
		double get();
		double orElseThrow(Function<E, RuntimeException> exceptionMapper);
		double orElseMap(ToDoubleFunction<E> fn);
		OptionalDouble opt();
		boolean filter(DoublePredicate fn);
		DoubleResult<E> peek(DoubleConsumer fn);
		DoubleResult<E> filterMap(DoublePredicate p, DoubleFunction<E> fn);
		DoubleResult<E> map(DoubleUnaryOperator fn);
		IntResult<E> mapToInt(DoubleToIntFunction fn);
		LongResult<E> mapToLong(DoubleToLongFunction fn);
		<Z> Result<E, Z> mapToObj(DoubleFunction<Z> fn);
		<Z> DoubleResult<Z> into();
		<Z> List<DoubleResult<Z>> intoList();
		<Z> Stream<DoubleResult<Z>> intoStream();
		DoubleResult<E> flatMap(DoubleFunction<DoubleResult<E>> fn);

		static <EE> Function<DoubleResult<EE>, DoubleResult<EE>> Map(DoubleUnaryOperator fn)
		{ return result -> result.asOk().map(fn); }
		static <Z, EE> Function<DoubleResult<EE>, DoubleResult<Z>> Into() { return result -> result.asOk().into(); }
		@SuppressWarnings("unused")
		static <Z, EE> Function<DoubleResult<EE>, DoubleResult<Z>> Into(final Class<Z> clazz)
		{ return result -> result.asOk().into(); }
		static <EE> Function<DoubleResult<EE>, DoubleResult<EE>> FlatMap(DoubleFunction<DoubleResult<EE>> fn)
		{ return result -> result.asOk().flatMap(fn); }
		static <EE> ToDoubleFunction<DoubleResult<EE>> Get() { return result -> result.asOk().get(); }
		static <EE> ToDoubleFunction<DoubleResult<EE>> OrElseMap(ToDoubleFunction<EE> fn)
		{ return result -> result.asOk().orElseMap(fn); }
		static <EE> ToDoubleFunction<DoubleResult<EE>> OrElseThrow(Function<EE, RuntimeException> exceptionMapper)
		{ return result -> result.asOk().orElseThrow(exceptionMapper); }
		static <EE> Function<DoubleResult<EE>, OptionalDouble> Opt() { return result -> result.asOk().opt(); }
		static <EE> Predicate<DoubleResult<EE>> Filter(DoublePredicate fn)
		{ return result -> result.asOk().filter(fn); }
		static <EE> Consumer<DoubleResult<EE>> Peek(DoubleConsumer fn) { return result -> result.asOk().peek(fn); }
		static <EE> Function<DoubleResult<EE>, DoubleResult<EE>> FilterMap(DoublePredicate p, DoubleFunction<EE> fn)
		{ return result -> result.asOk().filterMap(p, fn); }
	}

	/**
	 * An implementation of Result and Projection representing an Err value.
	 */
	public static final class ErrProjection<E> extends DoubleResult<E> implements Err<E> {
		final E errValue;
		private ErrProjection(final Sealed _token, final E _errValue) { super(_token, true); errValue = _errValue; }

		@Override public E get() { return errValue; }
		@Override public E orElseThrow(final DoubleFunction<RuntimeException> unused) { return errValue; }
		@Override public Optional<E> opt() { return Optional.of(errValue); }
		@Override public boolean filter(final Predicate<E> fn) { return fn.test(errValue); }
		@Override public DoubleResult<E> peek(final Consumer<E> fn) { fn.accept(errValue); return this; }
		@Override public <Z> DoubleResult<Z> map(final Function<E, Z> fn) { return err(fn.apply(errValue)); }
		@Override public <Z> Result<E, Z> into() { return Result.err(errValue); }
		@Override public <Z> List<Result<E, Z>> intoList() { return Collections.singletonList(Result.err(errValue)); }
		@Override public <Z> Stream<Result<E, Z>> intoStream() { return Stream.of(Result.err(errValue)); }
		@Override public <Z> DoubleResult<Z> flatMap(final Function<E, DoubleResult<Z>> fn) { return fn.apply(errValue); }
		@Override public E orElseMap(DoubleFunction<E> unused) { return errValue; }
		@Override public DoubleResult<E> filterMap(final Predicate<E> p, final ToDoubleFunction<E> fn)
		{ return p.test(errValue) ? ok(fn.applyAsDouble(errValue)) : this; }

		@Override public Ok<E> asOk() { return new EmptyOkProjection<>(this); }

		@Override public Err<E> asErr() { return this; }
		@Override public Result<Double, E> swap() { return Result.ok(errValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> fnErr, final DoubleFunction<MOK> unused)
		{ return Result.err(fnErr.apply(errValue)); }
		@Override public DoubleResult<E> peek(final Consumer<E> fnErr, final DoubleConsumer unused)
		{ fnErr.accept(errValue); return this; }
		@Override public <T> T reduce(final Function<E, T> fnErr, final DoubleFunction<T> unused)
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

	public static final class OkProjection<E> extends DoubleResult<E> implements Ok<E> {
		private final double okValue;
		private OkProjection(final Sealed _token, final double _okValue) { super(_token, false); okValue = _okValue; }

		@Override public double get() { return okValue; }
		@Override public double orElseThrow(final Function<E, RuntimeException> unused) { return okValue; }
		@Override public double orElseMap(ToDoubleFunction<E> unused) { return okValue; }
		@Override public OptionalDouble opt() { return OptionalDouble.of(okValue); }
		@Override public boolean filter(final DoublePredicate fn) { return fn.test(okValue); }
		@Override public DoubleResult<E> peek(final DoubleConsumer fn) { fn.accept(okValue); return this; }
		@Override public DoubleResult<E> map(final DoubleUnaryOperator fn) { return ok(fn.applyAsDouble(okValue)); }
		@Override public IntResult<E> mapToInt(final DoubleToIntFunction fn) { return IntResult.ok(fn.applyAsInt(okValue)); }
		@Override public LongResult<E> mapToLong(final DoubleToLongFunction fn) { return LongResult.ok(fn.applyAsLong(okValue)); }
		@Override public <Z> Result<E, Z> mapToObj(final DoubleFunction<Z> fn) { return Result.ok(fn.apply(okValue)); }
		@Override public <Z> DoubleResult<Z> into() { return ok(okValue); }
		@Override public <Z> List<DoubleResult<Z>> intoList() { return Collections.singletonList(ok(okValue)); }
		@Override public <Z> Stream<DoubleResult<Z>> intoStream() { return Stream.of(ok(okValue)); }
		@Override public DoubleResult<E> flatMap(final DoubleFunction<DoubleResult<E>> fn) { return fn.apply(okValue); }
		@Override public DoubleResult<E> filterMap(final DoublePredicate p, final DoubleFunction<E> fn)
		{ return p.test(okValue) ? err(fn.apply(okValue)) : this; }

		@Override public Ok<E> asOk() { return this; }
		@Override public Err<E> asErr() { return new EmptyErrProjection<>(this); }

		@Override public Result<Double, E> swap() { return Result.err(okValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> unused, final DoubleFunction<MOK> fnOk)
		{ return Result.ok(fnOk.apply(okValue)); }
		@Override public DoubleResult<E> peek(final Consumer<E> unused, final DoubleConsumer fnOk)
		{ fnOk.accept(okValue); return this; }
		@Override public <T> T reduce(final Function<E, T> unused, final DoubleFunction<T> fnOk)
		{ return fnOk.apply(okValue); }
		@Override public String toString() { return "Ok[" + okValue + "]"; }

		@Override public int hashCode() { return Double.hashCode(okValue); }
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
		@Override public E orElseThrow(final DoubleFunction<RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(okResult.okValue); }
		@Override public E orElseMap(final DoubleFunction<E> fn) { return fn.apply(okResult.okValue); }
		@Override public Optional<E> opt() { return Optional.empty(); }
		@Override public boolean filter(final Predicate<E> unused) { return true; }
		@Override public DoubleResult<E> peek(final Consumer<E> unused) { return okResult; }
		@Override public <Z> DoubleResult<Z> map(final Function<E, Z> unused) { return ok(okResult.okValue); }
		@Override public <Z> Result<E, Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> List<Result<E, Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> Stream<Result<E, Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> DoubleResult<Z> flatMap(final Function<E, DoubleResult<Z>> fn) { return ok(okResult.okValue); }
		@Override public DoubleResult<E> filterMap(final Predicate<E> unusedP, final ToDoubleFunction<E> unusedFn)
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

		@Override public double get() { throw new NoSuchElementException("Trying to get an Ok result from an Err Result."); }
		@Override public double orElseThrow(final Function<E, RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(errResult.errValue); }
		@Override public double orElseMap(final ToDoubleFunction<E> fn) { return fn.applyAsDouble(errResult.errValue); }
		@Override public OptionalDouble opt() { return OptionalDouble.empty(); }
		@Override public boolean filter(final DoublePredicate unused) { return true; }
		@Override public DoubleResult<E> peek(final DoubleConsumer unused) { return errResult; }
		@Override public DoubleResult<E> map(final DoubleUnaryOperator unused) { return err(errResult.errValue); }
		@Override public IntResult<E> mapToInt(final DoubleToIntFunction fn) { return IntResult.err(errResult.errValue); }
		@Override public LongResult<E> mapToLong(final DoubleToLongFunction fn) { return LongResult.err(errResult.errValue); }
		@Override public <Z> Result<E, Z> mapToObj(final DoubleFunction<Z> unused) { return Result.err(errResult.errValue); }
		@Override public <Z> DoubleResult<Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> List<DoubleResult<Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> Stream<DoubleResult<Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public DoubleResult<E> flatMap(final DoubleFunction<DoubleResult<E>> unused) { return err(errResult.errValue); }
		@Override public DoubleResult<E> filterMap(final DoublePredicate unusedP, final DoubleFunction<E> unusedFn)
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

	/**
	 * Construct a new Err Result with the supplied value.
	 * <pre>{@code
	 * final Result<String, UUID> res = Result.err("The supplied id is not a valid UUID");
	 * }</pre>
	 * @param err The error value from which a Result will be created
	 * @return a new err variant Result
	 */
	public static <EE> ErrProjection<EE> err(final EE err)
	{ return new ErrProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, err); }

	public static <EE> OkProjection<EE> ok(final double ok)
	{ return new OkProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, ok); }

	public static <EE> DoubleResult<EE> okOrElse(final OptionalDouble maybeOk, final Supplier<EE> errSupplier)
	{ return maybeOk.isPresent() ? ok(maybeOk.getAsDouble()) : err(errSupplier.get()); }

	public boolean isErr() { return isErr; }
	public boolean isOk() { return isOk; }

	public abstract Err<ERR> asErr();
	public abstract Ok<ERR> asOk();

	public abstract Result<Double, ERR> swap();

	public abstract <NEWERR, NEWOK> Result<NEWERR, NEWOK> mapResult(
			Function<ERR, NEWERR> fnErr, DoubleFunction<NEWOK> fnOk);
	public abstract DoubleResult<ERR> peek(Consumer<ERR> fnErr, DoubleConsumer fnOk);

	public abstract <T> T reduce(Function<ERR, T> fnErr, DoubleFunction<T> fnOk);

	public List<DoubleResult<ERR>> list() { return Collections.singletonList(this); }

	public Stream<DoubleResult<ERR>> stream() { return Stream.of(this); }

	public final boolean isErr;
	public final boolean isOk;

	private static final class Sealed {}
	private static final Sealed ADDITIONAL_SUBCLASSES_NOT_ALLOWED = new Sealed();
	/** This class cannot be extended beyond the two subclasses defined in this file */
	private DoubleResult(final Sealed _token, final boolean _isErr) {
		if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
			throw new IllegalArgumentException("Only the subclasses defined in the DoubleResult class may exist");
		isErr = _isErr;
		isOk = !_isErr;
	}
}
