package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    @DisplayName("Result.ok factory creates an Ok instance with value")
    void testOkFactory() {
        var res = Result.ok("hello");
        assertTrue(res.isOk());
        assertFalse(res.isErr());
        assertEquals("hello", res.expectOk());
    }

    @Test
    @DisplayName("Result.ok() factory creates an Ok instance with null value (Void)")
    void testOkVoidFactory() {
        var res = VoidResult.ok();
        assertTrue(res.isOk());
        assertFalse(res.isErr());
    }

    @Test
    @DisplayName("Result.err factories create Err instances")
    void testErrFactories() {
        // String err
        var res1 = Result.<String>err("error message");
        assertTrue(res1.isErr());
        assertEquals("error message", res1.expectErr().message());

        // List err
        var messages = List.of("err1", "err2");
        var res2 = Result.<String>err(messages);
        assertTrue(res2.isErr());
        AnyError res2Err = res2.expectErr();
        assertInstanceOf(AnyError.ErrorList.class, res2Err);
        AnyError.ErrorList res2ErrLst = (AnyError.ErrorList) res2Err;
        assertEquals(res2ErrLst.errors().size(), messages.size());
        assertEquals(res2ErrLst.errors().stream().map(AnyError::message).toList(), messages);

        // Exception err
        var ex = new RuntimeException("exception");
        var res3 = Result.<RuntimeException, String>err(ex);
        assertTrue(res3.isErr());
        assertEquals(ex, res3.expectErr().except());

        // Enum err
        enum MyError { FAIL }
        var res4 = Result.<MyError, String>err(MyError.FAIL);
        assertTrue(res4.isErr());
        AnyError res4Err = res4.expectErr();
        assertInstanceOf(AnyError.EnumError.class, res4Err);
        AnyError.EnumError<? extends Enum<?>> res4ErrEnum = (AnyError.EnumError<?>) res4.expectErr();
        assertEquals(MyError.class, res4ErrEnum.type());
        assertEquals(MyError.FAIL, res4ErrEnum.error());
    }

    @Test
    @DisplayName("expectOk throws exception when Result is Err")
    void testExpectOkFailure() {
        var res = Result.<String>err("fail");
        assertThrows(IllegalStateException.class, res::expectOk);
    }

    @Test
    @DisplayName("expectErr throws exception when Result is Ok")
    void testExpectErrFailure() {
        var res = Result.ok("success");
        assertThrows(IllegalStateException.class, res::expectErr);
    }

    @Test
    @DisplayName("as(Ok) throws exception when called on an Err")
    void testAsFailure() {
        var res = Result.<String>err("fail");
        var okInstance = new Result.Ok<>("some ok");
        assertThrows(IllegalArgumentException.class, () -> res.res(okInstance));
    }

    @Test
    @DisplayName("as(Ok) throws exception when argument is not the instance itself")
    void testAsWrongInstance() {
        var res = Result.ok("success");
        var okInstance = new Result.Ok<>("different ok");
        assertThrows(IllegalArgumentException.class, () -> res.res(okInstance));
    }
}
