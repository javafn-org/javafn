package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LongOkTest {

    @Test
    @DisplayName("LongOk.map applies function to value")
    void testMap() {
        var res = LongResult.ok(123L);
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isOk());
        assertEquals(246L, mapped.expectOk());
    }

    @Test
    @DisplayName("LongOk.flatMap applies function and returns resulting LongResult")
    void testFlatMap() {
        var res = LongResult.ok(123L);
        var flatMapped = res.flatMap(v -> {
            if (v > 100L) return LongResult.ok(v + 1);
            return LongResult.err("too small");
        });
        assertTrue(flatMapped.isOk());
        assertEquals(124L, flatMapped.expectOk());
    }

    @Test
    @DisplayName("LongOk.flatMap returns Err if mapping function returns Err")
    void testFlatMapToErr() {
        var res = LongResult.ok(50L);
        var flatMapped = res.flatMap(v -> {
            if (v > 100L) return LongResult.ok(v + 1);
            return LongResult.err("too small");
        });
        assertTrue(flatMapped.isErr());
        assertEquals("too small", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("LongOk.mapErr returns this unmodified")
    void testMapErr() {
        var res = LongResult.ok(123L);
        var mapped = res.mapErr(e -> AnyError.from("new error"));
        assertSame(res, mapped);
    }

    @Test
    @DisplayName("LongOk.filter returns result of predicate")
    void testFilter() {
        var res1 = LongResult.ok(123L);
        assertTrue(res1.filter(v -> v > 100L));
        assertFalse(res1.filter(v -> v < 100L));
    }

    @Test
    @DisplayName("LongOk.isOkAnd returns result of predicate")
    void testIsOkAnd() {
        var res = LongResult.ok(123L);
        assertTrue(res.isOkAnd(v -> v > 100L));
        assertFalse(res.isOkAnd(v -> v < 100L));
    }

    @Test
    @DisplayName("LongOk.failIf converts Ok to Err if predicate is true")
    void testFailIf() {
        var res = LongResult.ok(123L);
        var failed = res.failIf(v -> v > 100L, v -> AnyError.from("too large"));
        assertTrue(failed.isErr());
        assertEquals("too large", failed.expectErr().message());

        var notFailed = res.failIf(v -> v < 100L, v -> AnyError.from("too small"));
        assertSame(res, notFailed);
    }

    @Test
    @DisplayName("LongOk.ifOk executes consumer")
    void testIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = LongResult.ok(123L);
        res.ifOk(v -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifOk(v -> {}));
    }

    @Test
    @DisplayName("LongOk.reduce delegates to fnOk")
    void testReduce() {
        var res = LongResult.ok(123L);
        var reduced = res.reduce(e -> "err", v -> "ok-" + v);
        assertEquals("ok-123", reduced);
    }

    @Test
    @DisplayName("LongOk.consume delegates to fnOk")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = LongResult.ok(123L);
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, okCount.get());
        assertEquals(0, errCount.get());
    }

    @Test
    @DisplayName("LongOk.opt returns OptionalLong of value")
    void testOpt() {
        var res = LongResult.ok(123L);
        assertTrue(res.opt().isPresent());
        assertEquals(123L, res.opt().getAsLong());
    }

    @Test
    @DisplayName("LongOk.mapToX creates specialized results")
    void testMapToX() {
        var res = LongResult.ok(123L);
        
        var doubleRes = res.mapToDouble(v -> (double)v * 2);
        assertTrue(doubleRes.isOk());
        assertEquals(246.0, doubleRes.expectOk());

        var objRes = res.mapToObj(v -> "val-" + v);
        assertTrue(objRes.isOk());
        assertEquals("val-123", objRes.expectOk());

        var boxedRes = res.boxed();
        assertTrue(boxedRes.isOk());
        assertEquals(123L, boxedRes.expectOk());
    }
}
