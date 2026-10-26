package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.javafn.utils.Expect;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Try {

    /**
     * An interface similar to {@link Supplier} that declares that it throws an Exception.
     * We can't specify the type of exception that can be thrown, so we default to Exception.
     * We don't generalize further to Throwable since these indicate major errors that should
     * require the system to shutdown.
     * @param <O> The type of data that is returned when an exception is not thrown
     */
    @FunctionalInterface public interface ThrowingSupplier<O> {
        @Nullable O get() throws Exception;
    }
    @FunctionalInterface public interface IntThrowingSupplier {
        int get() throws Exception;
    }
    @FunctionalInterface public interface LongThrowingSupplier {
        long get() throws Exception;
    }
    @FunctionalInterface public interface DoubleThrowingSupplier {
        double get() throws Exception;
    }
    @FunctionalInterface public interface ToIntThrowingFunction<V> {
        int applyAsInt(@NonNull V v) throws Exception;
    }
    @FunctionalInterface public interface ToLongThrowingFunction<V> {
        long applyAsLong(@NonNull V v) throws Exception;
    }
    @FunctionalInterface public interface ToDoubleThrowingFunction<V> {
        double applyAsDouble(@NonNull V v) throws Exception;
    }
    @FunctionalInterface public interface FromIntThrowingFunction<V> {
        @Nullable V apply(int i) throws Exception;
    }
    @FunctionalInterface public interface FromLongThrowingFunction<V> {
        @Nullable V apply(long l) throws Exception;
    }
    @FunctionalInterface public interface FromDoubleThrowingFunction<V> {
        @Nullable V apply(double d) throws Exception;
    }

    /**
     * An interface similar to {@link Runnable} that declares that it throws an Exception.
     * We can't specify the type of exception that can be thrown, so we default to Exception.
     * We don't generalize further to Throwable since these indicate major errors that should
     * require the system to shutdown.
     */
    @FunctionalInterface public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * An interface similar to {@link Consumer} that declares that it throws an Exception.
     * We can't specify the type of exception that can be thrown, so we default to Exception.
     * We don't generalize further to Throwable since these indicate major errors that should
     * require the system to shutdown.
     * @param <O> The type of data that is accepted and may cause an exception to be thrown
     */
    @FunctionalInterface public interface ThrowingConsumer<O> {
        void accept(O o) throws Exception;
    }

    /**
     * An interface similar to Function that declares that it throws an Exception.
     * We can't specify the type of exception that can be thrown, so we default to Exception.
     * We don't generalize further to Throwable since these indicate major errors that should
     * require the system to shutdown.
     * @param <T> The type of data that is mapped by the function
     * @param <U> The type of data that is returned by the function
     */
    @FunctionalInterface public interface ThrowingFunction<T, U> {
        U map(T t) throws Exception;
    }

    /**
     * Try to call the supplied supplier.  If an exception is thrown, return an Err, otherwise,
     * return the value that was obtained as an Ok.
     * <pre>{@code
     * final String num = "3.14159z";
     * final Result<Integer> res = Try.toGet(() -> Integer.parseInt(num, 10));
     * res.optErr().ifPresent(ex -> System.out.println(ex.except().getClass())); // prints NumberFormatException.class
     * }</pre>
     * Of course, the real value is using this within streams where you can't throw exceptions.
     * <pre>{@code
     * final String[] nums = ...;
     * final int errCount = Arrays.stream(nums)
     *     .map(num -> Try.toGet(() -> Integer.parseInt(num, 10)))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println("There are " + errCount + " bad numbers in the supplied array.");
     * }</pre>
     * @see #toMap(ThrowingFunction)
     * @param fn the supplier that produces an O and can throw an exception
     */
    public static <O> @Nonnull Result<O> toGet(@Nonnull final ThrowingSupplier<O> fn) {
        final O o;
        try {
            o = fn.get();
        } catch (final Exception e) {
            return AnyError.fail(e);
        }
        return Result.ok(o);
    }

    public static @Nonnull IntResult toGetInt(@Nonnull final IntThrowingSupplier fn) {
        final int i;
        try {
            i = fn.get();
        } catch (final Exception e) {
            return IntResult.err(e);
        }
        return IntResult.ok(i);
    }
    public static @Nonnull LongResult toGetLong(@Nonnull final LongThrowingSupplier fn) {
        final long l;
        try {
            l = fn.get();
        } catch (final Exception e) {
            return LongResult.err(e);
        }
        return LongResult.ok(l);
    }
    public static @Nonnull DoubleResult toGetDouble(@Nonnull final DoubleThrowingSupplier fn) {
        final double d;
        try {
            d = fn.get();
        } catch (final Exception e) {
            return DoubleResult.err(e);
        }
        return DoubleResult.ok(d);
    }

    /**
     * Try to call the supplied runnable.  If an exception is thrown, return an Err, otherwise,
     * return a Void Ok.
     * <pre>{@code
     * final OutputStream out = ...;
     * final Result<AnyError, Void> res = Try.run(() -> out.flush());
     * }</pre>
     * As with {@link #toGet(ThrowingSupplier)}, the real value is when this is used in streams.
     * <pre>{@code
     * final FileOutputStreams[] openFiles = ...;
     * final int errCount = Arrays.stream(openFiles)
     *     .map(file -> Try.to(() -> file.close()))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println(errCount + " files failed to close.");
     * }</pre>
     * @param fn the Runnable that can throw an exception
     */
    public static @Nonnull VoidResult to(@Nonnull final ThrowingRunnable fn) {
        try {
            fn.run();
        } catch (final Exception e) {
            return VoidResult.err(e);
        }
        return VoidResult.ok();
    }

    /**
     * Return a function that can be used as an argument to {@link Stream#map(Function)} which accepts the
     * map argument and passes it to a lambda that can throw an exception.
     * If an exception is thrown, return an Err, otherwise, a Void Ok is returned.
     * <pre>{@code
     * final FileOutputStreams[] openFiles = ...;
     * final int errCount = Arrays.stream(openFiles)
     *     .map(Try.toUse(file -> file.close()))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println(errCount + " files failed to close.");
     * }</pre>
     * Compare this to {@link #to(ThrowingRunnable)},
     * which executes and returns a Result, this function RETURNS A FUNCTION.
     * Its intended use is in a stream.  Instead of passing a lambda that calls Try.*,
     * this function generates the lambda.
     * Notice the similarity between
     * <pre>{@code
     *     .map(Try.toUse(foo -> foo.bar()))
     *     .map(foo -> Try.to(() -> foo.bar()))
     * }</pre>
     * This function is essentially syntactic sugar for {@link #to(ThrowingRunnable)}.
     * @param fn the consumer that can throw an exception
     */
    public static <T> @Nonnull Function<T, VoidResult> toUse(@Nonnull final ThrowingConsumer<T> fn) {
        return t -> {
            try {
                fn.accept(t);
            } catch (final Exception e) {
                return VoidResult.err(e);
            }
            return VoidResult.ok();
        };
    }

    /**
     * Return a function that can be used as an argument to {@link Stream#map(Function)} which accepts the
     * map argument and passes it to a lambda that can throw an exception.
     * If an exception is thrown, return an Err.
     * Otherwise, the function's return value is wrapped in an Ok and returned.
     * <pre>{@code
     * final String[] nums = ...;
     * final int errCount = Arrays.stream(nums)
     *     .map(Try.toMap(num -> Integer.parseInt(num, 10)))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println("There are " + errCount + " bad numbers in the supplied array.");
     * }</pre>
     * Compare this to {@link #toGet(ThrowingSupplier)}
     * which executes and returns a Result, this function RETURNS A FUNCTION.
     * Its intended use is in a stream.  Instead of passing a lambda that calls Try.*,
     * this function generates the lambda.
     * Notice the similarity between
     * <pre>{@code
     *     .map(Try.toMap(num -> Integer.parseInt(num, 10)))
     *     .map(num -> Try.get(() -> Integer.parseInt(num, 10)))
     * }</pre>
     * This function is essentially syntactic sugar for {@link #toGet(ThrowingSupplier)}.
     * @param fn the mapping function that can throw an exception
     */
    public static <T, U> @Nonnull Function<T, Result<U>> toMap(@Nonnull final ThrowingFunction<T, U> fn) {
        return t -> {
            final U u;
            try {
                u = fn.map(t);
            } catch (final Exception e) {
                return AnyError.fail(e);
            }
            return Result.ok(u);
        };
    }

    public static <T> @Nonnull Function<T, IntResult> toMapToInt(@Nonnull final ToIntThrowingFunction<T> fn) {
        return t -> {
            final int u;
            try {
                u = fn.applyAsInt(t);
            } catch (final Exception e) {
                return IntResult.err(e);
            }
            return IntResult.ok(u);
        };
    }
    public static <T> @Nonnull Function<T, LongResult> toMapToLong(@Nonnull final ToLongThrowingFunction<T> fn) {
        return t -> {
            final long u;
            try {
                u = fn.applyAsLong(t);
            } catch (final Exception e) {
                return LongResult.err(e);
            }
            return LongResult.ok(u);
        };
    }
    public static <T> @Nonnull Function<T, DoubleResult> toMapToDouble(@Nonnull final ToDoubleThrowingFunction<T> fn) {
        return t -> {
            final double u;
            try {
                u = fn.applyAsDouble(t);
            } catch (final Exception e) {
                return DoubleResult.err(e);
            }
            return DoubleResult.ok(u);
        };
    }
    public static <T> @Nonnull IntFunction<Result<T>> toMapFromInt(@Nonnull final FromIntThrowingFunction<T> fn) {
        return t -> {
            final T u;
            try {
                u = fn.apply(t);
            } catch (final Exception e) {
                return Result.err(e);
            }
            return Result.ok(u);
        };
    }
    public static <T> @Nonnull LongFunction<Result<T>> toMapFromLong(@Nonnull final FromLongThrowingFunction<T> fn) {
        return t -> {
            final T u;
            try {
                u = fn.apply(t);
            } catch (final Exception e) {
                return Result.err(e);
            }
            return Result.ok(u);
        };
    }
    public static <T> @Nonnull DoubleFunction<Result<T>> toMapFromDouble(@Nonnull final FromDoubleThrowingFunction<T> fn) {
        return t -> {
            final T u;
            try {
                u = fn.apply(t);
            } catch (final Exception e) {
                return Result.err(e);
            }
            return Result.ok(u);
        };
    }

    private Try() {Expect.staticConstructorNotCalled(Try.class);}
}
