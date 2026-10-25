package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class IntErrTest {

    @Test
    @DisplayName("IntErr.map returns this unmodified (coerced type)")
    void testMap() {
        var res = IntResult.err("fail");
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isErr());
        assertEquals("fail", mapped.expectErr().message());
    }

    @Test
    @DisplayName("IntErr.flatMap returns this unmodified (coerced type)")
    void testFlatMap() {
        var res = IntResult.err("fail");
        var flatMapped = res.flatMap(v -> IntResult.ok(v + 1));
        assertTrue(flatMapped.isErr());
        assertEquals("fail", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("IntErr.mapErr applies function to error value")
    void testMapErr() {
        var res = IntResult.err("fail");
        var mapped = res.mapErr(e -> AnyError.from("mapped error"));
        assertTrue(mapped.isErr());
        assertEquals("mapped error", mapped.expectErr().message());
    }

    @Test
    @DisplayName("IntErr.flatRecover applies function to error value")
    void testFlatRecover() {
        var res = IntResult.err("fail");
        var recovered = res.flatRecover(e -> IntResult.ok(123));
        assertTrue(recovered.isOk());
        assertEquals(123, recovered.expectOk());
    }

    @Test
    @DisplayName("IntErr.flatRecover returns Err if mapping function returns Err")
    void testFlatRecoverToErr() {
        var res = IntResult.err("fail");
        var recovered = res.flatRecover(e -> IntResult.err("still failing"));
        assertTrue(recovered.isErr());
        assertEquals("still failing", recovered.expectErr().message());
    }

    @Test
    @DisplayName("IntErr.filterErr returns result of predicate")
    void testFilterErr() {
        var res = IntResult.err("fail");
        assertTrue(res.filterErr(e -> e.message().contains("fail")));
        assertFalse(res.filterErr(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("IntErr.isErrAnd returns result of predicate")
    void testIsErrAnd() {
        var res = IntResult.err("fail");
        assertTrue(res.isErrAnd(e -> e.message().contains("fail")));
        assertFalse(res.isErrAnd(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("IntErr.recoverIf converts Err to Ok if predicate is true")
    void testRecoverIf() {
        var res = IntResult.err("fail");
        var recovered = res.recoverIf(e -> e.message().contains("fail"), e -> 123);
        assertTrue(recovered.isOk());
        assertEquals(123, recovered.expectOk());

        var notRecovered = res.recoverIf(e -> e.message().contains("success"), e -> 123);
        assertSame(res, notRecovered);
    }

    @Test
    @DisplayName("IntErr.ifErr executes consumer")
    void testIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = IntResult.err("fail");
        res.ifErr(e -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifErr(e -> {}));
    }

    @Test
    @DisplayName("IntErr.reduce delegates to fnErr")
    void testReduce() {
        var res = IntResult.err("fail");
        var reduced = res.reduce(e -> "err-mapped", v -> "ok-mapped");
        assertEquals("err-mapped", reduced);
    }

    @Test
    @DisplayName("IntErr.consume delegates to fnErr")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = IntResult.err("fail");
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, errCount.get());
        assertEquals(0, okCount.get());
    }

    @Test
    @DisplayName("IntErr.into coerces OK type")
    void testInto() {
        var res = IntResult.err("fail");
        if (res instanceof IntResult.Err asErr) {
            Result<String> coerced = asErr.into();
            assertTrue(coerced.isErr());
            assertEquals("fail", coerced.expectErr().message());
        } else {
            fail();
        }
    }

    @Test
    @DisplayName("IntErr.intoLong/Double creates specialized results")
    void testIntoSpecialized() {
        var res = new IntResult.Err(AnyError.from("fail"));
        
        assertTrue(res.into().isErr());
        assertEquals("fail", res.into().expectErr().message());

        assertTrue(res.intoLong().isErr());
        assertEquals("fail", res.intoLong().expectErr().message());

        assertTrue(res.intoDouble().isErr());
        assertEquals("fail", res.intoDouble().expectErr().message());
    }
}
