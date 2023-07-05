package org.javafn.result;

import org.javafn.result.VoidResultCollection.VoidResultCollector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class VoidResult<ERR> {

	public static <EE> VoidResultCollector<EE> collector() { return new VoidResultCollector<>(); }

	public interface Err<E> {
		E get();
		E orElseThrow(Supplier<RuntimeException> exceptionMapper);
		E orElseMap(Supplier<E> fn);
		Optional<E> opt();
		boolean filter(Predicate<E> fn);
		VoidResult<E> peek(Consumer<E> fn);
		VoidResult<E> filterMap(Predicate<E> p, Consumer<E> fn);
		<Z> VoidResult<Z> map(Function<E, Z> fn);

		<Z> Result<E, Z> into();
		<Z> List<Result<E, Z>> intoList();
		<Z> Stream<Result<E, Z>> intoStream();
		<Z> VoidResult<Z> flatMap(Function<E, VoidResult<Z>> fn);
		static <EE> List<VoidResult<EE>> Wrap(List<EE> e)
		{ return e.stream().map(VoidResult::err).collect(Collectors.toList()); }

		static <Z, EE> Function<VoidResult<EE>, VoidResult<Z>> Map(Function<EE, Z> fn) {
			return result -> result.asErr().map(fn);
		}
		static <Z, EE> Function<VoidResult<EE>, Result<EE, Z>> Into() {
			return result -> Result.err(result.asErr().get());
		}
		@SuppressWarnings("unused")
		static <Z, EE> Function<VoidResult<EE>, Result<EE, Z>> Into(final Class<Z> clazz)
		{ return result -> result.asErr().into(); }
		static <EE, ZZ> Function<VoidResult<EE>, VoidResult<ZZ>> FlatMap(Function<EE, VoidResult<ZZ>> fn)
		{ return result -> result.asErr().flatMap(fn); }
		static <EE> Function<VoidResult<EE>, EE> Get() {
			return result -> result.asErr().get();
		}
		static <EE> Function<VoidResult<EE>, EE> OrElseMap(Supplier<EE> fn)
		{ return result -> result.asErr().orElseMap(fn); }
		static <EE> Function<VoidResult<EE>, EE> OrElseThrow(Supplier<RuntimeException> exceptionMapper)
		{ return result -> result.asErr().orElseThrow(exceptionMapper); }
		static <EE> Function<VoidResult<EE>, Optional<EE>> Opt() {
			return result -> result.asErr().opt();
		}
		static <EE> Predicate<VoidResult<EE>> Filter(Predicate<EE> fn)
		{ return result -> result.asErr().filter(fn); }
		static <EE> Consumer<VoidResult<EE>> Peek(Consumer<EE> fn) { return result -> result.asErr().peek(fn); }
		static <EE> Function<VoidResult<EE>, VoidResult<EE>> FilterMap(Predicate<EE> p, Consumer<EE> fn)
		{ return result -> result.asErr().filterMap(p, fn); }
	}

	/**
	 * A {@link Result.Projection} of a Result as an Ok value, which may or may not be present.
	 */
	public interface Ok<E> {
		void orElseThrow(Supplier<RuntimeException> ExceptionSupplier);
		<O> Result<E, O> map(Supplier<O> fn);
		<Z> VoidResult<Z> into();
		<Z> List<VoidResult<Z>> intoList();
		<Z> Stream<VoidResult<Z>> intoStream();
		VoidResult<E> flatMap(Supplier<VoidResult<E>> fn);
		VoidResult<E> filterMap(BooleanSupplier p, Supplier<E> fn);

		static <EE, OO> Function<VoidResult<EE>, Result<EE, OO>> Map(Supplier<OO> fn)
		{ return result -> result.asOk().map(fn); }
		static <Z, EE> Function<VoidResult<EE>, VoidResult<Z>> Into() { return result -> result.asOk().into(); }
		@SuppressWarnings("unused")
		static <Z, EE> Function<VoidResult<EE>, VoidResult<Z>> Into(final Class<Z> clazz)
		{ return result -> result.asOk().into(); }
		static <EE> Function<VoidResult<EE>, VoidResult<EE>> FlatMap(Supplier<VoidResult<EE>> fn)
		{ return result -> result.asOk().flatMap(fn); }
	}

	/**
	 * An implementation of Result and Projection representing an Err value.
	 */
	public static final class ErrProjection<E> extends VoidResult<E> implements Err<E> {
		final E errValue;
		private ErrProjection(final Sealed _token, final E _errValue) { super(_token, true); errValue = _errValue; }

		@Override public E get() { return errValue; }
		@Override public E orElseThrow(final Supplier<RuntimeException> unused) { return errValue; }
		@Override public Optional<E> opt() { return Optional.of(errValue); }
		@Override public boolean filter(final Predicate<E> fn) { return fn.test(errValue); }
		@Override public VoidResult<E> peek(final Consumer<E> fn) { fn.accept(errValue); return this; }
		@Override public <Z> VoidResult<Z> map(final Function<E, Z> fn) { return err(fn.apply(errValue)); }
		@Override public <Z> Result<E, Z> into() { return Result.err(errValue); }
		@Override public <Z> List<Result<E, Z>> intoList() { return Collections.singletonList(Result.err(errValue)); }
		@Override public <Z> Stream<Result<E, Z>> intoStream() { return Stream.of(Result.err(errValue)); }
		@Override public <Z> VoidResult<Z> flatMap(final Function<E, VoidResult<Z>> fn) { return fn.apply(errValue); }
		@Override public E orElseMap(Supplier<E> unused) { return errValue; }
		@Override public VoidResult<E> filterMap(final Predicate<E> p, final Consumer<E> fn) {
			if (p.test(errValue)) {
				fn.accept(errValue);
				return ok();
			}
			return this;
		}

		@Override public Ok<E> asOk() { return new EmptyOkProjection<>(this); }

		@Override public Err<E> asErr() { return this; }
		@Override public Result<Void, E> swap() { return Result.ok(errValue); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> fnErr, final Supplier<MOK> unused)
		{ return Result.err(fnErr.apply(errValue)); }
		@Override public VoidResult<E> peek(final Consumer<E> fnErr, final Runnable unused)
		{ fnErr.accept(errValue); return this; }
		@Override public <T> T reduce(final Function<E, T> fnErr, final Supplier<T> unused)
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

	public static final class OkProjection<E> extends VoidResult<E> implements Ok<E> {
		private OkProjection(final Sealed _token) { super(_token, false); }

		@Override public void orElseThrow(final Supplier<RuntimeException> unused) { }
		@Override public <Z> VoidResult<Z> into() { return ok(); }
		@Override public <Z> List<VoidResult<Z>> intoList() { return Collections.singletonList(ok()); }
		@Override public <Z> Stream<VoidResult<Z>> intoStream() { return Stream.of(ok()); }
		@Override public <Z> Result<E, Z> map(final Supplier<Z> fn) { return Result.ok(fn.get()); }
		@Override public VoidResult<E> flatMap(final Supplier<VoidResult<E>> fn) { return fn.get(); }
		@Override public VoidResult<E> filterMap(BooleanSupplier p, Supplier<E> fn)
		{ return p.getAsBoolean() ? err(fn.get()) : this; }

		@Override public Ok<E> asOk() { return this; }
		@Override public Err<E> asErr() {
			@SuppressWarnings("unchecked")
			final EmptyErrProjection<E> e = singletonEmptyErr;
			return e;
		}

		@Override public Result<Void, E> swap() { return Result.err(null); }
		@Override public <MERR, MOK> Result<MERR, MOK> mapResult(
				final Function<E, MERR> unused, final Supplier<MOK> fnOk)
		{ return Result.ok(fnOk.get()); }
		@Override public VoidResult<E> peek(final Consumer<E> unused, final Runnable fnOk)
		{ fnOk.run(); return this; }
		@Override public <T> T reduce(final Function<E, T> unused, final Supplier<T> fnOk)
		{ return fnOk.get(); }
		@Override public String toString() { return "VoidOk[]"; }

		@Override public int hashCode() { return 0; }
		@Override public boolean equals(final Object other) {
			if (other instanceof OkProjection) {
				return other == this;
			} else if (other instanceof EmptyErrProjection) {
				return ((EmptyErrProjection<?>) other).okResult == this;
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
		@Override public E orElseThrow(final Supplier<RuntimeException> exceptionMapper)
		{ throw exceptionMapper.get(); }
		@Override public E orElseMap(final Supplier<E> fn) { return fn.get(); }
		@Override public Optional<E> opt() { return Optional.empty(); }
		@Override public boolean filter(final Predicate<E> unused) { return true; }
		@Override public VoidResult<E> peek(final Consumer<E> unused) { return okResult; }
		@Override public <Z> VoidResult<Z> map(final Function<E, Z> unused) { return ok(); }
		@Override public <Z> Result<E, Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> List<Result<E, Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> Stream<Result<E, Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Ok Result projected as an Err."); }
		@Override public <Z> VoidResult<Z> flatMap(final Function<E, VoidResult<Z>> fn) { return ok(); }
		@Override public VoidResult<E> filterMap(final Predicate<E> unusedP, final Consumer<E> unusedFn)
		{ return okResult; }
		@Override public String toString() { return okResult.toString(); }

		@Override public int hashCode() { return okResult.hashCode(); }
		@Override public boolean equals(final Object other) {
			if (other instanceof EmptyErrProjection) {
				return Objects.equals(okResult, ((EmptyErrProjection<?>) other).okResult);
			} else if (other instanceof OkProjection) {
				return okResult == other;
			} else {
				return false;
			}
		}
	}

	private static final class EmptyOkProjection<E> implements Ok<E> {
		private final ErrProjection<E> errResult;
		private EmptyOkProjection(final ErrProjection<E> _errResult) { errResult = _errResult; }

		@Override public void orElseThrow(final Supplier<RuntimeException> fn) { throw fn.get(); }
		@Override public <Z> Result<E, Z> map(final Supplier<Z> fn) { return Result.err(errResult.errValue); }

		@Override public <Z> VoidResult<Z> into()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> List<VoidResult<Z>> intoList()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public <Z> Stream<VoidResult<Z>> intoStream()
		{ throw new NoSuchElementException("Trying to alter the error type of an Err Result projected as an Ok."); }
		@Override public VoidResult<E> flatMap(final Supplier<VoidResult<E>> unused) { return err(errResult.errValue); }
		@Override public VoidResult<E> filterMap(BooleanSupplier p, Supplier<E> fn) { return errResult; }
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

	public static <EE> OkProjection<EE> ok() {
		@SuppressWarnings("unchecked")
		final OkProjection<EE> o = (OkProjection<EE>) singletonOk;
		return o;
	}
	@SuppressWarnings("rawtypes")
	private static final OkProjection singletonOk = new OkProjection(VoidResult.ADDITIONAL_SUBCLASSES_NOT_ALLOWED);
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static final EmptyErrProjection singletonEmptyErr = new EmptyErrProjection(singletonOk);

	public boolean isErr() { return isErr; }
	public boolean isOk() { return isOk; }

	public abstract Err<ERR> asErr();
	public abstract Ok<ERR> asOk();

	public abstract Result<Void, ERR> swap();

	public abstract <NEWERR, NEWOK> Result<NEWERR, NEWOK> mapResult(
			Function<ERR, NEWERR> fnErr, Supplier<NEWOK> fnOk);
	public static <E, NEWERR, NEWOK> Function<VoidResult<E>, Result<NEWERR, NEWOK>> MapResult(
			final Function<E, NEWERR> fnErr, final Supplier<NEWOK> fnOk)
	{ return res -> res.mapResult(fnErr, fnOk); }
	public abstract VoidResult<ERR> peek(Consumer<ERR> fnErr, Runnable fnOk);
	public static <E> Function<VoidResult<E>, VoidResult<E>> Peek(Consumer<E> fnErr, Runnable fnOk)
	{ return res -> res.peek(fnErr, fnOk); }

	public abstract <T> T reduce(Function<ERR, T> fnErr, Supplier<T> fnOk);
	public static <E, T> Function<VoidResult<E>, T> Reduce(final Function<E, T> fnErr, final Supplier<T> fnOk)
	{ return res -> res.reduce(fnErr, fnOk); }

	public List<VoidResult<ERR>> list() { return Collections.singletonList(this); }

	public Stream<VoidResult<ERR>> stream() { return Stream.of(this); }

	public final boolean isErr;
	public final boolean isOk;

	private static final class Sealed {}
	private static final Sealed ADDITIONAL_SUBCLASSES_NOT_ALLOWED = new Sealed();
	/** This class cannot be extended beyond the two subclasses defined in this file */
	private VoidResult(final Sealed _token, final boolean _isErr) {
		if (_token != ADDITIONAL_SUBCLASSES_NOT_ALLOWED)
			throw new IllegalArgumentException("Only the subclasses defined in the VoidResult class may exist");
		isErr = _isErr;
		isOk = !_isErr;
	}
}
