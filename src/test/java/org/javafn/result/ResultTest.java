package org.javafn.result;

import org.javafn.result.Result.Err;
import org.javafn.result.Result.Ok;
import org.junit.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResultTest {

    @Test public void testResultErr() {
        final UUID err = UUID.randomUUID();
        final Result<UUID, Void> res = Result.err(err);
        assertTrue("Expected the result's isErr to return true", res.isErr());
        assertFalse("Expected the result's isOk to return false", res.isOk());
        assertEquals("Expected the error stored in the result to be returned",
                err, res.asErr().get());
        assertFalse("Expected the ok as optional to be empty", res.asOk().opt().isPresent());
    }
    @Test public void testResultOk() {
        final UUID ok = UUID.randomUUID();
        final Result<Void, UUID> res = Result.ok(ok);
        assertTrue("Expected the result's isOk to return true", res.isOk());
        assertFalse("Expected the result's isErr to return false", res.isErr());
        assertEquals("Expected the ok value stored in the result to be returned",
                ok, res.asOk().get());
        assertFalse("Expected the err as optional to be empty", res.asErr().opt().isPresent());
    }

    @Test public void testErrEquality() {
        final Result<UUID, Void> err = Result.err(UUID.randomUUID());
        assertEquals("An error projection should be equal to the result itself", err, err.asErr());
        assertEquals("An ok projection should be equal to the result itself, even if the result is an err", err, err.asOk());
        assertEquals("An ok projection and an err projection from the same result should be equal", err.asErr(), err.asOk());
    }
    @Test public void testOkEquality() {
        final Result<Void, UUID> ok = Result.ok(UUID.randomUUID());
        assertEquals("An ok projection should be equal to the result itself", ok, ok.asOk());
        assertEquals("An error projection should be equal to the result itself, even if the result is an ok", ok, ok.asErr());
        assertEquals("An ok projection and an err projection from the same result should be equal", ok.asErr(), ok.asOk());
    }

    @Test(expected = NoSuchElementException.class) public void testErrGetOkThrows() {
        final Result<UUID, Void> res = Result.err(UUID.randomUUID());
        res.asOk().get();
    }

    @Test(expected = NoSuchElementException.class) public void testOkGetErrThrows() {
        final Result<Void, UUID> res = Result.ok(UUID.randomUUID());
        res.asErr().get();
    }

    @Test public void testErrMapGet() {
        final UUID err = UUID.randomUUID();
        final Result<UUID, Void> res = Result.err(err);
        assertEquals("Epected the error value to be mapped and retrieved",
                err.toString(), res.asErr().map(UUID::toString).asErr().get());
        res.asOk().map(ok -> { fail("Expected the map function to not be called for an ok projection of an err."); return null; });
    }
    @Test public void testOkMapGet() {
        final UUID ok = UUID.randomUUID();
        final Result<Void, UUID> res = Result.ok(ok);
        assertEquals("Epected the ok value to be mapped and retrieved",
                ok.toString(), res.asOk().map(UUID::toString).asOk().get());
        res.asErr().map(err -> { fail("Expected the map function to not be called for an err projection of an ok."); return null;});
    }
    @Test public void testErrPeek() {
        final UUID err = UUID.randomUUID();
        final Result<UUID, Void> res = Result.err(err);
        res.asErr().peek(uuid -> assertEquals("Expected peek to be called with the wrapped err", err, uuid));
        res.asOk().peek(expectedNothing -> fail("Expected the peek function to not be called for an ok projection of an err."));
    }

    @Test public void testOkPeek() {
        final UUID ok = UUID.randomUUID();
        final Result<Void, UUID> res = Result.ok(ok);
        res.asOk().peek(uuid -> assertEquals("Expected peek to be called with the wrapped ok", ok, uuid));
        res.asErr().peek(expectedNothing -> fail("Expected the peek function to not be called for an err projection of an ok."));
    }

    @Test public void testSwap() {
        final UUID uuid = UUID.randomUUID();
        final Result<UUID, Void> res = Result.err(uuid);
        final Result<Void, UUID> swapped = res.swap();
        assertTrue(swapped.isOk());
        assertEquals("Expected swapped result to return ok value",
                uuid, swapped.asOk().get());
        final Result<UUID, Void> doubleSwapped = swapped.swap();
        assertEquals("Expected the original and doubleswapped result to be the same",
                res, doubleSwapped);
    }
    @Test public void testSwapErrIf() {
        final String mappedValue = "Forty-Two!";
        final Result<Integer, String> res = Result.err(42);
        final Result<Integer, String> swapped = res.asErr().filterMap(i -> i == 42, i -> mappedValue);
        assertTrue("Expected the result to be swapped into an ok.", swapped.isOk());
        assertEquals("Expected the result to be the mapped string", mappedValue, swapped.asOk().get());
        final Result<Integer, String> notSwapped = res.asErr().filterMap(i -> i == 0, i -> mappedValue);
        assertFalse("Expected the result to not be swapped into an ok.", notSwapped.isOk());
        assertEquals("Expected the not swapped value to be equal to the original.", res, notSwapped);
    }
    @Test public void testSwapOkIf() {
        final Integer mappedValue = 42;
        final Result<Integer, String> res = Result.ok("Forty-Two!");
        final Result<Integer, String> swapped = res.asOk().filterMap("Forty-Two!"::equals, i -> mappedValue);
        assertTrue("Expected the result to be swapped into an err.", swapped.isErr());
        assertEquals("Expected the result to be the mapped integer", mappedValue, swapped.asErr().get());
        final Result<Integer, String> notSwapped = res.asOk().filterMap("Not 42"::equals, i -> mappedValue);
        assertFalse("Expected the result to not be swapped into an err.", notSwapped.isErr());
        assertEquals("Expected the not swapped value to be equal to the original.", res, notSwapped);
    }
    @Test public void testFoldErr() {
        assertEquals("Expected orElseMap on err to return wrapped value",
                Integer.valueOf(42),
                Result.err(42).asErr().orElseMap(s -> -42));
        assertEquals("Expected orElseMap on ok to return mapped value",
                -42,
                Result.ok("Forty-Two!").asErr().orElseMap(s -> -42));
    }
    @Test public void testFoldOk() {
        assertEquals("Expected orElseMap on ok to return wrapped value",
                "Forty-Two!",
                Result.ok("Forty-Two!").asOk().orElseMap(i -> "Negative Forty-Two"));
        assertEquals("Expected orElseMap on err to return mapped value",
                "Negative Forty-Two",
                Result.err(42).asOk().orElseMap(i -> "Negative Forty-Two"));
    }

    @Test public void testTryGetOnThrows() {
        final Exception ex = new Exception("Intentionally thrown; expected to be caught");
        class ThisClassThrows {
            public String barf() throws Exception { throw ex; }
        }
        final Result<Exception, String> err = Try.get(() -> new ThisClassThrows().barf());
        assertTrue("Expected the result to be of type err.", err.isErr());
        assertEquals("Expected the returned exception to be the one caught", ex, err.asErr().get());
    }

    @Test public void testTryGetOnNoThrows() {
        final String s = "Hello World";
        class ThisClassThrows {
            public String barf() throws Exception { return s; }
        }
        final Result<Exception, String> ok = Try.get(() -> new ThisClassThrows().barf());
        assertTrue("Expected the result to be of type ok", ok.isOk());
        assertEquals("Expected the returned value to be the one returned by the function.",
                s, ok.asOk().get());
    }

    @Test public void testTryGetWithCustomException() {
        class CustomException extends Exception { }

        final Result<Exception, Void> err = Try.get(() -> { throw new CustomException(); });
        assertTrue("Expected the result to be of type err", err.isErr());
        assertEquals("Expected the returned exception to be the correct type",
                CustomException.class, err.asErr().get().getClass());
    }

    @Test public void testTryRunOnThrows() {
        final Exception ex = new Exception("Intentionally thrown; expected to be caught");
        class ThisClassThrows {
            public void barf() throws Exception { throw ex; }
        }
        final VoidResult<Exception> err = Try.run(() -> new ThisClassThrows().barf());
        assertTrue("Expected the result to be of type err.", err.isErr());
        assertEquals("Expected the returned exception to be the one caught", ex, err.asErr().get());
    }

    @Test public void testTryRunOnNoThrows() {
        final String s = "Hello World";
        class ThisClassThrows {
            public void barf() throws Exception { /* no op */ }
        }
        final VoidResult<Exception> ok = Try.run(() -> new ThisClassThrows().barf());
        assertTrue("Expected the result to be of type ok", ok.isOk());
    }

    @Test public void testTryRunWithCustomException() {
        class CustomException extends Exception { }

        final VoidResult<Exception> err = Try.run(() -> { throw new CustomException(); });
        assertTrue("Expected the result to be of type err", err.isErr());
        assertEquals("Expected the returned exception to be the correct type",
                CustomException.class, err.asErr().get().getClass());
    }

    @Test public void testTryMapOnThrows() {
        final Exception ex = new Exception("Intentionally thrown; expected to be caught");
        class ThisClassThrows {
            public String barf(Integer i) throws Exception { throw ex; }
        }
        final Function<Integer, Result<Exception, String>> fn = Try.Map(j -> new ThisClassThrows().barf(j));
        final Result<Exception, String> err = fn.apply(42);
        assertTrue("Expected the result to be of type err.", err.isErr());
        assertEquals("Expected the returned exception to be the one caught", ex, err.asErr().get());
    }

    @Test public void testTryMapOnNoThrows() {
        class ThisClassThrows {
            public String barf(Integer i) throws Exception { return String.valueOf(i); }
        }
        final Function<Integer, Result<Exception, String>> fn = Try.Map(j -> new ThisClassThrows().barf(j));
        final Result<Exception, String> ok = fn.apply(42);
        assertTrue("Expected the result to be of type ok.", ok.isOk());
        assertEquals("Expected the returned String to be the string value of the argument",
                "42", ok.asOk().get());
    }

    @Test public void testTryMapWithCustomException() {
        class CustomException extends Exception { }
        final Function<Integer, Result<Exception, String>> fn = Try.Map(j -> { throw new CustomException(); } );
        final Result<Exception, String> err = fn.apply(42);
        assertTrue("Expected the result to be of type err.", err.isErr());
        assertEquals("Expected the returned exception to be the correct type",
                CustomException.class, err.asErr().get().getClass());
    }

    @Test public void testReduce() {
        final Function<Integer, String> intToString = i -> Integer.toString(i);
        final Function<Boolean, String> boolToString = b -> Boolean.toString(b);
        final Result<Integer, Boolean> err = Result.err(42);
        final Result<Integer, Boolean> ok = Result.ok(true);
        assertEquals("Expected the reduced err to be a string",
                "42", err.reduce(intToString, boolToString));
        assertEquals("Expected the reduced ok to be a string",
                "true", ok.reduce(intToString, boolToString));
    }

    @Test public void testMap() {
        final Function<Integer, Long> intMapper = i -> i * 2L;
        final Function<String, Boolean> stringMapper = String::isEmpty;;
        final Result<Integer, String> err = Result.err(42);
        final Result<Integer, String> ok = Result.ok("");
        assertEquals("Expected the err mapping function to be applied",
                Result.err(84L), err.mapResult(intMapper, stringMapper));
        assertEquals("Expected the ok mapping function to be applied",
                Result.ok(true), ok.mapResult(intMapper, stringMapper));
    }

    @Test public void testOkOrElse() {
        final String errorString = "The optional was empty";
        final Result<String, Integer> err = Result.okOrElse(Optional.empty(), () -> errorString);
        final Result<String, Integer> ok = Result.okOrElse(Optional.of(42), () -> errorString);

        assertTrue("Expecting an err for the empty optional", err.isErr());
        assertEquals("Expecting the error message to be set", errorString, err.asErr().get());
        assertTrue("Expecting an ok for the present optional", ok.isOk());
        assertEquals("Expecting the ok value to be set", Integer.valueOf(42), ok.asOk().get());
    }

    @Test public void testWrapErr() {
        final int start = 42;
        final int end = 255;
        final List<Integer> values = IntStream.range(start, end).boxed().collect(Collectors.toList());
        final List<Result<Integer, UUID>> results = Err.Wrap(values);
        for (int i = 0; i < end - start; i++) {
            final Result<Integer, UUID> e = results.get(i);
            assertTrue("Expected a result to be of type Err", e.isErr());
            assertEquals("Expected the err value to be the right integer.",
                    i + start, (int) e.asErr().get());
        }
    }

    @Test public void testWrapOk() {
        final int count = 100;
        final List<UUID> values = IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        final List<Result<Integer, UUID>> results = Ok.Wrap(values);
        for (int i = 0; i < count; i++) {
            final Result<Integer, UUID> e = results.get(i);
            assertTrue("Expected a result to be of type Ok", e.isOk());
            assertEquals("Expected the ok value to be the specific uuid from the source.",
                    values.get(i), e.asOk().get());
        }
    }
}
