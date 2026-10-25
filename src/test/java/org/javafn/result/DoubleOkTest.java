package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DoubleOkTest {

    @Test
    @DisplayName("DoubleOk.map applies function to value")
    void testMap() {
        var res = DoubleResult.ok(123.4);
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isOk());
        assertEquals(246.8, mapped.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleOk.flatMap applies function and returns resulting DoubleResult")
    void testFlatMap() {
        var res = DoubleResult.ok(123.4);
        var flatMapped = res.flatMap(v -> {
            if (v > 100.0) return DoubleResult.ok(v + 1.0);
            return DoubleResult.err("too small");
        });
        assertTrue(flatMapped.isOk());
        assertEquals(124.4, flatMapped.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleOk.flatMap returns Err if mapping function returns Err")
    void testFlatMapToErr() {
        var res = DoubleResult.ok(50.0);
        var flatMapped = res.flatMap(v -> {
            if (v > 100.0) return DoubleResult.ok(v + 1.0);
            return DoubleResult.err("too small");
        });
        assertTrue(flatMapped.isErr());
        assertEquals("too small", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("DoubleOk.mapErr returns this unmodified")
    void testMapErr() {
        var res = DoubleResult.ok(123.4);
        var mapped = res.mapErr(e -> AnyError.from("new error"));
        assertSame(res, mapped);
    }

    @Test
    @DisplayName("DoubleOk.filter returns result of predicate")
    void testFilter() {
        var res1 = DoubleResult.ok(123.4);
        assertTrue(res1.filter(v -> v > 100.0));
        assertFalse(res1.filter(v -> v < 100.0));
    }

    @Test
    @DisplayName("DoubleOk.isOkAnd returns result of predicate")
    void testIsOkAnd() {
        var res = DoubleResult.ok(123.4);
        assertTrue(res.isOkAnd(v -> v > 100.0));
        assertFalse(res.isOkAnd(v -> v < 100.0));
    }

    @Test
    @DisplayName("DoubleOk.failIf converts Ok to Err if predicate is true")
    void testFailIf() {
        var res = DoubleResult.ok(123.4);
        var failed = res.failIf(v -> v > 100.0, v -> AnyError.from("too large"));
        assertTrue(failed.isErr());
        assertEquals("too large", failed.expectErr().message());

        var notFailed = res.failIf(v -> v < 100.0, v -> AnyError.from("too small"));
        assertSame(res, notFailed);
    }

    @Test
    @DisplayName("DoubleOk.ifOk executes consumer")
    void testIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = DoubleResult.ok(123.4);
        res.ifOk(v -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifOk(v -> {}));
    }

    @Test
    @DisplayName("DoubleOk.reduce delegates to fnOk")
    void testReduce() {
        var res = DoubleResult.ok(123.4);
        var reduced = res.reduce(e -> "err", v -> "ok-" + v);
        assertEquals("ok-123.4", reduced);
    }

    @Test
    @DisplayName("DoubleOk.consume delegates to fnOk")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = DoubleResult.ok(123.4);
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, okCount.get());
        assertEquals(0, errCount.get());
    }

    @Test
    @DisplayName("DoubleOk.opt returns OptionalDouble of value")
    void testOpt() {
        var res = DoubleResult.ok(123.4);
        assertTrue(res.opt().isPresent());
        assertEquals(123.4, res.opt().getAsDouble(), 0.0001);
    }

    @Test
    @DisplayName("DoubleOk.mapToX creates specialized results")
    void testMapToX() {
        var res = DoubleResult.ok(123.4);
        
        var longRes = res.mapToLong(v -> (long)(v * 2));
        assertTrue(longRes.isOk());
        assertEquals(246L, longRes.expectOk());

        var intRes = res.mapToInt(v -> (int)(v * 2));
        assertTrue(intRes.isOk());
        assertEquals(246, intRes.expectOk());

        var objRes = res.mapToObj(v -> "val-" + v);
        assertTrue(objRes.isOk());
        assertEquals("val-123.4", objRes.expectOk());

        var boxedRes = res.boxed();
        assertTrue(boxedRes.isOk());
        assertEquals(123.4, boxedRes.expectOk());
    }
}
