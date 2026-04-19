package org.javafn.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public sealed interface ResultNew<ERR, OK> permits ResultNew.Err, ResultNew.Ok {

	static <ERR, OK> ResultNew<ERR, OK> ok(final OK ok) { return new Ok<>(ok); }
	static <ERR, OK> ResultNew<ERR, OK> err(final ERR err) { return new Err<>(err); }

	default Err<ERR, OK> as(Err<?, ?> thiz) {
		throw new AssertionError("as(Err) called on non-Err instance; use instanceof Err and its pattern variable");
	}
	default Ok<ERR, OK> as(Ok<?, ?> thiz) {
		throw new AssertionError("as(Ok) called on non-Ok instance; use instanceof Ok and its pattern variable");
	}
	default ERR expectErr() {
		throw new AssertionError("Expected Result to be an Err instance, but it was not");
	}
	default OK expectOk() {
		throw new AssertionError("Expected Result to be an Ok instance, but it was not");
	}
	default Optional<ERR> optErr() { return Optional.empty(); }
	default Optional<OK> optOk() { return Optional.empty(); }

	<P, Q> ResultNew<P, Q> map(Function<ERR, P> fnErr, Function<OK, Q> fnOk);
	<Z> ResultNew<ERR, Z> mapOk(Function<OK, Z> fn);
	<Z> ResultNew<Z, OK> mapErr(Function<ERR, Z> fn);

	default boolean filterOk(Predicate<OK> fn) { return true; }
	default boolean filterErr(Predicate<ERR> fn) { return true; }

	default ResultNew<ERR, OK> peekOk(Consumer<OK> fn) { return this; }
	default ResultNew<ERR, OK> peekErr(Consumer<ERR> fn) { return this; }

	ResultNew<ERR, OK> mapOkToErrIf(Predicate<OK> testFn, Function<OK, ERR> mapFn);
	ResultNew<ERR, OK> mapErrToOkIf(Predicate<ERR> testFn, Function<ERR, OK> mapFn);

	<Z> ResultNew<ERR, Z> flatMap(Function<OK, ResultNew<ERR, Z>> fn);

	<Z> Z reduce(Function<ERR, Z> fnErr, Function<OK, Z> fnOk);

	ResultNew<OK, ERR> swap();

	final class Err<ERR, OK> implements ResultNew<ERR, OK> {
		private final ERR v;
		private Err(final ERR _err) { v = _err; }

		public Err<ERR, OK> as(final Err<?, ?> thiz) {
			if (thiz != this) throw new AssertionError("Err.as: argument must be the pattern variable bound to this Err");
			return this;
		}
		public <Z> ResultNew<ERR, Z> into() { return err(v); }
		public ERR expectErr() { return v; }
		public Optional<ERR> optErr() { return Optional.ofNullable(v); }

		public <P, Q> ResultNew<P, Q> map(Function<ERR, P> fnErr, Function<OK, Q> fnOk) {
			return err(fnErr.apply(v));
		}
		public <Z> ResultNew<ERR, Z> mapOk(final Function<OK, Z> fn) { return err(v); }
		public <Z> ResultNew<Z, OK> mapErr(final Function<ERR, Z> fn) { return err(fn.apply(v)); }

		public boolean filterErr(final Predicate<ERR> fn) { return fn.test(v); }
		public ResultNew<ERR, OK> peekErr(final Consumer<ERR> fn) {
			fn.accept(v);
			return this;
		}
		public ResultNew<ERR, OK> mapOkToErrIf(Predicate<OK> testFn, Function<OK, ERR> mapFn) { return this; }
		public ResultNew<ERR, OK> mapErrToOkIf(Predicate<ERR> testFn, Function<ERR, OK> mapFn) {
			if (testFn.test(v)) {
				return ok(mapFn.apply(v));
			} else {
				return this;
			}
		}
		public <Z> ResultNew<ERR, Z> flatMap(Function<OK, ResultNew<ERR, Z>> fn) {
			return err(v);
		}
		public <Z> Z reduce(Function<ERR, Z> fnErr, Function<OK, Z> fnOk) {
			return fnErr.apply(v);
		}
		public ResultNew<OK, ERR> swap() { return ok(v); }
	}
	final class Ok<ERR, OK> implements ResultNew<ERR, OK> {
		private final OK v;
		private Ok(final OK _ok) { v = _ok; }

		public Ok<ERR, OK> as(final Ok<?, ?> thiz) {
			if (thiz != this) throw new AssertionError("Ok.as: argument must be the pattern variable bound to this Ok");
			return this;
		}
		public <Z> ResultNew<Z, OK> into() { return ok(v); }
		public OK expectOk() { return v; }
		public Optional<OK> optOk() { return Optional.ofNullable(v); }

		public <P, Q> ResultNew<P, Q> map(Function<ERR, P> fnErr, Function<OK, Q> fnOk) {
			return ok(fnOk.apply(v));
		}
		public <Z> ResultNew<ERR, Z> mapOk(final Function<OK, Z> fn) { return ok(fn.apply(v)); }
		public <Z> ResultNew<Z, OK> mapErr(final Function<ERR, Z> fn) { return ok(v); }

		public boolean filterOk(final Predicate<OK> fn) { return fn.test(v); }
		public ResultNew<ERR, OK> peekOk(final Consumer<OK> fn) {
			fn.accept(v);
			return this;
		}
		public ResultNew<ERR, OK> mapOkToErrIf(Predicate<OK> testFn, Function<OK, ERR> mapFn) {
			if (testFn.test(v)) {
				return err(mapFn.apply(v));
			} else {
				return this;
			}
		}
		public ResultNew<ERR, OK> mapErrToOkIf(Predicate<ERR> testFn, Function<ERR, OK> mapFn) { return this; }
		public <Z> ResultNew<ERR, Z> flatMap(Function<OK, ResultNew<ERR, Z>> fn) {
			return fn.apply(v);
		}
		public <Z> Z reduce(Function<ERR, Z> fnErr, Function<OK, Z> fnOk) {
			return fnOk.apply(v);
		}
		public ResultNew<OK, ERR> swap() { return err(v); }
	}

	static void main(String[] args) {
		final ResultNew<String, Long> res = testIt();
	}

	static ResultNew<String, Long> testIt() {

		final Integer i;
		{
			final var res1 = ResultNew.<String, Integer>ok(67);
			if (res1 instanceof Err<?, ?> e) {
				return res1.as(e).into();
			}
			i = res1.expectOk();
		}

		return ok(i.longValue());
	}
}
