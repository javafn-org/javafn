package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class IntOkTest {

    @Test
    @DisplayName("IntOk.map applies function to value")
    void testMap() {
        var res = IntResult.ok(123);
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isOk());
        assertEquals(246, mapped.expectOk());
    }

    @Test
    @DisplayName("IntOk.flatMap applies function and returns resulting IntResult")
    void testFlatMap() {
        var res = IntResult.ok(123);
        var flatMapped = res.flatMap(v -> {
            if (v > 100) return IntResult.ok(v + 1);
            return IntResult.err("too small");
        });
        assertTrue(flatMapped.isOk());
        assertEquals(124, flatMapped.expectOk());
    }

    @Test
    @DisplayName("IntOk.flatMap returns Err if mapping function returns Err")
    void testFlatMapToErr() {
        var res = IntResult.ok(50);
        var flatMapped = res.flatMap(v -> {
            if (v > 100) return IntResult.ok(v + 1);
            return IntResult.err("too small");
        });
        assertTrue(flatMapped.isErr());
        assertEquals("too small", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("IntOk.mapErr returns this unmodified")
    void testMapErr() {
        var res = IntResult.ok(123);
        var mapped = res.mapErr(e -> AnyError.from("new error"));
        assertSame(res, mapped);
    }

    @Test
    @DisplayName("IntOk.filter returns result of predicate")
    void testFilter() {
        var res1 = IntResult.ok(123);
        assertTrue(res1.filter(v -> v > 100));
        assertFalse(res1.filter(v -> v < 100));
    }

    @Test
    @DisplayName("IntOk.isOkAnd returns result of predicate")
    void testIsOkAnd() {
        var res = IntResult.ok(123);
        assertTrue(res.isOkAnd(v -> v > 100));
        assertFalse(res.isOkAnd(v -> v < 100));
    }

    @Test
    @DisplayName("IntOk.failIf converts Ok to Err if predicate is true")
    void testFailIf() {
        var res = IntResult.ok(123);
        var failed = res.failIf(v -> v > 100, v -> AnyError.from("too large"));
        assertTrue(failed.isErr());
        assertEquals("too large", failed.expectErr().message());

        var notFailed = res.failIf(v -> v < 100, v -> AnyError.from("too small"));
        assertSame(res, notFailed);
    }

    @Test
    @DisplayName("IntOk.ifOk executes consumer")
    void testIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = IntResult.ok(123);
        res.ifOk(v -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifOk(v -> {}));
    }

    @Test
    @DisplayName("IntOk.reduce delegates to fnOk")
    void testReduce() {
        var res = IntResult.ok(123);
        var reduced = res.reduce(e -> "err", v -> "ok-" + v);
        assertEquals("ok-123", reduced);
    }

    @Test
    @DisplayName("IntOk.consume delegates to fnOk")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = IntResult.ok(123);
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, okCount.get());
        assertEquals(0, errCount.get());
    }

    @Test
    @DisplayName("IntOk.opt returns OptionalInt of value")
    void testOpt() {
        var res = IntResult.ok(123);
        assertTrue(res.opt().isPresent());
        assertEquals(123, res.opt().getAsInt());
    }

    @Test
    @DisplayName("IntOk.mapToX creates specialized results")
    void testMapToX() {
        var res = IntResult.ok(123);
        
        var longRes = res.mapToLong(v -> (long)v * 2);
        assertTrue(longRes.isOk());
        assertEquals(246L, longRes.expectOk());

        var doubleRes = res.mapToDouble(v -> (double)v * 2);
        assertTrue(doubleRes.isOk());
        assertEquals(246.0, doubleRes.expectOk());

        var objRes = res.mapToObj(v -> "val-" + v);
        assertTrue(objRes.isOk());
        assertEquals("val-123", objRes.expectOk());

        var boxedRes = res.boxed();
        assertTrue(boxedRes.isOk());
        assertEquals(123, boxedRes.expectOk());
    }
}
