package org.javafn.result;

import org.javafn.result.AnyError.ErrorList;
import org.javafn.result.AnyError.ExceptionError;
import org.javafn.result.Result.Err;
import org.javafn.result.Result.Ok;
import org.javafn.result.Try.ThrowingSupplier;
import org.javafn.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.javafn.result.Result.err;
import static org.javafn.result.Result.ok;

/**
 * Build an object from fields where getting the field may produce errors, for example,
 * parsing a JSON object.
 * <p>
 * Assuming a record {@code Foo(String v1, int v2, List<Bar> v3) { }}, the use pattern would be
 * to create field objects {@code var f1 = new TypedField<String>(...); ...
 * var f3 = new TypedListField<Bar>(...); }, then construct the object as
 *
 * <pre>
 * {@code
 *      Result<AnyError, Foo> fooParsed = new ResultBuilder<Foo>()
 *          .with(f1, () -> get v1 or throw exception )
 *          .with(f2, () -> get v2 or throw exception )
 *          .with(f3, () -> List.of(bar1Parsed, bar2Parsed, ...)
 *          .build(fields -> new Foo(fields.get(f1), fields.get(f2), fields.get(f3)));
 * }</pre>
 * <p>
 * If any of the getters produce an error, the value of fooParsed will be an Err.  The AnyError type
 * will aggregate, so if all fields fail to parse, the result will contain all errors generated.
 * If no errors were generated, the builder is called with a map of the supplied fields to their values.
 */
public class ResultBuilder<B> {

	public static sealed class TypedField<T> permits TypedListField {
		protected final String name;
		protected final Class<T> type;
		public TypedField(final String _name, final Class<T> _type) {
			name = _name;
			type = _type;
		}
		public String name() { return name; }
		public Class<T> type() { return type; }
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null || obj.getClass() != this.getClass()) return false;
			final var that = (TypedField<?>) obj;
			return Objects.equals(this.name, that.name) &&
					Objects.equals(this.type, that.type);
		}
		@Override public int hashCode() { return Objects.hash(name, type); }
		@Override public String toString() {
			return "TypedField[" + "name=" + name + ", " + "type=" + type + ']';
		}
	}
	public static final class TypedListField<T> extends TypedField<T> {
		public TypedListField(final String name, final Class<T> type) {
			super(name, type);
		}
		@Override public String toString() {
			return "TypedListField[" + "name=" + name + ", " + "type=" + type + ']';
		}
	}

	public static class TypedFieldMap {
		private final Map<TypedField<?>, Object> map;
		private TypedFieldMap(final Map<TypedField<?>, Object> _map) { map = Map.copyOf(_map); }
		public <T> T get(final TypedField<T> field) {
			@SuppressWarnings("unchecked")
			final T t = (T) map.get(field);
			return t;
		}
		public <T> List<T> get(final TypedListField<T> field) {
			@SuppressWarnings("unchecked")
			final List<T> t = (List<T>) map.get(field);
			return t;
		}
	}

	private final List<AnyError> errors = new ArrayList<>();
	private final List<Pair<TypedField<?>, Object>> oks = new ArrayList<>();

	public <T> ResultBuilder<B> with(final TypedField<T> field, Try.ThrowingSupplier<Result<AnyError, T>>fn) {
		final Result<AnyError, T> res = Try.get(fn)
				.asErr().map(AnyError::from)
				.asOk().flatMap(Function.identity());
		if (res.isErr) {
			errors.add(res.asErr().get());
		} else {
			oks.add(Pair.of(field, res.asOk().get()));
		}
		return this;
	}

	public <T> ResultBuilder<B> with(final TypedField<T> field, final T defaultIfNull, Try.ThrowingSupplier<Result<AnyError, T>>fn) {
		final Result<AnyError, T> res = Try.get(fn)
				.asErr().filterMap(x -> x instanceof NullPointerException, x -> ok(defaultIfNull))
				.asErr().map(AnyError::from)
				.asOk().flatMap(Function.identity());
		if (res.isErr) {
			errors.add(res.asErr().get());
		} else {
			oks.add(Pair.of(field, res.asOk().get()));
		}
		return this;
	}

	public <T> ResultBuilder<B> with(final TypedField<T> field, final Supplier<T> defaultIfNull, Try.ThrowingSupplier<Result<AnyError, T>>fn) {
		final Result<AnyError, T> res = Try.get(fn)
				.asErr().filterMap(x -> x instanceof NullPointerException, x -> ok(defaultIfNull.get()))
				.asErr().map(AnyError::from)
				.asOk().flatMap(Function.identity());
		if (res.isErr) {
			errors.add(res.asErr().get());
		} else {
			oks.add(Pair.of(field, res.asOk().get()));
		}
		return this;
	}

	public <T> ResultBuilder<B> with(final TypedListField<T> field, final Supplier<List<Result<AnyError, T>>> _values) {
		final var values = _values.get();
		final var errs = values.stream()
				.filter(Result::isErr)
				.map(Err.Get())
				.toList();
		if (errs.isEmpty()) {
			oks.add(Pair.of(field, values.stream().map(Ok.Get()).toList()));
		} else {
			errors.add(new ErrorList(errs));
		}
		return this;
	}

	public Result<AnyError, B> build(final Function<TypedFieldMap, B> fn) {
		if (errors.isEmpty()) {
			final Map<ResultBuilder.TypedField<?>, Object> fields = new HashMap<>();
			oks.forEach(Pair.ForEach(fields::put));
			return ok(fn.apply(new ResultBuilder.TypedFieldMap(fields)));
		} else {
			return err(errors.stream()
					.reduce(new ErrorList(), AnyError::join, AnyError::join));
		}
	}
}
