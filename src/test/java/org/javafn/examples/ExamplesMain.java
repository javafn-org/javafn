package org.javafn.examples;

import org.javafn.examples.DomainTypes.Bar;
import org.javafn.examples.DomainTypes.Foo;
import org.javafn.result.AnyError;
import org.javafn.result.IntResult;
import org.javafn.result.LongResult;
import org.javafn.result.Result;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class ExamplesMain {

    private static final Random RAND = new Random();

    public enum DomainError { INVALID_INPUT, FOO_NOT_FOUND, RANDOM_ERROR }

    public static Result<Foo> doWork() {
        return Result.ok(new Foo());
    }
    public static Result<Bar> doWork(final Foo foo) {
        return Result.ok(new Bar());
    }
    public static IntResult getCount() {
        if (RAND.nextBoolean()) {
            return IntResult.ok(RAND.nextInt());
        } else {
            return IntResult.err(DomainError.RANDOM_ERROR);
        }
    }
    public static List<Foo> getClientFoos() {
        return IntStream.range(0, 20 + RAND.nextInt() % 20)
                .mapToObj(ignored -> new Foo())
                .toList();
    }

    public static void main(final String[] args) {

        //********************************************************************************
        // snips-start: result_creation
        final Result<Foo> ok = Result.ok(new Foo());
        final Result<Foo> err = Result.err("something went wrong");
        final Result<Foo> errEx = Result.err(new IllegalStateException("boom"));
        final Result<Foo> errEnum = Result.err(DomainError.INVALID_INPUT);

        // AnyError factories
        final AnyError a = AnyError.from("simple message");
        final AnyError b = AnyError.from("code %d not found", 404);
        final AnyError c = AnyError.from(new NumberFormatException("not a number"));
        final AnyError joined = a.join(b);        // ErrorList, messages joined with "\n"
        final AnyError alsoJoined = AnyError.joined(a, b, c);

        // AnyError.fail is shorthand for Result.err(AnyError.from(...))
        final Result<Foo> failed = AnyError.fail("validation failed");
        final Result<Foo> other = AnyError.from("another error").fail();
        // snips-end: result_creation

    }

    public static void variantInspect() {
        // snips-start: variant_inspect
        final Result<Foo> res = doWork();
        if (res instanceof Result.Ok<Foo> asOk) {
            final Foo foo = asOk.value();
            foo.doFooThings();
        } else if (res instanceof Result.Err<Foo> asErr) {
            final AnyError err = asErr.value();
            System.err.println("Oh no! " + err.message());
        }
        // snips-end: variant_inspect
    }

    public static void improperExpectVariant() {
        try {
            // snips-start: improper_expect_ok
            final Result<Foo> isErr = Result.err("DON'T DO THIS");
            final Foo foo = isErr.expectOk();
            // snips-end: improper_expect_ok
        } catch (final IllegalStateException x) {
            // expected; for demo purposes
        }
        try {
            // snips-start: improper_expect_err
            final Result<Foo> isOk = Result.ok(new Foo("DON'T DO THIS"));
            final AnyError foo = isOk.expectErr();
            // snips-end: improper_expect_err
        } catch (final IllegalStateException x) {
            // expected; for demo purposes
        }
    }

    public static void optionalFromResult() {
        // snips-start: optional_view
        final Result<Foo> resFoo = doWork();
        final Foo foo = resFoo.opt().orElseGet(() -> new Foo(
                "Create default value if doWork produced an error"));
        final Result<Bar> resBar = doWork(foo);
        resBar.optErr().ifPresent(err ->
                System.err.println("An error was returned: " + err.message()));
        // snips-end: optional_view
    }

    public static void mappingResults() {
        // snips-start: mapping_results
        final Result<Foo> resFoo1 = doWork();
        final Result<Foo> resFoo2 = resFoo1.map(Foo::updateState);
        final Result<Bar> resBar = resFoo2.flatMap(foo -> doWork(foo));
        resBar.ifErr(theErr -> System.err.println(theErr.message()));
        final Result<Bar> maybeRecovered = resBar.flatRecover(err -> {
            if (err instanceof AnyError.EnumError<?> domainErr &&
                    domainErr.error().equals(DomainError.FOO_NOT_FOUND)) {
                // try to recover
                return doWork(new Foo("Try again with good Foo"));
            } else {
                // Leave it as an err
                return Result.err(err);
            }
        });
        // snips-end: mapping_results
    }

    // snips-start: escape_hatch
    public static Result<Bar> demoEscapeHatch() {
        final Foo foo; {
            final Result<Foo> maybeFoo = doWork();
            if (maybeFoo instanceof Result.Err<Foo> asErr) return asErr.into();
            foo = maybeFoo.expectOk();
        }
        // Use foo here.  maybeFoo is not in scope.
        foo.doFooThings();
        foo.updateState();
        final Result<Bar> bar = doWork(foo);
        return bar;
    }
    // snips-end: escape_hatch

    // snips-start: pattern_match
    public LongResult patternMatch() {
        final int intermediate; {
            final IntResult res = getCount();
            if (res instanceof IntResult.Err asErr) {
                return asErr.intoLong();
            } else if (res instanceof IntResult.Ok asOk) {
                intermediate = asOk.value();
            } else {
                throw new IllegalStateException("""
                        Impossible to reach here, but Java 17 does not recognize this.
                        This else block is no longer needed in Java 21.
                        """);
            }
        }

        return LongResult.ok(intermediate + 1);
    }
    // snips-end: pattern_match

    // snips-start: switch_expression
    public LongResult switchExpression() {
        final int intermediate;
        switch (getCount()) {
            case IntResult.Err asErr -> { return asErr.intoLong(); }
            case IntResult.Ok asOk -> intermediate = asOk.value();
        }
        // For Java 17, this is only possible with preview features enabled.
        // Otherwise, a default case is necessary.
        // For Java 21+, this will be the preferred escape hatch syntax.
        // NOTICE: no unsafe "expect" patterns!

        return LongResult.ok(intermediate + 1);
    }
    // snips-end: switch_expression

    public void consumeAndReduce() {
        // snips-start: consume_and_reduce
        doWork().consume(
                err -> System.err.println(err.message()),
                Foo::doFooThings);

        final Bar bar = doWork().reduce(
                err -> {
                    System.err.println(err.message());
                    return new Bar();
                },
                foo -> foo.safeBarGenerate("Both lambdas produce a Bar")
        );
        // snips-end: consume_and_reduce
    }
}
