package org.javafn.either;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.*;

class EitherTest {

    @Test
    @DisplayName("Right construction and value retrieval")
    void testRight() {
        Either<Integer, String> either = Either.right("hello");
        assertTrue(either.isRight());
        assertFalse(either.isLeft());
        assertEquals("hello", either.expectRight());
        assertEquals(Optional.of("hello"), either.optRight());
        assertEquals(Optional.empty(), either.optLeft());
    }

    @Test
    @DisplayName("Left construction and value retrieval")
    void testLeft() {
        Either<Integer, String> either = Either.left(42);
        assertTrue(either.isLeft());
        assertFalse(either.isRight());
        assertEquals(42, either.expectLeft());
        assertEquals(Optional.of(42), either.optLeft());
        assertEquals(Optional.empty(), either.optRight());
    }

    @Test
    @DisplayName("Type-safe access via as()")
    void testAs() {
        Either<Integer, String> either = Either.right("hello");
        if (either instanceof Either.Right<?, ?> r) {
            assertEquals("hello", either.as(r).value());
        }

        Either<Integer, String> eitherLeft = Either.left(42);
        if (eitherLeft instanceof Either.Left<?, ?> l) {
            assertEquals(42, eitherLeft.as(l).value());
        }
    }

    @Test
    @DisplayName("as() throws IllegalArgumentException on wrong type")
    void testAsWrongType() {
        Either<Integer, String> right = Either.right("hello");
        Either<Integer, String> left = Either.left(42);
        assertThrows(IllegalArgumentException.class, () -> right.as((Either.Left<?, ?>)left));
        assertThrows(IllegalArgumentException.class, () -> left.as((Either.Right<?, ?>)right));
        if (right instanceof Either.Right<?, ?> r) {
            Either.Right<Integer, String> asRight = right.as(r);
            assertSame(right, asRight);
        } else {
            fail();
        }
        if (left instanceof Either.Left<?, ?> l) {
            Either.Left<Integer, String> asLeft = left.as(l);
            assertSame(left, asLeft);
        } else {
            fail();
        }
    }

    @Test
    @DisplayName("map transforms both sides")
    void testMap() {
        Either<Integer, String> eitherRight = Either.right("hello");
        var mappedRight = eitherRight.map(i -> i * 2, String::toUpperCase);
        assertEquals("HELLO", mappedRight.expectRight());

        Either<Integer, String> eitherLeft = Either.left(42);
        var mappedLeft = eitherLeft.map(i -> i * 2, String::toUpperCase);
        assertEquals(84, mappedLeft.expectLeft());
    }

    @Test
    @DisplayName("mapLeft transforms only the left side")
    void testMapLeft() {
        Either<Integer, String> eitherRight = Either.right("hello");
        var mappedRight = eitherRight.mapLeft(i -> i * 2);
        assertEquals("hello", mappedRight.expectRight());

        Either<Integer, String> eitherLeft = Either.left(42);
        var mappedLeft = eitherLeft.mapLeft(i -> i * 2);
        assertEquals(84, mappedLeft.expectLeft());
    }

    @Test
    @DisplayName("mapRight transforms only the right side")
    void testMapRight() {
        Either<Integer, String> eitherRight = Either.right("hello");
        var mappedRight = eitherRight.mapRight(String::toUpperCase);
        assertEquals("HELLO", mappedRight.expectRight());

        Either<Integer, String> eitherLeft = Either.left(42);
        var mappedLeft = eitherLeft.mapRight(String::toUpperCase);
        assertEquals(42, mappedLeft.expectLeft());
    }

    @Test
    @DisplayName("filterLeft returns true for Right or matching Left")
    void testFilterLeft() {
        Either<Integer, String> eitherRight = Either.right("hello");
        assertTrue(eitherRight.filterLeft(i -> i > 0));

        Either<Integer, String> eitherLeftMatch = Either.left(10);
        assertTrue(eitherLeftMatch.filterLeft(i -> i > 0));

        Either<Integer, String> eitherLeftNoMatch = Either.left(-10);
        assertFalse(eitherLeftNoMatch.filterLeft(i -> i > 0));
    }

    @Test
    @DisplayName("filterRight returns true for Left or matching Right")
    void testFilterRight() {
        Either<Integer, String> eitherLeft = Either.left(42);
        assertTrue(eitherLeft.filterRight(not(String::isEmpty)));

        Either<Integer, String> eitherRightMatch = Either.right("hello");
        assertTrue(eitherRightMatch.filterRight(not(String::isEmpty)));

        Either<Integer, String> eitherRightNoMatch = Either.right("");
        assertFalse(eitherRightNoMatch.filterRight(not(String::isEmpty)));
    }

    @Test
    @DisplayName("ifLeft performs side effect only on left")
    void testIfLeft() {
        final int[] count = {0};
        Either<Integer, String> eitherRight = Either.right("hello");
        eitherRight.ifLeft(i -> count[0]++);
        assertEquals(0, count[0]);

        Either<Integer, String> eitherLeft = Either.left(42);
        eitherLeft.ifLeft(i -> count[0]++);
        assertEquals(1, count[0]);
    }

    @Test
    @DisplayName("ifRight performs side effect only on right")
    void testIfRight() {
        final int[] count = {0};
        Either<Integer, String> eitherLeft = Either.left(42);
        eitherLeft.ifRight(s -> count[0]++);
        assertEquals(0, count[0]);

        Either<Integer, String> eitherRight = Either.right("hello");
        eitherRight.ifRight(s -> count[0]++);
        assertEquals(1, count[0]);
    }

    @Test
    @DisplayName("toRightIf converts matching left to right")
    void testToRightIf() {
        Either<Integer, String> eitherLeftMatch = Either.left(10);
        var converted = eitherLeftMatch.toRightIf(i -> i > 0, i -> "Positive");
        assertTrue(converted.isRight());
        assertEquals("Positive", converted.expectRight());

        Either<Integer, String> eitherLeftNoMatch = Either.left(-10);
        var notConverted = eitherLeftNoMatch.toRightIf(i -> i > 0, i -> "Positive");
        assertTrue(notConverted.isLeft());
        assertEquals(-10, notConverted.expectLeft());

        Either<Integer, String> eitherRight = Either.right("already right");
        var rightStayed = eitherRight.toRightIf(i -> i > 0, i -> "Positive");
        assertTrue(rightStayed.isRight());
        assertEquals("already right", rightStayed.expectRight());
    }

    @Test
    @DisplayName("toLeftIf converts matching right to left")
    void testToLeftIf() {
        Either<Integer, String> eitherRightMatch = Either.right("hello");
        var converted = eitherRightMatch.toLeftIf(not(String::isEmpty), s -> 1);
        assertTrue(converted.isLeft());
        assertEquals(1, converted.expectLeft());

        Either<Integer, String> eitherRightNoMatch = Either.right("");
        var notConverted = eitherRightNoMatch.toLeftIf(not(String::isEmpty), s -> 1);
        assertTrue(notConverted.isRight());
        assertEquals("", notConverted.expectRight());

        Either<Integer, String> eitherLeft = Either.left(42);
        var leftStayed = eitherLeft.toLeftIf(not(String::isEmpty), s -> 1);
        assertTrue(leftStayed.isLeft());
        assertEquals(42, leftStayed.expectLeft());
    }

    @Test
    @DisplayName("swap interchanges left and right")
    void testSwap() {
        Either<Integer, String> eitherLeft = Either.left(42);
        var swappedLeft = eitherLeft.swap();
        assertTrue(swappedLeft.isRight());
        assertEquals(42, swappedLeft.expectRight());

        Either<Integer, String> eitherRight = Either.right("hello");
        var swappedRight = eitherRight.swap();
        assertTrue(swappedRight.isLeft());
        assertEquals("hello", swappedRight.expectLeft());
    }

    @Test
    @DisplayName("reduce aggregates values from either side")
    void testReduce() {
        Either<Integer, String> eitherLeft = Either.left(42);
        var reducedLeft = eitherLeft.reduce(i -> "Num: " + i, s -> "Str: " + s);
        assertEquals("Num: 42", reducedLeft);

        Either<Integer, String> eitherRight = Either.right("hello");
        var reducedRight = eitherRight.reduce(i -> "Num: " + i, s -> "Str: " + s);
        assertEquals("Str: hello", reducedRight);
    }

    @Test
    @DisplayName("use triggers consumer on either side")
    void testUse() {
        final int[] count = {0};
        Either<Integer, String> eitherLeft = Either.left(42);
        eitherLeft.use(i -> count[0]++, s -> count[0]++);
        assertEquals(1, count[0]);

        Either<Integer, String> eitherRight = Either.right("hello");
        eitherRight.use(i -> count[0]++, s -> count[0]++);
        assertEquals(2, count[0]);
    }

    @Test
    @DisplayName("expectLeft/expectRight throw when variant is wrong")
    void testUnsafeExpects() {
        Either<Integer, String> eitherRight = Either.right("hello");
        assertThrows(IllegalStateException.class, eitherRight::expectLeft);

        Either<Integer, String> eitherLeft = Either.left(42);
        assertThrows(IllegalStateException.class, eitherLeft::expectRight);
    }
}
