package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class ResultCompanionTest {

    @Test
    @DisplayName("Oks.map delegates to Result.map")
    void testOksMap() {
        var res = Result.ok("123");
        Function<Result<String>, Result<Integer>> mapper = Result.Oks.map(Integer::parseInt);
        var result = mapper.apply(res);
        assertEquals(123, result.expectOk());
    }

    @Test
    @DisplayName("Oks.filter delegates to Result.filter")
    void testOksFilter() {
        var resOk = Result.ok("hello");
        var resErr = Result.<String>err("fail");
        Predicate<Result<String>> filter = Result.Oks.filter(s -> s.startsWith("h"));
        
        assertTrue(filter.test(resOk));
        assertTrue(filter.test(resErr)); // Result.filter returns true for Err
    }

    @Test
    @DisplayName("Oks.isOkAnd delegates to Result.isOkAnd")
    void testOksIsOkAnd() {
        var resOk = Result.ok("hello");
        var resErr = Result.<String>err("fail");
        Predicate<Result<String>> isOkAnd = Result.Oks.isOkAnd(s -> s.length() > 2);
        
        assertTrue(isOkAnd.test(resOk));
        assertFalse(isOkAnd.test(resErr));
    }

    @Test
    @DisplayName("Oks.failIf delegates to Result.failIf")
    void testOksFailIf() {
        var res = Result.ok("fail me");
        Function<Result<String>, Result<String>> failIf = Result.Oks.failIf(s -> s.contains("fail"), s -> AnyError.from("failed"));
        var result = failIf.apply(res);
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("Oks.flatMap delegates to Result.flatMap")
    void testOksFlatMap() {
        var res = Result.ok("123");
        Function<Result<String>, Result<Integer>> flatMap = Result.Oks.flatMap(v -> Result.ok(Integer.parseInt(v)));
        var result = flatMap.apply(res);
        assertEquals(123, result.expectOk());
    }

    @Test
    @DisplayName("Oks.ifOk delegates to Result.ifOk")
    void testOksIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = Result.ok("success");
        Consumer<Result<String>> ifOk = Result.Oks.ifOk(v -> count.incrementAndGet());
        ifOk.accept(res);
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("Errs.map delegates to Result.mapErr")
    void testErrsMap() {
        var res = Result.<String>err("fail");
        Function<Result<String>, Result<String>> mapper = Result.Errs.map(e -> AnyError.from("mapped"));
        var result = mapper.apply(res);
        assertEquals("mapped", result.expectErr().message());
    }

    @Test
    @DisplayName("Errs.filter delegates to Result.filterErr")
    void testErrsFilter() {
        var resOk = Result.ok("success");
        var resErr = Result.<String>err("fail");
        Predicate<Result<String>> filter = Result.Errs.filter(e -> e.message().contains("fail"));
        
        assertTrue(filter.test(resOk)); // Result.filterErr returns true for Ok
        assertTrue(filter.test(resErr));
    }

    @Test
    @DisplayName("Errs.isErrAnd delegates to Result.isErrAnd")
    void testErrsIsErrAnd() {
        var resOk = Result.ok("success");
        var resErr = Result.<String>err("fail");
        Predicate<Result<String>> isErrAnd = Result.Errs.isErrAnd(e -> e.message().contains("fail"));
        
        assertFalse(isErrAnd.test(resOk));
        assertTrue(isErrAnd.test(resErr));
    }

    @Test
    @DisplayName("Errs.recoverIf delegates to Result.recoverIf")
    void testErrsRecoverIf() {
        var res = Result.<String>err("fail");
        Function<Result<String>, Result<String>> recoverIf = Result.Errs.recoverIf(e -> e.message().contains("fail"), e -> "recovered");
        var result = recoverIf.apply(res);
        assertTrue(result.isOk());
        assertEquals("recovered", result.expectOk());
    }

    @Test
    @DisplayName("Errs.flatRecover delegates to Result.flatRecover")
    void testErrsFlatRecover() {
        var res = Result.<String>err("fail");
        Function<Result<String>, Result<String>> flatRecover = Result.Errs.flatRecover(e -> Result.ok("recovered"));
        var result = flatRecover.apply(res);
        assertTrue(result.isOk());
        assertEquals("recovered", result.expectOk());
    }

    @Test
    @DisplayName("Errs.ifErr delegates to Result.ifErr")
    void testErrsIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = Result.<String>err("fail");
        Consumer<Result<String>> ifErr = Result.Errs.ifErr(e -> count.incrementAndGet());
        ifErr.accept(res);
        assertEquals(1, count.get());
    }
}
