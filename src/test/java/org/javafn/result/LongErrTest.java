package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class LongErrTest {

    @Test
    @DisplayName("LongErr.map returns this unmodified (coerced type)")
    void testMap() {
        var res = LongResult.err("fail");
        var mapped = res.map(v -> v * 2);
        assertTrue(mapped.isErr());
        assertEquals("fail", mapped.expectErr().message());
    }

    @Test
    @DisplayName("LongErr.flatMap returns this unmodified (coerced type)")
    void testFlatMap() {
        var res = LongResult.err("fail");
        var flatMapped = res.flatMap(v -> LongResult.ok(v + 1));
        assertTrue(flatMapped.isErr());
        assertEquals("fail", flatMapped.expectErr().message());
    }

    @Test
    @DisplayName("LongErr.mapErr applies function to error value")
    void testMapErr() {
        var res = LongResult.err("fail");
        var mapped = res.mapErr(e -> AnyError.from("mapped error"));
        assertTrue(mapped.isErr());
        assertEquals("mapped error", mapped.expectErr().message());
    }

    @Test
    @DisplayName("LongErr.flatRecover applies function to error value")
    void testFlatRecover() {
        var res = LongResult.err("fail");
        var recovered = res.flatRecover(e -> LongResult.ok(123L));
        assertTrue(recovered.isOk());
        assertEquals(123L, recovered.expectOk());
    }

    @Test
    @DisplayName("LongErr.flatRecover returns Err if mapping function returns Err")
    void testFlatRecoverToErr() {
        var res = LongResult.err("fail");
        var recovered = res.flatRecover(e -> LongResult.err("still failing"));
        assertTrue(recovered.isErr());
        assertEquals("still failing", recovered.expectErr().message());
    }

    @Test
    @DisplayName("LongErr.filterErr returns result of predicate")
    void testFilterErr() {
        var res = LongResult.err("fail");
        assertTrue(res.filterErr(e -> e.message().contains("fail")));
        assertFalse(res.filterErr(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("LongErr.isErrAnd returns result of predicate")
    void testIsErrAnd() {
        var res = LongResult.err("fail");
        assertTrue(res.isErrAnd(e -> e.message().contains("fail")));
        assertFalse(res.isErrAnd(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("LongErr.recoverIf converts Err to Ok if predicate is true")
    void testRecoverIf() {
        var res = LongResult.err("fail");
        var recovered = res.recoverIf(e -> e.message().contains("fail"), e -> 123L);
        assertTrue(recovered.isOk());
        assertEquals(123L, recovered.expectOk());

        var notRecovered = res.recoverIf(e -> e.message().contains("success"), e -> 123L);
        assertSame(res, notRecovered);
    }

    @Test
    @DisplayName("LongErr.ifErr executes consumer")
    void testIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = LongResult.err("fail");
        res.ifErr(e -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifErr(e -> {}));
    }

    @Test
    @DisplayName("LongErr.reduce delegates to fnErr")
    void testReduce() {
        var res = LongResult.err("fail");
        var reduced = res.reduce(e -> "err-mapped", v -> "ok-mapped");
        assertEquals("err-mapped", reduced);
    }

    @Test
    @DisplayName("LongErr.consume delegates to fnErr")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = LongResult.err("fail");
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, errCount.get());
        assertEquals(0, okCount.get());
    }

    @Test
    @DisplayName("LongErr.into coerces OK type")
    void testInto() {
        var res = LongResult.err("fail").res();
        if (res instanceof LongResult.Err asErr) {
            Result<String> coerced = asErr.into();
            assertTrue(coerced.isErr());
            assertEquals("fail", coerced.expectErr().message());
        } else {
            fail();
        }
    }

    @Test
    @DisplayName("LongErr.intoDouble creates specialized results")
    void testIntoSpecialized() {
        var res = new LongResult.Err(AnyError.from("fail"));

        assertTrue(res.into().isErr());
        assertEquals("fail", res.into().expectErr().message());
        
        assertTrue(res.intoInt().isErr());
        assertEquals("fail", res.intoInt().expectErr().message());

        assertTrue(res.intoDouble().isErr());
        assertEquals("fail", res.intoDouble().expectErr().message());
    }
}
