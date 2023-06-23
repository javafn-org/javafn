package org.javafn.result;

import java.util.function.Consumer;
import java.util.function.Function;
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
        O get() throws Exception;
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
     * final Result<Exception, Integer> res = Try.get(() -> Integer.parseInt(num, 10));
     * res.asErr().opt().ifPresent(ex -> System.out.println(ex.getClass())); // prints NumberFormatException.class
     * }</pre>
     * Of course, the real value is using this within streams where you can't throw exceptions.
     * <pre>{@code
     * final String[] nums = ...;
     * final int errCount = Arrays.stream(nums)
     *     .map(num -> Try.get() -> Integer.parseInt(num, 10)))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println("There are " + errCount + " bad numbers in the supplied array.");
     * }</pre>
     * @param fn the supplier that produces an O and can throw an exception
     */
    public static <O> Result<Exception, O> get(final ThrowingSupplier<O> fn) {
        final O o;
        try {
            o = fn.get();
        } catch (final Exception e) {
            return Result.err(e);
        }
        return Result.ok(o);
    }

    public static IntResult<Exception> getInt(final IntThrowingSupplier fn) {
        final int i;
        try {
            i = fn.get();
        } catch (final Exception e) {
            return IntResult.err(e);
        }
        return IntResult.ok(i);
    }
    public static LongResult<Exception> getLong(final LongThrowingSupplier fn) {
        final long l;
        try {
            l = fn.get();
        } catch (final Exception e) {
            return LongResult.err(e);
        }
        return LongResult.ok(l);
    }
    public static DoubleResult<Exception> getDouble(final DoubleThrowingSupplier fn) {
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
     * final Result<Exception, Void> res = Result.tryDo(() -> out.flush());
     * }</pre>
     * As with {@link #get(ThrowingSupplier)}, the real value is when this is used in streams.
     * <pre>{@code
     * final FileOutputStreams[] openFiles = ...;
     * final int errCount = Arrays.stream(openFiles)
     *     .map(file -> Try.run(() -> file.close()))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println(errCount + " files failed to close.");
     * }</pre>
     * @param fn the Runnable that can throw an exception
     */
    public static VoidResult<Exception> run(final ThrowingRunnable fn) {
        try {
            fn.run();
        } catch (final Exception e) {
            return VoidResult.err(e);
        }
        return VoidResult.ok();
    }

    public static Supplier<VoidResult<Exception>> Run(final ThrowingRunnable fn) {
        return () -> {
            try {
                fn.run();
            } catch (final Exception e) {
                return VoidResult.err(e);
            }
            return VoidResult.ok();
        };
    }

    /**
     * Return a function that can be used as an argument to {@link Stream#map(Function)} which accepts the
     * map argument and passes it to a lambda that can throw an exception.
     * If an exception is thrown, return an Err, otherwise, a Void Ok is returned.
     * <pre>{@code
     * final FileOutputStreams[] openFiles = ...;
     * final int errCount = Arrays.stream(openFiles)
     *     .map(Try.Accept(file -> file.close()))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println(errCount + " files failed to close.");
     * }</pre>
     * Compare this to {@link #run(ThrowingRunnable)},
     * which executes and returns a Result, this function RETURNS A FUNCTION.
     * Its intended use is in a stream.  Instead of passing a lambda that calls Try.*,
     * this function generates the lambda.
     * Notice the similarity between
     * <pre>{@code
     *     .map(Try.Accept(foo -> foo.bar()))
     *     .map(foo -> Try.run(() -> foo.bar()))
     * }</pre>
     * This function is essentially syntactic sugar for {@link #run(ThrowingRunnable)}.
     * @param fn the consumer that can throw an exception
     */
    public static <T> Function<T, VoidResult<Exception>> Accept(final ThrowingConsumer<T> fn) {
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
     *     .map(Try.Map(num -> Integer.parseInt(num, 10)))
     *     .filter(Result::isErr)
     *     .count();
     * System.out.println("There are " + errCount + " bad numbers in the supplied array.");
     * }</pre>
     * Compare this to {@link #get(ThrowingSupplier)}
     * which executes and returns a Result, this function RETURNS A FUNCTION.
     * Its intended use is in a stream.  Instead of passing a lambda that calls Try.*,
     * this function generates the lambda.
     * Notice the similarity between
     * <pre>{@code
     *     .map(Try.Map(num -> Integer.parseInt(num, 10)))
     *     .map(num -> Try.get(()-> Integer.parseInt(num, 10)))
     * }</pre>
     * This function is essentially syntactic sugar for {@link #get(ThrowingSupplier)}.
     * @param fn the mapping function that can throw an exception
     */
    public static <T, U> Function<T, Result<Exception, U>> Map(final ThrowingFunction<T, U> fn) {
        return t -> {
            final U u;
            try {
                u = fn.map(t);
            } catch (final Exception e) {
                return Result.err(e);
            }
            return Result.ok(u);
        };
    }

    private Try() { throw new IllegalStateException("This is a static class that should never be instantiated."); }
}
