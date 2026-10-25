package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class OkTest {

    @Test
    @DisplayName("Ok.map applies function to value")
    void testMap() {
        var res = Result.ok("123");
        var mapped = res.map(Integer::parseInt);
        assertTrue(mapped.isOk());
        assertEquals(123, mapped.expectOk());
    }

    @Test
    @DisplayName("Ok.flatMap applies function and returns resulting Result")
    void testFlatMap() {
        var res = Result.ok("123");
        var flatMapped = res.flatMap(v -> {
            try {
                return Result.ok(Integer.parseInt(v));
            } catch (NumberFormatException e) {
                return Result.err(e);
            }
        });
        assertTrue(flatMapped.isOk());
        assertEquals(123, flatMapped.expectOk());
    }

    @Test
    @DisplayName("Ok.flatMap returns Err if mapping function returns Err")
    void testFlatMapToErr() {
        var res = Result.ok("abc");
        var flatMapped = res.flatMap(v -> Result.err("invalid"));
        assertTrue(flatMapped.isErr());
        assertEquals("invalid", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("Ok.mapErr returns this unmodified")
    void testMapErr() {
        var res = Result.ok("success");
        var mapped = res.mapErr(e -> AnyError.from("new error"));
        assertSame(res, mapped);
    }

    @Test
    @DisplayName("Ok.filter returns result of predicate")
    void testFilter() {
        var res1 = Result.ok("hello");
        assertTrue(res1.filter(s -> s.startsWith("h")));
        assertFalse(res1.filter(s -> s.startsWith("z")));

        var res2 = Result.ok(null);
        assertFalse(res2.filter(Objects::nonNull));
    }

    @Test
    @DisplayName("Ok.isOkAnd returns result of predicate")
    void testIsOkAnd() {
        var res = Result.ok("hello");
        assertTrue(res.isOkAnd(s -> s.length() > 2));
        assertFalse(res.isOkAnd(s -> s.length() > 10));
    }

    @Test
    @DisplayName("Ok.failIf converts Ok to Err if predicate is true")
    void testFailIf() {
        var res = Result.ok("fail me");
        var failed = res.failIf(s -> s.contains("fail"), s -> AnyError.from("failed!"));
        assertTrue(failed.isErr());
        assertEquals("failed!", failed.expectErr().message());

        var notFailed = res.failIf(s -> s.contains("success"), s -> AnyError.from("failed!"));
        assertSame(res, notFailed);
    }

    @Test
    @DisplayName("Ok.ifOk executes consumer")
    void testIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = Result.ok("success");
        res.ifOk(v -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifOk(v -> {}));
    }

    @Test
    @DisplayName("Ok.reduce delegates to fnOk")
    void testReduce() {
        var res = Result.ok("value");
        var reduced = res.reduce(e -> "err", v -> "ok-" + v);
        assertEquals("ok-value", reduced);
    }

    @Test
    @DisplayName("Ok.consume delegates to fnOk")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = Result.ok("value");
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, okCount.get());
        assertEquals(0, errCount.get());
    }

    @Test
    @DisplayName("Ok.mapToX creates specialized typed results")
    void testMapToX() {
        var res = Result.ok("123");
        
        var intRes = res.mapToInt(Integer::parseInt);
        assertTrue(intRes.isOk());
        assertEquals(123, intRes.expectOk());

        var longRes = res.mapToLong(Long::parseLong);
        assertTrue(longRes.isOk());
        assertEquals(123L, longRes.expectOk());

        var doubleRes = res.mapToDouble(Double::parseDouble);
        assertTrue(doubleRes.isOk());
        assertEquals(123.0, doubleRes.expectOk());
    }

    @Test
    @DisplayName("Ok.opt returns Optional of value")
    void testOpt() {
        var res1 = Result.ok("value");
        assertEquals("value", res1.opt().get());

        var res2 = Result.ok((String)null);
        assertTrue(res2.opt().isEmpty());
    }
}
