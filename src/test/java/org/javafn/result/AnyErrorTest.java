package org.javafn.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnyErrorTest {

    enum TestError {
        FILE_NOT_FOUND,
        ACCESS_DENIED
    }

    @Test
    @DisplayName("StringError captures and returns message")
    void testStringError() {
        var err = AnyError.from("simple error");
        assertEquals("simple error", err.message());
        assertInstanceOf(AnyError.StringError.class, err);
    }

    @Test
    @DisplayName("StringError.except() returns a new RuntimeException")
    void testStringErrorExcept() {
        var err = AnyError.from("simple error");
        var ex = err.except();
        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("simple error", ex.getMessage());
    }

    @Test
    @DisplayName("ExceptionError captures exception and returns it via except()")
    void testExceptionError() {
        var ex = new IllegalArgumentException("invalid argument");
        var err = AnyError.from(ex);
        assertEquals(ex, err.except());
        assertTrue(err.message().contains("IllegalArgumentException"));
        assertInstanceOf(AnyError.ExceptionError.class, err);
    }

    @Test
    @DisplayName("ExceptionError includes prefix in message")
    void testExceptionErrorPrefix() {
        var ex = new IllegalArgumentException("invalid argument");
        var err = AnyError.from(ex, "Custom Prefix");
        assertTrue(err.message().startsWith("Custom Prefix"));
        assertTrue(err.message().contains("IllegalArgumentException"));
    }

    @Test
    @DisplayName("EnumError captures enum and returns formatted message")
    void testEnumError() {
        var err = AnyError.from(TestError.FILE_NOT_FOUND);
        assertEquals("FILE_NOT_FOUND", err.message());
        assertInstanceOf(AnyError.EnumError.class, err);
    }

    @Test
    @DisplayName("EnumError includes details in message")
    void testEnumErrorDetails() {
        var err = AnyError.from(TestError.ACCESS_DENIED, "User not authorized");
        assertEquals("ACCESS_DENIED: User not authorized", err.message());
    }

    @Test
    @DisplayName("ErrorList joins messages with newlines")
    void testErrorListMessage() {
        var errs = AnyError.joined(List.of(
                AnyError.from("err1"),
                AnyError.from("err2"),
                AnyError.from("err3")
        ));
        assertEquals("err1\nerr2\nerr3", errs.message());
        assertInstanceOf(AnyError.ErrorList.class, errs);
    }

    @Test
    @DisplayName("ErrorList from string list creates ErrorList of StringErrors")
    void testErrorListFromStringList() {
        var err = AnyError.from(List.of("err1", "err2"));
        assertEquals("err1\nerr2", err.message());
    }

    @Test
    @DisplayName("Empty ErrorList produces empty message")
    void testEmptyErrorList() {
        var err = new AnyError.ErrorList();
        assertEquals("", err.message());
    }

    @Test
    @DisplayName("join combines errors into ErrorList")
    void testJoin() {
        var e1 = AnyError.from("e1");
        var e2 = AnyError.from("e2");
        var e3 = AnyError.from("e3");

        // String + String
        var res1 = e1.join(e2);
        assertEquals("e1\ne2", res1.message());
        assertInstanceOf(AnyError.ErrorList.class, res1);

        // ErrorList + String
        var res2 = res1.join(e3);
        assertEquals("e1\ne2\ne3", res2.message());

        // String + ErrorList
        var res3 = e1.join(res1);
        assertEquals("e1\ne1\ne2", res3.message());

        // ErrorList + ErrorList
        var el1 = AnyError.joined(List.of(e1));
        var el2 = AnyError.joined(List.of(e2, e3));
        var res4 = el1.join(el2);
        assertEquals("e1\ne2\ne3", res4.message());

        // Join with null
        assertEquals(e1, e1.join(null));
    }

    @Test
    @DisplayName("fold aggregates errors from results")
    void testFold() {
        // All OK
        var resOk = AnyError.fold(
                Result.ok("1"),
                Result.ok("2")
        );
        assertTrue(resOk.isOk());

        // Mixed
        var resMixed = AnyError.fold(
                Result.ok("1"),
                Result.<String>err("err2"),
                Result.ok("3"),
                Result.<String>err("err4")
        );
        assertTrue(resMixed.isErr());
        assertEquals("err2\nerr4", resMixed.expectErr().message());

        // All Err
        var resErr = AnyError.fold(
                Result.<String>err("err1"),
                Result.<String>err("err2")
        );
        assertTrue(resErr.isErr());
        assertEquals("err1\nerr2", resErr.expectErr().message());
    }

    @Test
    @DisplayName("static fail methods create Result.Err")
    void testStaticFail() {
        assertTrue(AnyError.fail("msg").isErr());
        assertTrue(AnyError.fail(List.of("m1", "m2")).isErr());
        assertTrue(AnyError.fail(new Exception()).isErr());
        assertTrue(AnyError.fail(TestError.FILE_NOT_FOUND).isErr());
    }

    @Test
    @DisplayName("instance fail method creates Result.Err")
    void testInstanceFail() {
        var err = AnyError.from("msg");
        var res = err.fail();
        assertTrue(res.isErr());
        assertEquals("msg", res.expectErr().message());
    }
}
