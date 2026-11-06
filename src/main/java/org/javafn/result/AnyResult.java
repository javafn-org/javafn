package org.javafn.result;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Optional;
import java.util.function.Predicate;

public interface AnyResult {
    boolean isOk();
    boolean isErr();
    @NonNull Optional<AnyError> optErr();
    @NonNull AnyError expectErr();
    boolean filterErr(@NonNull Predicate<AnyError> fn);
    boolean isErrAnd(@NonNull Predicate<AnyError> fn);
}
