package org.javafn.result;

import org.javafn.result.LongResultCollection.LongResultCollector;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class LongResult<ERR> {

	public static <EE> LongResultCollector<EE> collector() { return new LongResultCollector<>(); }

	public interface Err<E> {
		E get();
		E orElseThrow(LongFunction<RuntimeException> exceptionMapper);
		E orElseMap(LongFunction<E> fn);
		Optional<E> opt();
		boolean filter(Predicate<E> fn);
		LongResult<E> peek(Consumer<E> fn);
		LongResult<E> filterMap(Predicate<E> p, ToLongFunction<E> fn);
		<Z> LongResult<Z> map(Function<E, Z> fn);

		<Z> Result<E, Z> into();
		<Z> List<Result<E, Z>> intoList();
		<Z> Stream<Result<E, Z>> intoStream();
		<Z> LongResult<Z> flatMap(Function<E, LongResult<Z>> fn);
		static <EE> List<LongResult<EE>> Wrap(List<EE> e)
		{ return e.stream().map(LongResult::err).collect(Collectors.toList()); }

		static <Z, EE> Function<LongResult<EE>, LongResult<Z>> Map(Function<EE, Z> fn) {
			return result -> result.asErr().map(fn);
		}
		static <Z, EE> Function<LongResult<EE>, Result<EE, Z>> Into() {
			return result -> Result.err(result.asErr().get());
		}
		@SuppressWarnings("unused")
		static <Z, EE> Function<LongResult<EE>, Result<EE, Z>> Into(final Class<Z> clazz)
		{ return result -> result.asErr().into(); }
		static <EE, ZZ> Function<LongResult<EE>, LongResult<ZZ>> FlatMap(Function<EE, LongResult<ZZ>> fn)
		{ return result -> result.asErr().flatMap(fn); }
		static <EE> Function<LongResult<EE>, EE> Get() {
			return result -> result.asErr().get();
		}
		static <EE> Function<LongResult<EE>, EE> OrElseMap(LongFunction<EE> fn)
		{ return result -> result.asErr().orElseMap(fn); }
		static <EE> Function<LongResult<EE>, EE> OrElseThrow(LongFunction<RuntimeException> exceptionMapper)
		{ return result -> result.asErr().orElseThrow(exceptionMapper); }
		static <EE> Function<LongResult<EE>, Optional<EE>> Opt() {
			return result -> result.asErr().opt();
		}
		static <EE> Predicate<LongResult<EE>> Filter(Predicate<EE> fn)
		{ return result -> result.asErr().filter(fn); }
		static <EE> Consumer<LongResult<EE>> Peek(Consumer<EE> fn) { return result -> result.asErr().peek(fn); }
		static <EE> Function<LongResult<EE>, LongResult<EE>> FilterMap(Predicate<EE> p, ToLongFunction<EE> fn)
		{ return result -> result.asErr().filterMap(p, fn); }
	}

	/**
	 * A {@link Result.Projection} of a Result as an Ok value, which may or may not be present.
	 */
	public interface Ok<E> {
		long get();
		long orElseThrow(Function<E, RuntimeException> exceptionMapper);
		long orElseMap(ToLongFunction<E> fn);
		OptionalLong opt();
		boolean filter(LongPredicate fn);
		LongResult<E> peek(LongConsumer fn);
		LongResult<E> filterMap(LongPredicate p, LongFunction<E> fn);
		LongResult<E> map(LongUnaryOperator fn);
		IntResult<E> mapToInt(LongToIntFunction fn);
		DoubleResult<E> mapToDouble(LongToDoubleFunction fn);
		<Z> Result<E, Z> mapToObj(LongFunction<Z> fn);
		<Z> LongResult<Z> into();
		<Z> List<LongResult<Z>> intoList();
		<Z> Stream<LongResult<Z>> intoStream();
		LongResult<E> flatMap(LongFunction<LongResult<E>> fn);

		static <EE> Function<LongResult<EE>, LongResult<EE>> Map(LongUnaryOperator fn)
		{ return result -> result.asOk().map(fn); }
		static <Z, EE> Function<LongResult<EE>, LongResult<Z>> Into() { return result -> result.asOk().into(); }
		@SuppressWarnings("unused")
		static <Z, EE> Function<LongResult<EE>, LongResult<Z>> Into(final Class<Z> clazz)
		{ return result -> result.asOk().into(); }
		static <EE> Function<LongResult<EE>, LongResult<EE>> FlatMap(LongFunction<LongResult<EE>> fn)
		{ return result -> result.asOk().flatMap(fn); }
		static <EE> ToLongFunction<LongResult<EE>> Get() { return result -> result.asOk().get(); }
		static <EE> ToLongFunction<LongResult<EE>> OrElseMap(ToLongFunction<EE> fn)
		{ return result -> result.asOk().orElseMap(fn); }
		static <EE> ToLongFunction<LongResult<EE>> OrElseThrow(Function<EE, RuntimeException> exceptionMapper)
		{ return result -> result.asOk().orElseThrow(exceptionMapper); }
		static <EE> Function<LongResult<EE>, OptionalLong> Opt() { return result -> result.asOk().opt(); }
		static <EE> Predicate<LongResult<EE>> Filter(LongPredicate fn)
		{ return result -> result.asOk().filter(fn); }
		static <EE> Consumer<LongResult<EE>> Peek(LongConsumer fn) { return result -> result.asOk().peek(fn); }
		static <EE> Function<LongResult<EE>, LongResult<EE>> FilterMap(LongPredicate p, LongFunction<EE> fn)
		{ return result -> result.asOk().filterMap(p, fn); }
	}

	/**
	 * An implementation of Result and Projection representing an Err value.
	 */
	public static final class ErrProjection<E> extends LongResult<E> implements Err<E> {
		final E errValue;
		private ErrProjection(final Sealed _token, final E _errValue) { super(_token, true); errValue = _errValue; }

		@Override public E get() { return errValue; }
		@Override public E orElseThrow(final LongFunction<RuntimeException> unused) { return errValue; }
		@Override public Optional<E> opt() { return Optional.of(errValue); }
		@Override public boolean filter(final Predicate<E> fn) { return fn.test(errValue); }
		@Override public LongResult<E> peek(final Consumer<E> fn) { fn.accept(errValue); return this; }
		@Override public <Z> LongResult<Z> map(final Function<E, Z> fn) { return err(fn.apply(errValue)); }
		@Override public <Z> Result<E, Z> into() { return Result.err(errValue); }
		@Override public <Z> List<Result<E, Z>> intoList() { return Collections.singletonList(Result.err(errValue)); }
		@Override public <Z> Stream<Result<E, Z>> intoStream() { return Stream.of(Result.err(errValue)); }
		@Override public <Z> LongResult<Z> flatMap(final Function<E, LongResult<Z>> fn) { return fn.apply(errValue); }
		@Override public E orElseMap(LongFunction<E> unused) { return errValue; }
		@Override public LongResult<E> filterMap(final Predicate<E> p, final ToLongFunction<E> fn)
		{ return p.test(errValue) ? ok(fn.applyAsLong(errValue)) : this; }

		@Override public Ok<E> asOk() { return new EmptyOkProjection<>(this); }

		@Override public Err<E> asErr() { return this; }
		@Override public Result<Long, E> swap() { return Result.ok(errValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> fnErr, final LongFunction<MOK> unused)
		{ return Result.err(fnErr.apply(errValue)); }
		@Override public LongResult<E> peek(final Consumer<E> fnErr, final LongConsumer unused)
		{ fnErr.accept(errValue); return this; }
		@Override public <T> T reduce(final Function<E, T> fnErr, final LongFunction<T> unused)
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

	public static final class OkProjection<E> extends LongResult<E> implements Ok<E> {
		private final long okValue;
		private OkProjection(final Sealed _token, final long _okValue) { super(_token, false); okValue = _okValue; }

		@Override public long get() { return okValue; }
		@Override public long orElseThrow(final Function<E, RuntimeException> unused) { return okValue; }
		@Override public long orElseMap(ToLongFunction<E> unused) { return okValue; }
		@Override public OptionalLong opt() { return OptionalLong.of(okValue); }
		@Override public boolean filter(final LongPredicate fn) { return fn.test(okValue); }
		@Override public LongResult<E> peek(final LongConsumer fn) { fn.accept(okValue); return this; }
		@Override public LongResult<E> map(final LongUnaryOperator fn) { return ok(fn.applyAsLong(okValue)); }
		@Override public IntResult<E> mapToInt(final LongToIntFunction fn) { return IntResult.ok(fn.applyAsInt(okValue)); }
		@Override public DoubleResult<E> mapToDouble(final LongToDoubleFunction fn) { return DoubleResult.ok(fn.applyAsDouble(okValue)); }
		@Override public <Z> Result<E, Z> mapToObj(final LongFunction<Z> fn) { return Result.ok(fn.apply(okValue)); }
		@Override public <Z> LongResult<Z> into() { return ok(okValue); }
		@Override public <Z> List<LongResult<Z>> intoList() { return Collections.singletonList(ok(okValue)); }
		@Override public <Z> Stream<LongResult<Z>> intoStream() { return Stream.of(ok(okValue)); }
		@Override public LongResult<E> flatMap(final LongFunction<LongResult<E>> fn) { return fn.apply(okValue); }
		@Override public LongResult<E> filterMap(final LongPredicate p, final LongFunction<E> fn)
		{ return p.test(okValue) ? err(fn.apply(okValue)) : this; }

		@Override public Ok<E> asOk() { return this; }
		@Override public Err<E> asErr() { return new EmptyErrProjection<>(this); }

		@Override public Result<Long, E> swap() { return Result.err(okValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> unused, final LongFunction<MOK> fnOk)
		{ return Result.ok(fnOk.apply(okValue)); }
		@Override public LongResult<E> peek(final Consumer<E> unused, final LongConsumer fnOk)
		{ fnOk.accept(okValue); return this; }
		@Override public <T> T reduce(final Function<E, T> unused, final LongFunction<T> fnOk)
		{ return fnOk.apply(okValue); }
		@Override public String toString() { return "Ok[" + okValue + "]"; }

		@Override public int hashCode() { return Long.hashCode(okValue); }
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
		@Override public E orElseThrow(final LongFunction<RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(okResult.okValue); }
		@Override public E orElseMap(final LongFunction<E> fn) { return fn.apply(okResult.okValue); }
		@Override public Optional<E> opt() { return Optional.empty(); }
		@Override public boolean filter(final Predicate<E> unused) { return true; }
		@Override public LongResult<E> peek(final Consumer<E> unused) { return okResult; }
		@Override public <Z> LongResult<Z> map(final Function<E, Z> unused) { return ok(okResult.okValue); }
		@Override public <Z> Result<E, Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> List<Result<E, Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> Stream<Result<E, Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> LongResult<Z> flatMap(final Function<E, LongResult<Z>> fn) { return ok(okResult.okValue); }
		@Override public LongResult<E> filterMap(final Predicate<E> unusedP, final ToLongFunction<E> unusedFn)
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

		@Override public long get() { throw new NoSuchElementException("Trying to get an Ok result from an Err Result."); }
		@Override public long orElseThrow(final Function<E, RuntimeException> exceptionMapper)
		{ throw exceptionMapper.apply(errResult.errValue); }
		@Override public long orElseMap(final ToLongFunction<E> fn) { return fn.applyAsLong(errResult.errValue); }
		@Override public OptionalLong opt() { return OptionalLong.empty(); }
		@Override public boolean filter(final LongPredicate unused) { return true; }
		@Override public LongResult<E> peek(final LongConsumer unused) { return errResult; }
		@Override public LongResult<E> map(final LongUnaryOperator unused) { return err(errResult.errValue); }
		@Override public IntResult<E> mapToInt(final LongToIntFunction fn) { return IntResult.err(errResult.errValue); }
		@Override public DoubleResult<E> mapToDouble(final LongToDoubleFunction fn) { return DoubleResult.err(errResult.errValue); }
		@Override public <Z> Result<E, Z> mapToObj(final LongFunction<Z> unused) { return Result.err(errResult.errValue); }
		@Override public <Z> LongResult<Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> List<LongResult<Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> Stream<LongResult<Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public LongResult<E> flatMap(final LongFunction<LongResult<E>> unused) { return err(errResult.errValue); }
		@Override public LongResult<E> filterMap(final LongPredicate unusedP, final LongFunction<E> unusedFn)
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

	public static <EE> OkProjection<EE> ok(final long ok)
	{ return new OkProjection<>(ADDITIONAL_SUBCLASSES_NOT_ALLOWED, ok); }

	public static <EE> LongResult<EE> okOrElse(final OptionalLong maybeOk, final Supplier<EE> errSupplier)
	{ return maybeOk.isPresent() ? ok(maybeOk.getAsLong()) : err(errSupplier.get()); }

	public boolean isErr() { return isErr; }
	public boolean isOk() { return isOk; }

	public abstract Err<ERR> asErr();
	public abstract Ok<ERR> asOk();

	public abstract Result<Long, ERR> swap();

	public abstract <NEWERR, NEWOK> Result<NEWERR, NEWOK> mapResult(
			Function<ERR, NEWERR> fnErr, LongFunction<NEWOK> fnOk);
	public abstract LongResult<ERR> peek(Consumer<ERR> fnErr, LongConsumer fnOk);

	public abstract <T> T reduce(Function<ERR, T> fnErr, LongFunction<T> fnOk);

	public List<LongResult<ERR>> list() { return Collections.singletonList(this); }

	public Stream<LongResult<ERR>> stream() { return Stream.of(this); }

	public final boolean isErr;
	public final boolean isOk;

	private static final class Sealed {}
	private static final Sealed ADDITIONAL_SUBCLASSES_NOT_ALLOWED = new Sealed();
	/** This class cannot be extended beyond the two subclasses defined in this file */
	private LongResult(final Sealed _token, final boolean _isErr) {
		if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
			throw new IllegalArgumentException("Only the subclasses defined in the LongResult class may exist");
		isErr = _isErr;
		isOk = !_isErr;
	}
}
