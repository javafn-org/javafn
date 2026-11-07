package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface AnyResult<OK, THIS extends AnyResult<OK, THIS>> {

    /**
     * Return true if this is an ok variant, false otherwise.
     * This function is useful in a stream pipeline.
     * <pre>{@code
     * somestreamofResults.filter(Result::isOk).map(Result::expectOk)...
     * }</pre>
     * More general usage would involve an instanceof check with pattern matching.
     * <pre>{@code
     * final Result<SomeType> res = doWork();
     * if (res instanceof Ok<SomeType> asOk) {
     *     asOk().value().methodOnSomeType();
     * }
     * }</pre>
     * @return true if this is an ok variant
     */
    default boolean isOk() { return false; }

    /**
     * Return true if this is an err variant, false otherwise.
     * This function is useful in a stream pipeline.
     * <pre>{@code
     * somestreamofResults.filter(Result::isErr).map(Result::expectErr)...
     * }</pre>
     * More general usage would involve an instanceof check with pattern matching.
     * <pre>{@code
     * final SomeType st;
     * {
     *     final Result<SomeType> res = doWork();
     *     if (res instanceof Err<SomeType> asErr) {
     *         return asErr.into();
     *     }
     *     st = res.expectOk();
     * }
     * st.methodOnSomeType();
     * }</pre>
     * @return true if this is an err variant
     */
    default boolean isErr() { return false; }

    /**
     * Get the non-null error value of this result wrapped in an optional, or an empty optional if this result
     * is not an err or the wrapped err value is null.
     */
    default @NonNull Optional<AnyError> optErr() { return Optional.empty(); }

    /**
     * Unsafe operation, only safe in a branch opposite a test against ok
     * or inside a branch that checked {@link #isErr()}.
     * Access the error value or throw an exception.
     * <pre>{@code
     * final Result<String> res = ...;
     * if (res instanceof Ok<?> o) {
     *     ... // do something with ok
     * } else {
     *     AnyError e = res.expectErr();
     * }
     * }</pre>
     * @return the error value in this Result
     * @throws IllegalStateException if this Result is not actually an Err
     */
    default @NonNull AnyError expectErr() {
        throw new IllegalStateException("Expected result to be an Err instance, but it was not");
    }

    /**
     * Apply the supplied predicate to the wrapped error value or return true if this is an ok.
     * Semantically, {@code return (this instanceof Ok || fn.test(err))}
     * If you want {@code &&} semantics, use {@link #isErrAnd(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return true if this is not an error or the result of applying the predicate to the error value
     */
    default boolean filterErr(@NonNull Predicate<AnyError> fn) { return true; }

    /**
     * Apply the supplied predicate to the wrapped error value or return false if this is an ok.
     * Semantically, {@code return (this instanceof Err && fn.test(err))}
     * If you want {@code ||} semantics, use {@link #filterErr(Predicate)}
     * @param fn the predicate to apply to the error value
     * @return false if this is not an error or the result of applying the predicate to the error value
     */
    default boolean isErrAnd(@NonNull Predicate<AnyError> fn) { return false; }

    /**
     * Drop any variant type information and treat this as the sum type,
     * e.g, Result vice Ok or Err.
     * <pre>{@code
     *  Optional.of(foo)
     *      .map(f -> Result.ok(foo)) // Ok<Foo>
     *      // Does not compile because Err<Foo> is not an Ok
     *      .orElseGet(() -> err("Not a foo"));
     * }</pre>
     * <pre>{@code
     *  Optional.of(foo)
     *      .map(f -> Result.ok(foo).res())      // Result<Foo>
     *      .orElseGet(() -> err("Not a foo"));  // Result<Foo>
     * }</pre>
     * or with type hints:
     * <pre>{@code
     *  Optional.of(foo)
     *      .<Result<Foo>>map(f -> Result.ok(foo))// Result<Foo>
     *      .orElseGet(() -> err("Not a foo"));   // Result<Foo>
     * }</pre>
     * @return this as a Result rather than an Ok or Err
     */
    THIS res();

    /**
     * Convert this AnyResult into a {@link Result}.  If this is already a Result,
     * it returns this unmodified.  Otherwise, if it is a primitive, return a Result
     * with the primitive boxed.  Void results are special because they have no valid
     * values (aside from null), and null is not a valid value for {@link Result#ok(Object)}.
     * Therefore, {@link VoidResult} implements this interface using the {@link org.javafn.result.VoidResult.Unit}
     * type defined within, which allows a single value {@link org.javafn.result.VoidResult.Unit#NIL}.
     */
    Result<OK> boxed();

    /**
     * Apply the mapping function to the error value if this is an Err, otherwise return this
     * @param fn the function to apply to an error value
     * @return a new result with the error mapped or this unmodified if this is an ok
     */
    @NonNull THIS mapErr(@NonNull Function<AnyError, AnyError> fn);

    /**
     * Apply the supplied consumer to the wrapped err value, or if this is an ok, perform no operation.
     * <strong>
     *     This function is used to achieve side effects.
     *     You are strongly encouraged to document the side effect if it involves mutating external state.
     * </strong>
     * In the most innocuous case, you're writing to a log.
     * In other cases, you could be modifying a collection, for example,
     * Collecting a list of errors before filtering them from the pipeline.
     * @return this, unmodified
     */
    @NonNull THIS ifErr(@NonNull Consumer<AnyError> fn);
}
