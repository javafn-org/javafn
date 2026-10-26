package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DoubleErrTest {

    @Test
    @DisplayName("DoubleErr.map returns this unmodified (coerced type)")
    void testMap() {
        var res = DoubleResult.err("fail");
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isErr());
        assertEquals("fail", mapped.expectErr().message());
    }

    @Test
    @DisplayName("DoubleErr.flatMap returns this unmodified (coerced type)")
    void testFlatMap() {
        var res = DoubleResult.err("fail");
        var flatMapped = res.flatMap(v -> DoubleResult.ok(v + 1.0));
        assertTrue(flatMapped.isErr());
        assertEquals("fail", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("DoubleErr.mapErr applies function to error value")
    void testMapErr() {
        var res = DoubleResult.err("fail");
        var mapped = res.mapErr(e -> AnyError.from("mapped error"));
        assertTrue(mapped.isErr());
        assertEquals("mapped error", mapped.expectErr().message());
    }

    @Test
    @DisplayName("DoubleErr.flatRecover applies function to error value")
    void testFlatRecover() {
        var res = DoubleResult.err("fail");
        var recovered = res.flatRecover(e -> DoubleResult.ok(123.4));
        assertTrue(recovered.isOk());
        assertEquals(123.4, recovered.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleErr.flatRecover returns Err if mapping function returns Err")
    void testFlatRecoverToErr() {
        var res = DoubleResult.err("fail");
        var recovered = res.flatRecover(e -> DoubleResult.err("still failing"));
        assertTrue(recovered.isErr());
        assertEquals("still failing", recovered.expectErr().message());
    }

    @Test
    @DisplayName("DoubleErr.filterErr returns result of predicate")
    void testFilterErr() {
        var res = DoubleResult.err("fail");
        assertTrue(res.filterErr(e -> e.message().contains("fail")));
        assertFalse(res.filterErr(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("DoubleErr.isErrAnd returns result of predicate")
    void testIsErrAnd() {
        var res = DoubleResult.err("fail");
        assertTrue(res.isErrAnd(e -> e.message().contains("fail")));
        assertFalse(res.isErrAnd(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("DoubleErr.recoverIf converts Err to Ok if predicate is true")
    void testRecoverIf() {
        var res = DoubleResult.err("fail");
        var recovered = res.recoverIf(e -> e.message().contains("fail"), e -> 123.4);
        assertTrue(recovered.isOk());
        assertEquals(123.4, recovered.expectOk(), 0.0001);

        var notRecovered = res.recoverIf(e -> e.message().contains("success"), e -> 123.4);
        assertSame(res, notRecovered);
    }

    @Test
    @DisplayName("DoubleErr.ifErr executes consumer")
    void testIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = DoubleResult.err("fail");
        res.ifErr(e -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifErr(e -> {}));
    }

    @Test
    @DisplayName("DoubleErr.reduce delegates to fnErr")
    void testReduce() {
        var res = DoubleResult.err("fail");
        var reduced = res.reduce(e -> "err-mapped", v -> "ok-mapped");
        assertEquals("err-mapped", reduced);
    }

    @Test
    @DisplayName("DoubleErr.consume delegates to fnErr")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = DoubleResult.err("fail");
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, errCount.get());
        assertEquals(0, okCount.get());
    }

    @Test
    @DisplayName("DoubleErr.into coerces OK type")
    void testInto() {
        var res = DoubleResult.err("fail").res();
        if (res instanceof DoubleResult.Err asErr) {
            Result<String> coerced = asErr.into();
            assertTrue(coerced.isErr());
            assertEquals("fail", coerced.expectErr().message());
        } else {
            fail();
        }
    }

    @Test
    @DisplayName("DoubleErr.intoLong/Int creates specialized results")
    void testIntoSpecialized() {
        var res = new DoubleResult.Err(AnyError.from("fail"));
        
        assertTrue(res.into().isErr());
        assertEquals("fail", res.into().expectErr().message());

        assertTrue(res.intoLong().isErr());
        assertEquals("fail", res.intoLong().expectErr().message());

        assertTrue(res.intoInt().isErr());
        assertEquals("fail", res.intoInt().expectErr().message());
    }
}
