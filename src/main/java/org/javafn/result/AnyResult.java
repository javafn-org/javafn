package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Optional;
import java.util.function.Predicate;

public interface AnyResult<OK> {
    boolean isOk();
    boolean isErr();
    @NonNull Optional<AnyError> optErr();
    @NonNull AnyError expectErr();
    boolean filterErr(@NonNull Predicate<AnyError> fn);
    boolean isErrAnd(@NonNull Predicate<AnyError> fn);

    /**
     * Convert this AnyResult into a {@link Result}.  If this is already a Result,
     * it returns this unmodified.  Otherwise, if it is a primitive, return a Result
     * with the primitive boxed.  Void results are special because they have no valid
     * values (aside from null), and null is not a valid value for {@link Result#ok(Object)}.
     * Therefore, {@link VoidResult} implements this interface using the {@link org.javafn.result.VoidResult.Unit}
     * type defined within, which allows a single value {@link org.javafn.result.VoidResult.Unit#NIL}.
     */
    Result<OK> boxed();
}
