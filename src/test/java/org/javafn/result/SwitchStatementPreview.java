package org.javafn.result;

import org.javafn.utils.Expect;

public class SwitchStatementPreview {

    public static void main(String[] args) {
        System.out.println(doTest(true));
        System.out.println(doTest(false));
    }

    public static IntResult doTest(final boolean isErr) {
        // Success variable is final
        final String s;
        switch (genResult(isErr) /* result is not stored in a variable; never in scope */) {
            // err is only in scope for the err arm
            case Result.Err<?> err -> {
                // Immediately return from doTest (coercing the result)
                return err.intoInt();
            }
            // Compiler determines s is definitely assigned in this arm (Java 21+)
            case Result.Ok<String> ok -> s = ok.value();
            // No default case necessary (Java 21+)
            default -> throw Expect.that("switch statement was exhaustive").get();
        }
        // Neither err nor ok is not in scope here.  Only s is in scope.
        return IntResult.ok(s.length());
    }


    public static Result<String> genResult(final boolean isErr) {
        if (isErr) {
            return Result.ok("This is a success!");
        } else {
            return Result.err(new RuntimeException());
        }
    }
}
