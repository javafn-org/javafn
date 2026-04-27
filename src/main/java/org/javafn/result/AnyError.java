package org.javafn.result;

import org.javafn.result.Result.Err;
import org.javafn.utils.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.javafn.result.Result.err;
import static org.javafn.result.Result.ok;

/**
 * A wrapper that can be used with the Result class to represent errors from multiple sources,
 * for example strings or exceptions, as well as aggregate them without worrying too much about
 * their wrapped types.  Each implementation produces a single string {@link #message()},
 * which may be built from a wrapped collection of messages.  These are joined using a newline.
 */
public interface AnyError {

	String message();
	default AnyError join(AnyError that) {
		if (that == null) return this;
		return new ErrorList(List.of(this, that));
	}

	/* *******************************************************************************/

	@SafeVarargs
	static Result<AnyError, Void> joined(final Result<AnyError, ?>... results) {
		return joined(Arrays.asList(results));
	}
	static Result<AnyError, Void> joined(final List<Result<AnyError, ?>> results) {
		return results.stream()
				.filter(Result::isErr)
				.map(Result::expectErr)
				.reduce(AnyError::join)
				.map(Result::<AnyError, Void>err)
				.orElseGet(Result::ok);
	}
	static <K, J, L> Result<AnyError, List<L>> joined(
			final List<K> values,
			final Function<K, Result<AnyError, J>> toRes,
			final Function<K, L> okMapper) {
		return values.stream()
				.map(toRes)
				.filter(Result::isErr)
				.map(Result::expectErr)
				.reduce(AnyError::join)
				.<Result<AnyError, List<L>>>map(Result::err)
				.orElseGet(() -> ok(values.stream()
						.map(okMapper)
						.toList()));
	}

	static <O> Result<AnyError, O> fail(final String errorMessage) {
		return err(new StringError(errorMessage));
	}
	static <O> Result<AnyError, O> fail(final List<String> errorMessages) {
		return err(new StringListError(errorMessages));
	}
	static <E extends Exception, O> Result<AnyError, O> fail(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new ExceptionError<>(e, type));
	}
	static <E extends Enum<E>, O> Result<AnyError, O> fail(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new EnumError<>(e, type));
	}
	/* *******************************************************************************/

	static AnyError from(final String errorMessage) {
		return new StringError(errorMessage);
	}
	static AnyError from(final List<String> errorMessages) {
		return new StringListError(errorMessages);
	}
	static <E extends Exception> AnyError from(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new ExceptionError<>(e, type);
	}
	static <E extends Enum<E>> AnyError from(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new EnumError<>(e, type);
	}

	/* *******************************************************************************/

	record ErrorList(List<AnyError> errors) implements AnyError {
		public ErrorList() {
			this(List.of());
		}
		@Override public String message() {
			return errors.stream()
					.map(AnyError::message)
					.collect(Collectors.joining("\n"));
		}
		@Override
		public ErrorList join(final AnyError that) {
			if (that == null) return this;
			if (that instanceof ErrorList thatList) {
				return new ErrorList(Data.append(errors, thatList.errors));
			} else {
				return new ErrorList(Data.append(errors, that));
			}
		}
	}

	record StringError(String message) implements AnyError {
		@Override public String toString() { return message; }
	}

	record StringListError(List<String> messages) implements AnyError {
		public StringListError() { this(List.of()); }
		@Override public String message() { return String.join("\n", messages); }
		@Override public String toString() { return message(); }
	}

	record ExceptionError<E extends Exception>(E error, Class<E> type) implements AnyError {
		@SuppressWarnings("unchecked")
		public ExceptionError(final Exception x) {
			this((E) x, (Class<E>) x.getClass());
		}
		public ExceptionError(final Err<E, ?> x) {
			this(x.value());
		}
		@Override public String message() {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			error.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
		@Override public String toString() { return message(); }
	}

	record EnumError<E extends Enum<E>>(E error, Class<E> type) implements AnyError {
		@Override public String message() { return error.name(); }
		@Override public String toString() { return message(); }
	}
}
