package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class IntResultCompanionTest {

    @Test
    @DisplayName("IntResult.Oks.map delegates to IntResult.map")
    void testOksMap() {
        var res = IntResult.ok(123);
        Function<IntResult, IntResult> mapper = IntResult.Oks.map(v -> v * 2);
        var result = mapper.apply(res);
        assertEquals(246, result.expectOk());
    }

    @Test
    @DisplayName("IntResult.Oks.filter delegates to IntResult.filter")
    void testOksFilter() {
        var resOk = IntResult.ok(123);
        var resErr = IntResult.err("fail");
        Predicate<IntResult> filter = IntResult.Oks.filter(v -> v > 100);
        
        assertTrue(filter.test(resOk));
        assertTrue(filter.test(resErr)); // IntResult.filter returns true for Err
    }

    @Test
    @DisplayName("IntResult.Oks.isOkAnd delegates to IntResult.isOkAnd")
    void testOksIsOkAnd() {
        var resOk = IntResult.ok(123);
        var resErr = IntResult.err("fail");
        Predicate<IntResult> isOkAnd = IntResult.Oks.isOkAnd(v -> v > 100);
        
        assertTrue(isOkAnd.test(resOk));
        assertFalse(isOkAnd.test(resErr));
    }

    @Test
    @DisplayName("IntResult.Oks.failIf delegates to IntResult.failIf")
    void testOksFailIf() {
        var res = IntResult.ok(123);
        Function<IntResult, IntResult> failIf = IntResult.Oks.failIf(v -> v > 100, v -> AnyError.from("failed"));
        var result = failIf.apply(res);
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("IntResult.Oks.flatMap delegates to IntResult.flatMap")
    void testOksFlatMap() {
        var res = IntResult.ok(123);
        Function<IntResult, IntResult> flatMap = IntResult.Oks.flatMap(v -> IntResult.ok(v + 1));
        var result = flatMap.apply(res);
        assertEquals(124, result.expectOk());
    }

    @Test
    @DisplayName("IntResult.Oks.ifOk delegates to IntResult.ifOk")
    void testOksIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = IntResult.ok(123);
        Consumer<IntResult> ifOk = IntResult.Oks.ifOk(v -> count.incrementAndGet());
        ifOk.accept(res);
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("IntResult.Errs.map delegates to IntResult.mapErr")
    void testErrsMap() {
        var res = IntResult.err("fail");
        Function<IntResult, IntResult> mapper = IntResult.Errs.map(e -> AnyError.from("mapped"));
        var result = mapper.apply(res);
        assertEquals("mapped", result.expectErr().message());
    }

    @Test
    @DisplayName("IntResult.Errs.filter delegates to IntResult.filterErr")
    void testErrsFilter() {
        var resOk = IntResult.ok(123);
        var resErr = IntResult.err("fail");
        Predicate<IntResult> filter = IntResult.Errs.filter(e -> e.message().contains("fail"));
        
        assertTrue(filter.test(resOk)); // IntResult.filterErr returns true for Ok
        assertTrue(filter.test(resErr));
    }

    @Test
    @DisplayName("IntResult.Errs.isErrAnd delegates to IntResult.isErrAnd")
    void testErrsIsErrAnd() {
        var resOk = IntResult.ok(123);
        var resErr = IntResult.err("fail");
        Predicate<IntResult> isErrAnd = IntResult.Errs.isErrAnd(e -> e.message().contains("fail"));
        
        assertFalse(isErrAnd.test(resOk));
        assertTrue(isErrAnd.test(resErr));
    }

    @Test
    @DisplayName("IntResult.Errs.recoverIf delegates to IntResult.recoverIf")
    void testErrsRecoverIf() {
        var res = IntResult.err("fail");
        Function<IntResult, IntResult> recoverIf = IntResult.Errs.recoverIf(e -> e.message().contains("fail"), e -> 123);
        var result = recoverIf.apply(res);
        assertTrue(result.isOk());
        assertEquals(123, result.expectOk());
    }

    @Test
    @DisplayName("IntResult.Errs.flatRecover delegates to IntResult.flatRecover")
    void testErrsFlatRecover() {
        var res = IntResult.err("fail");
        Function<IntResult, IntResult> flatRecover = IntResult.Errs.flatRecover(e -> IntResult.ok(123));
        var result = flatRecover.apply(res);
        assertTrue(result.isOk());
        assertEquals(123, result.expectOk());
    }

    @Test
    @DisplayName("IntResult.Errs.ifErr delegates to IntResult.ifErr")
    void testErrsIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = IntResult.err("fail");
        Consumer<IntResult> ifErr = IntResult.Errs.ifErr(e -> count.incrementAndGet());
        ifErr.accept(res);
        assertEquals(1, count.get());
    }
}
