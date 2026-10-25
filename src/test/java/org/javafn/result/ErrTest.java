package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ErrTest {

    @Test
    @DisplayName("Err.map returns this unmodified (coerced type)")
    void testMap() {
        var res = Result.<String>err("fail");
        var mapped = res.map(v -> "success");
        assertTrue(mapped.isErr());
        assertEquals("fail", mapped.expectErr().message());
        // Since into() is called, it's a new Err object but with same value
        assertNotSame(res, mapped); 
    }

    @Test
    @DisplayName("Err.flatMap returns this unmodified (coerced type)")
    void testFlatMap() {
        var res = Result.<String>err("fail");
        var flatMapped = res.flatMap(v -> Result.ok("success"));
        assertTrue(flatMapped.isErr());
        assertEquals("fail", flatMapped.expectErr().message());
        assertNotSame(res, flatMapped);
    }

    @Test
    @DisplayName("Err.mapErr applies function to error value")
    void testMapErr() {
        var res = Result.<String>err("fail");
        var mapped = res.mapErr(e -> AnyError.from("mapped error"));
        assertTrue(mapped.isErr());
        assertEquals("mapped error", mapped.expectErr().message());
    }

    @Test
    @DisplayName("Err.flatRecover applies function to error value")
    void testFlatRecover() {
        var res = Result.<String>err("fail");
        var recovered = res.flatRecover(e -> Result.ok("recovered"));
        assertTrue(recovered.isOk());
        assertEquals("recovered", recovered.expectOk());
    }

    @Test
    @DisplayName("Err.flatRecover returns Err if mapping function returns Err")
    void testFlatRecoverToErr() {
        var res = Result.<String>err("fail");
        var recovered = res.flatRecover(e -> Result.err("still failing"));
        assertTrue(recovered.isErr());
        assertEquals("still failing", recovered.expectErr().message());
    }

    @Test
    @DisplayName("Err.filterErr returns result of predicate")
    void testFilterErr() {
        var res = Result.<String>err("fail");
        assertTrue(res.filterErr(e -> e.message().contains("fail")));
        assertFalse(res.filterErr(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("Err.isErrAnd returns result of predicate")
    void testIsErrAnd() {
        var res = Result.<String>err("fail");
        assertTrue(res.isErrAnd(e -> e.message().contains("fail")));
        assertFalse(res.isErrAnd(e -> e.message().contains("success")));
        var res2 = Result.ok("fail");
        assertFalse(res2.isErrAnd(e -> e.message().contains("fail")));
        assertFalse(res2.isErrAnd(e -> e.message().contains("success")));
    }

    @Test
    @DisplayName("Err.recoverIf converts Err to Ok if predicate is true")
    void testRecoverIf() {
        var res = Result.<String>err("fail");
        var recovered = res.recoverIf(e -> e.message().contains("fail"), e -> "recovered");
        assertTrue(recovered.isOk());
        assertEquals("recovered", recovered.expectOk());

        var notRecovered = res.recoverIf(e -> e.message().contains("success"), e -> "recovered");
        assertSame(res, notRecovered);
    }

    @Test
    @DisplayName("Err.ifErr executes consumer")
    void testIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = Result.<String>err("fail");
        res.ifErr(e -> count.incrementAndGet());
        assertEquals(1, count.get());
        assertSame(res, res.ifErr(e -> {}));
    }

    @Test
    @DisplayName("Err.reduce delegates to fnErr")
    void testReduce() {
        var res = Result.<String>err("fail");
        var reduced = res.reduce(e -> e.message() + "-err-mapped", v -> v + "-ok-mapped");
        assertEquals("fail-err-mapped", reduced);
    }

    @Test
    @DisplayName("Err.consume delegates to fnErr")
    void testConsume() {
        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger errCount = new AtomicInteger();
        var res = Result.<String>err("fail");
        res.consume(e -> errCount.incrementAndGet(), v -> okCount.incrementAndGet());
        assertEquals(1, errCount.get());
        assertEquals(0, okCount.get());
    }

    @Test
    @DisplayName("Err.into coerces OK type")
    void testInto() {
        var res = Result.<String>err("fail");
        if (res instanceof Result.Err<String> asErr) {
            Result<Number> coerced = asErr.into();
            assertTrue(coerced.isErr());
            assertEquals("fail", coerced.expectErr().message());
        } else {
            fail();
        }
    }

    @Test
    @DisplayName("Err.intoInt/Long/Double creates specialized results")
    void testIntoSpecialized() {
        var res = new Result.Err<String>(AnyError.from("fail"));
        
        assertTrue(res.intoInt().isErr());
        assertEquals("fail", res.intoInt().expectErr().message());

        assertTrue(res.intoLong().isErr());
        assertEquals("fail", res.intoLong().expectErr().message());

        assertTrue(res.intoDouble().isErr());
        assertEquals("fail", res.intoDouble().expectErr().message());
    }
}
