package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.javafn.result.Result.err;

/**
 * A wrapper that can be used with the Result class to represent errors from multiple sources,
 * for example strings or exceptions, as well as aggregate them without worrying too much about
 * their wrapped types.  Each implementation produces a single string {@link #message()},
 * which may be built from a wrapped collection of messages.  These are joined using a newline.
 */
public interface AnyError {

	/**
	 * Get the message associated with this error.
	 */
	@NonNull String message();

	/**
	 * If this error wraps an ExceptionError, get the exception.  If it wraps anything else, construct a new
	 * RuntimeException from the message and return it.  The stack trace of the new exception will be the
	 * call site of this function, which may not be terribly useful.  This function is meant to be used
	 * in conjunction with Try, where the error is guaranteed to be an ExceptionError, but there's no
	 * way to capture this in the type system.
	 */
	default @NonNull Exception except() {
		return new RuntimeException(message());
	}

	/**
	 * Construct a new ErrorList error type by joining that instance with this.
	 */
	default @NonNull AnyError join(@Nullable final AnyError that) {
		if (that == null) return this;
		if (that instanceof ErrorList el) {
			return new ErrorList(Data.append(List.<AnyError>of(this), el.errors()));
		}
		return new ErrorList(List.of(this, that));
	}

	/** Turn this AnyError into an Err Result */
	default <T> @NonNull Result<T> fail() {
		return Result.err(this);
	}

	/* *******************************************************************************/

	static @NonNull Result<Void> fold(@NonNull final Result<?>... results) {
		return fold(Arrays.asList(results));
	}
	static @NonNull Result<Void> fold(@NonNull final List<Result<?>> results) {
		return results.stream()
				.filter(Result::isErr)
				.map(Result::expectErr)
				.reduce(AnyError::join)
				.map(Result::<Void>err)
				.orElseGet(Result::ok);
	}

	static <O> @NonNull Result<O> fail(@NonNull final String errorMessage) {
		return err(new StringError(errorMessage));
	}
	static <O> @NonNull Result<O> fail(@NonNull final List<String> errorMessages) {
		return err(new ErrorList(errorMessages.stream().map(AnyError::from).toList()));
	}
	static <E extends Exception, O> @NonNull Result<O> fail(@NonNull final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new ExceptionError<>(e, type, Optional.empty()));
	}
	static <E extends Exception, O> @NonNull Result<O> fail(@NonNull final E e, @Nullable final String msgPrefix) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new ExceptionError<>(e, type, Optional.ofNullable(msgPrefix)));
	}
	static <E extends Enum<E>, O> @NonNull Result<O> fail(@NonNull final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new EnumError<>(e, type, Optional.empty()));
	}
	static <E extends Enum<E>, O> @NonNull Result<O> fail(@NonNull final E e, @Nullable final String msgDetails) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return err(new EnumError<>(e, type, Optional.ofNullable(msgDetails)));
	}
	/* *******************************************************************************/

	static @NonNull AnyError from(@NonNull final String errorMessage) {
		return new StringError(errorMessage);
	}
	static @NonNull AnyError from(@NonNull final List<String> errorMessages) {
		return new ErrorList(errorMessages.stream().map(AnyError::from).toList());
	}
	static <E extends Exception> @NonNull AnyError from(@NonNull final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new ExceptionError<>(e, type, Optional.empty());
	}
	static <E extends Exception> @NonNull AnyError from(@NonNull final E e, @Nullable final String msgPrefix) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new ExceptionError<>(e, type, Optional.ofNullable(msgPrefix));
	}
	static <E extends Enum<E>> @NonNull AnyError from(@NonNull final E e) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new EnumError<>(e, type, Optional.empty());
	}
	static <E extends Enum<E>> @NonNull AnyError from(@NonNull final E e, @Nullable final String msgDetails) {
		@SuppressWarnings("unchecked")
		final Class<E> type = (Class<E>) e.getClass();
		return new EnumError<>(e, type, Optional.ofNullable(msgDetails));
	}
	static @NonNull AnyError joined(@NonNull final List<AnyError> errors) {
		return new ErrorList(errors);
	}

	/* *******************************************************************************/

	record ErrorList(@NonNull List<AnyError> errors) implements AnyError {
		public ErrorList() {
			this(List.of());
		}
		@Override public @NonNull String message() {
			return errors.stream()
					.map(AnyError::message)
					.collect(Collectors.joining("\n"));
		}
		@Override
		public @NonNull ErrorList join(@Nullable final AnyError that) {
			if (that == null) return this;
			if (that instanceof ErrorList thatList) {
				return new ErrorList(Data.append(errors, thatList.errors));
			} else {
				return new ErrorList(Data.append(errors, that));
			}
		}
	}

	record StringError(@NonNull String message) implements AnyError {
		@Override public @NonNull String toString() { return message; }
	}

	record ExceptionError<E extends Exception>(
			@NonNull E error,
			@NonNull Class<E> type,
			@NonNull Optional<String> msgPrefix
	) implements AnyError {
		@SuppressWarnings("unchecked")
		public ExceptionError(@NonNull final Exception x) {
			this((E) x, (Class<E>) x.getClass(), Optional.empty());
		}
		@Override public @NonNull Exception except() { return error; }
		@Override public @NonNull String message() {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			msgPrefix.ifPresent(pw::println);
			error.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
		@Override public @NonNull String toString() { return message(); }
	}

	record EnumError<E extends Enum<E>>(
			@NonNull E error,
			@NonNull Class<E> type,
			@NonNull Optional<String> msgDetails
	) implements AnyError {
		@Override public @NonNull String message() { return error.name() + msgDetails().map(s -> ": " + s).orElse(""); }
		@Override public @NonNull String toString() { return message(); }
	}
}
