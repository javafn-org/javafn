package org.javafn.examples;

import org.javafn.examples.DomainTypes.Bar;
import org.javafn.examples.DomainTypes.Foo;
import org.javafn.examples.DomainTypes.HttpResponse;
import org.javafn.result.AnyError;
import org.javafn.either.Either;
import org.javafn.result.IntResult;
import org.javafn.result.LongResult;
import org.javafn.result.Result;
import org.javafn.result.Result.Oks;
import org.javafn.result.ResultBatch;
import org.javafn.result.Results;
import org.javafn.result.Try;
import org.javafn.result.VoidResult;
import org.javafn.utils.Expect;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

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
                        Impossible to reach here,
                        but Java 17 does not recognize it.
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
            default -> throw Expect
                    .exhaustiveMatchOnSealedType(IntResult.class);
        }
        // For Java 21+, this will be the preferred escape hatch syntax.
        // default case necessary until Java21+
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

    public static void sideEffectsIfOkIfErr() {
        // snips-start: side_effects_ifok_iferr
        final Result<Foo> res = Result.ok(new Foo());

        res.ifOk(v -> System.out.println("Ok: " + v))
           .ifErr(e -> System.err.println("Err: " + e.message()))
           .map(Foo::updateState);
        // snips-end: side_effects_ifok_iferr
    }

    public static void predicates() {
        // snips-start: predicates
        final IntResult r = IntResult.ok(10);

        // || semantics: true if not an ok, else predicate result
        final boolean okIfPositive = r.filter(i -> i > 0);

        // && semantics: false if not an ok, else predicate result
        final boolean okAndPositive = r.isOkAnd(i -> i > 0);

        // error-side equivalents
        final IntResult err = IntResult.err("bad");
        final boolean errMatches = err.filterErr(
                e -> e.message().contains("bad"));
        final boolean errAndMatches = err.isErrAnd(
                e -> e.message().contains("bad"));
        // snips-end: predicates
    }

    public static void recoverIf() {
        // snips-start: recover_if
        final IntResult recovered = IntResult.err("parse failed")
                .recoverIf(
                    e -> e.message().contains("parse"),
                    e -> 0);
        // snips-end: recover_if
    }

    public static void failIf() {
        // snips-start: fail_if
        final Result<Foo> checked = Result.ok(new Foo("hello"))
                .failIf(
                    not(Foo::isStateValid),
                    foo -> AnyError.from("validation failed"));
        // snips-end: fail_if
    }

    public static void resultBatch() {
        // snips-start: result_batch
        final List<Result<Foo>> results = List.of(
                Result.ok(new Foo("A")),
                Result.err("boom"),
                Result.ok(new Foo("B")),
                Result.err("bang")
        );

        final ResultBatch<Foo> batch = results.stream().collect(Result.batch());
        // batch.errors() ==> ["boom", "bang"]
        // batch.oks() ==> [fooA, fooB]

        final Result<List<Foo>> folded = batch.fold();
        // folded -> Err(joined errors) because there were errors
        // snips-end: result_batch
    }

    public static void resultErrsOks() {
        // snips-start: result_errs_oks
        final Result<Foo> result1 = Result.ok(new Foo());
        final Result<Foo> result2 = Result.err("failed");

        final List<Foo> validFoos = Stream.of(result1, result2)
                .map(Result.Errs.map(e ->
                        e.withContext("During example")))
                .peek(Result.Errs.ifErr(e -> {
                    if (e.message().contains("fatal")) {
                        System.err.println(e);
                    } else {
                        System.out.println(e);
                    }
                }))
                .map(Oks.map(Foo::updateState))
                .filter(Oks.isOkAnd(Foo::isStateValid))
                .map(Result::expectOk)
                .toList();
        // snips-end: result_errs_oks
    }

    public static void flatRecoverTodo() {
        // snips-start: flat_recover_todo
        // TODO — flatRecover vs recoverIf: the idiomatic distinction between
        // the two isn't fully settled yet. Write a comparison example (recover
        // a known error vs. transform an error into a new ok value).
        // snips-end: flat_recover_todo
    }

    public static void tryUsage() {
        // snips-start: try_usage
        // Single value
        final Result<Integer> parsed = Try.toGet(
                () -> Integer.parseInt("42"));   // Ok(42)
        final Result<Integer> bad = Try.toGet(
                () -> Integer.parseInt("zzz"));  // Err

        // Primitive supplier
        final IntResult parsedInt = Try.toGetInt(() -> Integer.parseInt("42"));

        // Runnable that might throw -> VoidResult
        final VoidResult flushed = Try.to(
                () -> System.out.println("flushed"));

        // In streams
        final List<String> numbers = List.of("1", "2", "zzz", "4");

        final List<IntResult> results = numbers.stream()
            .map(Try.toMapToInt(Integer::parseInt))
            .toList();
        // snips-end: try_usage
    }

    public static void tryToUse() {
        // snips-start: try_to_use
        final long failedCloses =
                getClientFoos().stream()
                .map(Foo::open)
                .map(Try.toUse(Closeable::close))
                .filter(VoidResult::isErr)
                .count();
        // snips-end: try_to_use
    }

    public static void tryExceptionTypesTodo() {
        // snips-start: try_exception_types_todo
        // TODO — Try cannot let you select which exception types to catch.
        // Document the recommended workaround (catch Exception, then inspect
        // except() / pattern-match on the wrapped exception). Show a worked
        // example of filtering Try results by exception type.
        // snips-end: try_exception_types_todo
    }

    public static void tryNamingTodo() {
        // snips-start: try_naming_todo
        // TODO — verify exact naming: toGet, toMap, toUse, to, and the
        // primitive variants. Cross-check against the production codebase.
        // snips-end: try_naming_todo
    }

    public static void eitherUsage() {
        // snips-start: either_usage
        // An export job runs either at a specific instant or on a recurring weekday.
        // Both alternatives are equally valid — this is a sum type, not error handling.
        final Either<Instant, DayOfWeek> schedule = Either.right(DayOfWeek.WEDNESDAY);

        // Compute the next run time regardless of which alternative we have.
        final Instant now = Instant.now();
        final Instant nextRun = schedule.reduce(
            instant -> instant.isAfter(now) ? instant : now.plusSeconds(86_400),
            day -> LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(day))
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC)
        );

        // Pattern match to describe the schedule
        final String description;
        if (schedule instanceof Either.Left<Instant, DayOfWeek> left) {
            final Instant instant = left.value();
            description = "One-time export at " + instant;
        } else if (schedule instanceof Either.Right<Instant, DayOfWeek> right) {
            final DayOfWeek dayOfWeek = right.value();
            description = "Recurring export on " + dayOfWeek;
        } else {
            throw new IllegalStateException("Not possible, but required until Java21+");
        }
        System.out.println(description);

        // Same thing with a reduce
        final String sameDescription = schedule.reduce(
                instant -> "One-time export at " + instant,
                dayOfWeek -> "Recurring export on " + dayOfWeek);
        System.out.println(sameDescription);

        // Side effect: log the schedule details, returning the Either unmodified
        schedule.use(
            instant -> System.out.println("Scheduled at: " + instant),
            day -> System.out.println("Recurring on: " + day)
        );

        // snips-end: either_usage
    }

    public static void eitherDifferencesTodo() {
        // snips-start: either_differences
        // TODO — confirm any behavioral differences between Either and Result
        // beyond naming (e.g. of(...), toRightIf/toLeftIf, swap).
        // snips-end: either_differences
    }

    public static void resultBatchDetailedTodo() {
        // snips-start: result_batch_detailed
        // TODO — write a real-world "validate all fields and report every
        // error" example.
        // snips-end: result_batch_detailed
    }

    public static void intResult() {
        // snips-start: int_result
        final IntResult r = IntResult.ok(7);

        // primitive-aware mapping (no boxing)
        final IntResult doubled = r.map(i -> i * 2);

        // convert back to object Result
        final Result<Integer> boxed = r.boxed();
        final Result<String> asObj = r.mapToObj(i -> "num=" + i);

        // Err conversions preserve the error
        final IntResult.Err err = IntResult.err("bad");
        final LongResult longErr = err.intoLong();
        // snips-end: int_result
    }

    public static void voidResult() {
        // snips-start: void_result
        final VoidResult vr = VoidResult.ok();
        vr.ifOk(() -> System.out.println("done"));
        vr.ifErr(e -> System.err.println(e.message()));

        // failIf on VoidResult uses BooleanSupplier / Supplier<AnyError>
        final VoidResult checked = VoidResult.ok()
            .failIf(() -> false, () -> AnyError.from("condition failed"));
        // snips-end: void_result
    }

    public static void primitiveDifferencesTodo() {
        // snips-start: primitive_differences_todo
        // TODO — confirm whether LongResult/DoubleResult have any methods
        // IntResult lacks (or vice versa) beyond the obvious type changes.
        // snips-end: primitive_differences_todo
    }

    public static void mapToPrimitiveTodo() {
        // snips-start: map_to_primitive_todo
        // TODO — document the mapToInt/mapToLong/mapToDouble conversion
        // matrix fully.
        // snips-end: map_to_primitive_todo
    }

    public static void anyError() {
        // snips-start: any_error
        // From a string
        final AnyError a = AnyError.from("something went wrong");
        final AnyError b = AnyError.from("code %d not found", 404);

        // From an exception
        final AnyError c = AnyError.from(new NumberFormatException("not a number"));

        // Join several errors into one
        final AnyError combined = a.join(b).join(c);
        final String msg = combined.message();

        // Turn any AnyError into an Err Result
        final Result<String> fail = a.fail();
        // snips-end: any_error
    }

    public static void anyErrorExceptTodo() {
        // snips-start: any_error_except_todo
        // TODO — confirm whether AnyError.except() should only be trusted
        // after a Try operation, and document the contract.
        // snips-end: any_error_except_todo
    }

    public static void resultsHelpers() {
        //snips-start: results_helpers
        final Result<Foo> nonNull = Results.requireNonNull(new Foo());

        // Ok(42)
        final IntResult parsedI = Results.parseInt("42");
        final LongResult parsedL = Results.parseLong("42");
        final Result<Integer> decodedI = Results.decodeInt("0x2A");
        final Result<Long> decodedL = Results.decodeLong("0x2A");

        // Err
        final IntResult badI = Results.parseInt("zzz");
        final LongResult badL = Results.parseLong("zzz");
        final Result<Integer> badDI = Results.decodeInt("zzz");
        final Result<Long> badDL = Results.decodeLong("zzz");
        //snips-end: results_helpers
    }

    public static void resultBatchFromParts() {
        // snips-start: result_batch_from_parts
        // Validate multiple path parts, collecting every error before deciding success.
        final String path = "/home/user/Documents/report.txt";
        final List<String> parts = List.of(path.split(
                Pattern.quote(File.separator)));

        final Result<Path> validated = parts.stream()
            .map(Result::ok)
            .map(Oks.failIf(
                    String::isBlank,
                    err -> AnyError.from("path part must not be blank")))
            .map(Oks.failIf(
                part -> part.contains(".."),
                err -> AnyError.from("path part cannot contain '..'")))
            // ...
            .collect(Result.batch())
            .reduce((errs, oks) -> errs.isEmpty()
                ? Result.ok(Path.of(path))
                : AnyError.joined(errs).fail());
        // snips-end: result_batch_from_parts
    }

    public static void resultBatchJobPattern() {
        // snips-start: result_batch_job_pattern
        // Collect a batch of file task results, then either report all errors or
        // keep the successful ones.
        List<Result<Foo>> tasks = List.of(
            Result.ok(new Foo("task-1")),
            Result.err("file not found"),
            Result.ok(new Foo("task-2")),
            Result.err("permission denied")
        );

        final ResultBatch<Foo> batch = tasks.stream().collect(Result.batch());

        if (batch.hasErrors()) {
            // Rollback: report all errors, drop the successes
            batch.oks().forEach(Foo::rollback);
            final Result<List<Foo>> aborted = AnyError
                    .joined(batch.errors()).fail();
        } else {
            // Commit: all succeeded, use the oks
            final Result<List<Foo>> committed = Result.ok(batch.oks());
        }

        // Partial success: keep both successes and errors separately
        batch.oks().forEach(Foo::doFooThings);
        batch.errors().forEach(err ->
                System.err.println("Error: " + err.message()));
        // snips-end: result_batch_job_pattern
    }

    public static void resultBatchHttpResponse() {
        // snips-start: result_batch_http_response
        final HttpResponse response = getClientFoos().stream()
                .map(Foo::updateState)
                .map(foo -> foo.isStateValid()
                        ? Result.ok(foo)
                        : Result.<Foo>err(foo.name() + " is invalid"))
                .collect(Result.batch())
                .fold()
                .reduce(errs -> new HttpResponse().code(400).entity(
                        "The following errors were encountered fulfilling "
                                + "your request.  Please fix and try again: "
                                + errs.message()),
                        oks -> new HttpResponse().code(200).entity(oks));
        // snips-end: result_batch_http_response
    }
}
