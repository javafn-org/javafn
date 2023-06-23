package org.javafn.either;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EitherTest {

    @Test public void testEitherLeft() {
        final UUID left = UUID.randomUUID();
        final Either<UUID, Void> res = Either.ofLeft(left);
        assertTrue("Expected the result's isLeft to return true", res.isLeft());
        assertFalse("Expected the result's isRight to return false", res.isRight());
        assertEquals("Expected the leftor stored in the result to be returned",
                left, res.asLeft().get());
        assertFalse("Expected the right as optional to be empty", res.asRight().opt().isPresent());
    }
    @Test public void testEitherRight() {
        final UUID right = UUID.randomUUID();
        final Either<Void, UUID> res = Either.ofRight(right);
        assertTrue("Expected the result's isRight to return true", res.isRight());
        assertFalse("Expected the result's isLeft to return false", res.isLeft());
        assertEquals("Expected the right value stored in the result to be returned",
                right, res.asRight().get());
        assertFalse("Expected the left as optional to be empty", res.asLeft().opt().isPresent());
    }

    @Test(expected = NoSuchElementException.class) public void testLeftGetRightThrows() {
        final Either<UUID, Void> res = Either.ofLeft(UUID.randomUUID());
        res.asRight().get();
    }

    @Test(expected = NoSuchElementException.class) public void testRightGetLeftThrows() {
        final Either<Void, UUID> res = Either.ofRight(UUID.randomUUID());
        res.asLeft().get();
    }

    @Test public void testLeftMapGet() {
        final UUID left = UUID.randomUUID();
        final Either<UUID, Void> res = Either.ofLeft(left);
        assertEquals("Epected the leftor value to be mapped and retrieved",
                left.toString(), res.asLeft().map(UUID::toString).asLeft().get());
        res.asRight().map(right -> { fail("Expected the map function to not be called for an right projection of an left."); return null; });
    }
    @Test public void testRightMapGet() {
        final UUID right = UUID.randomUUID();
        final Either<Void, UUID> res = Either.ofRight(right);
        assertEquals("Epected the right value to be mapped and retrieved",
                right.toString(), res.asRight().map(UUID::toString).asRight().get());
        res.asLeft().map(left -> { fail("Expected the map function to not be called for an left projection of an right."); return null;});
    }
    @Test public void testLeftPeek() {
        final UUID left = UUID.randomUUID();
        final Either<UUID, Void> res = Either.ofLeft(left);
        res.asLeft().peek(uuid -> assertEquals("Expected peek to be called with the wrapped left", left, uuid));
        res.asRight().peek(expectedNothing -> fail("Expected the peek function to not be called for an right projection of an left."));
    }

    @Test public void testRightPeek() {
        final UUID right = UUID.randomUUID();
        final Either<Void, UUID> res = Either.ofRight(right);
        res.asRight().peek(uuid -> assertEquals("Expected peek to be called with the wrapped right", right, uuid));
        res.asLeft().peek(expectedNothing -> fail("Expected the peek function to not be called for an left projection of an right."));
    }
    @Test public void testProjectionsAreEqual() {
        final Either<UUID, Void> resLeft = Either.ofLeft(UUID.randomUUID());
        assertEquals("Expected an left projection and an right projection of the same result to be equal.",
                resLeft.asRight(), resLeft.asLeft());
        final Either<Void, UUID> resRight = Either.ofRight(UUID.randomUUID());
        assertEquals("Expected an right projection and an left projection of the same result to be equal.",
                resRight.asLeft(), resRight.asRight());
    }
    @Test public void testSwap() {
        final UUID uuid = UUID.randomUUID();
        final Either<UUID, Void> res = Either.ofLeft(uuid);
        final Either<Void, UUID> swapped = res.swap();
        assertTrue(swapped.isRight());
        assertEquals("Expected swapped result to return right value",
                uuid, swapped.asRight().get());
        final Either<UUID, Void> doubleSwapped = swapped.swap();
        assertEquals("Expected the original and doubleswapped result to be the same",
                res, doubleSwapped);
    }
    @Test public void testSwapLeftIf() {
        final String mappedValue = "Forty-Two!";
        final Either<Integer, String> res = Either.ofLeft(42);
        final Either<Integer, String> swapped = res.asLeft().filterMap(i -> i == 42, i -> mappedValue);
        assertTrue("Expected the result to be swapped into an right.", swapped.isRight());
        assertEquals("Expected the result to be the mapped string", mappedValue, swapped.asRight().get());
        final Either<Integer, String> notSwapped = res.asLeft().filterMap(i -> i == 0, i -> mappedValue);
        assertFalse("Expected the result to not be swapped into an right.", notSwapped.isRight());
        assertEquals("Expected the not swapped value to be equal to the original.", res, notSwapped);
    }
    @Test public void testSwapRightIf() {
        final Integer mappedValue = 42;
        final Either<Integer, String> res = Either.ofRight("Forty-Two!");
        final Either<Integer, String> swapped = res.asRight().filterMap("Forty-Two!"::equals, i -> mappedValue);
        assertTrue("Expected the result to be swapped into an left.", swapped.isLeft());
        assertEquals("Expected the result to be the mapped integer", mappedValue, swapped.asLeft().get());
        final Either<Integer, String> notSwapped = res.asRight().filterMap("Not 42"::equals, i -> mappedValue);
        assertFalse("Expected the result to not be swapped into an left.", notSwapped.isLeft());
        assertEquals("Expected the not swapped value to be equal to the original.", res, notSwapped);
    }
    @Test public void testFoldLeft() {
        assertEquals("Expected fold on left to return wrapped value",
                Integer.valueOf(42),
                Either.ofLeft(42).asLeft().orElseMap(s -> -42));
        assertEquals("Expected fold on right to return mapped value",
                -42,
                Either.ofRight("Forty-Two!").asLeft().orElseMap(s -> -42));
    }
    @Test public void testFoldRight() {
        assertEquals("Expected fold on right to return wrapped value",
                "Forty-Two!",
                Either.ofRight("Forty-Two!").asRight().orElseMap(i -> "Negative Forty-Two"));
        assertEquals("Expected fold on left to return mapped value",
                "Negative Forty-Two",
                Either.ofLeft(42).asRight().orElseMap(i -> "Negative Forty-Two"));
    }
}
