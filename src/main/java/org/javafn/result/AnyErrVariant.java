package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Any of the result implementations wraping an error value,
 * obtained by calling e.g., {@link Result#err(AnyError)}.
 * To access {@link Result.Err#value}, You should obtain an Err by doing an instanceof check
 * <pre>{@code
 * final Result<String> res = ...
 * if (res instanceof Err<?> e) {
 *     final AnyError value = e.value();
 * }
 * }</pre>
 */
public interface AnyErrVariant {
    /**
     * Pass through to the underlying {@link AnyError#message()} function.
     */
    @NonNull String message();
    /**
     * Pass through to the underlying {@link AnyError#except()} function.
     */
    @NonNull Exception except();
    /**
     * Pass through to the underlying {@link AnyError#exceptUnchecked()} function.
     */
    @NonNull RuntimeException exceptUnchecked();

    /**
     * Coerce the OK type to a new type and return this otherwise unmodified.
     * This function is only defined for Err variants.
     * You should obtain an Err by doing an instanceof check
     * <pre>{@code
     * public Result<Foo> demoFunction() {
     *     final Result<Bar> res = someOperation();
     *     if (res instanceof Err<?> e) {
     *         return e.into();
     *     }
     *     final Bar bar = res.expectOk();
     * }
     * }</pre>
     * For primitive result types, convert into a Result with the required type
     *
     * @return this Err with the OK type coerced to a new type
     */
    <Z> @NonNull Result.Err<Z> into();

    /**
     * Coerce this SomeResult.Err into an IntResult
     */
    @NonNull IntResult.Err intoInt();
    /**
     * Coerce this SomeResult.Err into a LongResult
     */
    @NonNull LongResult.Err intoLong();
    /**
     * Coerce this SomeResult.Err into a DoubleResult
     */
    @NonNull DoubleResult.Err intoDouble();
    /**
     * Coerce this SomeResult.Err into a VoidResult
     */
    @NonNull VoidResult.Err intoVoid();
}
