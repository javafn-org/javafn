package org.javafn.result;

import org.javafn.utils.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import static org.javafn.result.Result.err;

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

	static <O> Result<O> fail(final String errorMessage) {
		return err(new StringError(errorMessage));
	}
	static <O> Result<O> fail(final List<String> errorMessages) {
		return err(new ErrorList(errorMessages.stream().map(AnyError::from).toList()));
	}
	static <E extends Exception, O> Result<O> fail(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new ExceptionError<>(e, type));
	}
	static <E extends Enum<E>, O> Result<O> fail(final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new EnumError<>(e, type));
	}
	/* *******************************************************************************/

	static AnyError from(final String errorMessage) {
		return new StringError(errorMessage);
	}
	static AnyError from(final List<String> errorMessages) {
		return new ErrorList(errorMessages.stream().map(AnyError::from).toList());
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
	static AnyError joined(final List<AnyError> errors) {
		return new ErrorList(errors);
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

	record ExceptionError<E extends Exception>(E error, Class<E> type) implements AnyError {
		@SuppressWarnings("unchecked")
		public ExceptionError(final Exception x) {
			this((E) x, (Class<E>) x.getClass());
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
