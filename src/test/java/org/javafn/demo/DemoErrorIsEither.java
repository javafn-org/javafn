package org.javafn.demo;

import org.javafn.either.Either;
import org.javafn.either.Either.Right;
import org.javafn.result.Result;

import java.util.function.Function;

public class DemoErrorIsEither {

	public static void main(String[] args) {
		genStringErrorType().asErr()
				.peek(Right.Peek(Throwable::printStackTrace))
				.asErr().map(e -> e.reduce(Function.identity(), Throwable::getMessage))
				.asErr().peek(System.err::println);
		genExceptionErrorType().asErr()
				.peek(Right.Peek(Throwable::printStackTrace))
				.asErr().map(e -> e.reduce(Function.identity(), Throwable::getMessage))
				.asErr().peek(System.err::println);
	}

	static Result<Either<String, Exception>, Void> genStringErrorType() {
		return Result.err(Either.ofLeft("Not an exception, but an error nonetheless"));
	}

	static Result<Either<String, Exception>, Void> genExceptionErrorType() {
		return Result.err(Either.ofRight(new RuntimeException(("This is an exception that, let's assume, I caught"))));
	}
}
