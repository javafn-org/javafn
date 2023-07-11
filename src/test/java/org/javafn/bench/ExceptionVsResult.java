package org.javafn.bench;

import org.javafn.result.IntResult;
import org.javafn.tuple.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ExceptionVsResult {
	static int numStrings = 100_000;
	static int numRunsPerWarmup = 50;
	static int numRunsPerProbability = 100;

	public static void main(String[] args) {
		final List<String[]> data = new ArrayList<>();
		data.add(new String[] {"Probabilities", "Exceptions", "Results"});

		final Random random = new Random();

		final List<Double> probabilities = DoubleStream.iterate(0.0, v -> v + 0.05)
				.limit(21)
				.boxed()
				.collect(Collectors.toList());
		Collections.shuffle(probabilities);

		for (int i = 0; i < probabilities.size(); i++) {
			final double probability = probabilities.get(i);
			System.out.format("Working on error probability %.2f; %.1f%% Complete.%n", probability,
					(100.0 * (i / (double) probabilities.size())));

			// Generate some random strings
			final int numBadStrings = (int) Math.floor(numStrings * probability);
			final List<String> stringList = Stream.concat(
					IntStream.range(0, numStrings - numBadStrings)
							.map(ignored -> random.nextInt())
							.mapToObj(Integer::toString),
					IntStream.range(0, numBadStrings)
							.mapToObj(ignored -> IntStream.range(0, 20)
									.map(ignored2 -> random.nextInt(90) + ' ')
									.mapToObj(String::valueOf)
									.collect(Collectors.joining())))
					.collect(Collectors.toList());

			final long[] resultTimes = new long[numRunsPerProbability];
			final long[] exceptionTimes = new long[numRunsPerProbability];

			final List<String> exceptions = new ArrayList<>(numBadStrings);
			final IntConsumer testUsingException = index -> {
				Collections.shuffle(stringList);
				exceptions.clear();
				final long startExceptions = System.nanoTime();
				for (final String s : stringList) {
					try {
						Integer.parseInt(s, 10);
					} catch (final NumberFormatException x) {
						exceptions.add(x.getMessage());
					}
				}
				if (index < 0) return;  // warmup
				exceptionTimes[index] = System.nanoTime() - startExceptions;
			};

			final List<String> results = new ArrayList<>(numBadStrings);
			final IntConsumer testUsingResult = index -> {
				Collections.shuffle(stringList);
				results.clear();
				final long startResult = System.nanoTime();
				for (final String s : stringList) {
					ExceptionVsResult.parseInt(s, 10).asErr().peek(results::add);
				}
				if (index < 0) return;  // warmup
				resultTimes[index] =  System.nanoTime() - startResult;
			};

			for (int j = 0; j < numRunsPerWarmup + numRunsPerProbability; j++) {
				final int index = j - numRunsPerWarmup;
				if (random.nextBoolean()) {
					testUsingException.accept(index);
					testUsingResult.accept(index);
				} else {
					testUsingResult.accept(index);
					testUsingException.accept(index);
				}
				// Validate that we're getting the same results
				if (results.size() != exceptions.size()) {
					System.err.println("Something's wrong; message sets are not the same size!");
				}
				// More detailed validation, outputting the first result of the mismatching sets
				// if (results.size() != exceptions.size() ||
				// 		Pair.zip(results.stream().sorted(), exceptions.stream().sorted())
				// 				.anyMatch(Pair.Filter((resMsg, exMsg) -> !resMsg.equals(exMsg)))) {
				// 	System.err.println("Oops, the result and error message collections aren't the same size or have differing elements!");
				// 	System.out.printf("%s ?= %s%n", results.get(0), exceptions.get(0));
				// }
			}

			data.add(new String[]{
					String.format("%.2f", probability),
					Long.toString(Arrays.stream(exceptionTimes).sum() / numRunsPerProbability),
					Long.toString(Arrays.stream(resultTimes).sum() / numRunsPerProbability)
			});
		}
		writeToCSV(data);
	}

	private static IntResult<String> parseInt(final String s, final int radix) {
		// Borrowed from Integer.parseInt(...)
		if (s == null) {
			return IntResult.err("null");
		}

		if (radix < Character.MIN_RADIX) {
			return IntResult.err("radix " + radix + " less than Character.MIN_RADIX");
		}

		if (radix > Character.MAX_RADIX) {
			return IntResult.err("radix " + radix + " greater than Character.MAX_RADIX");
		}

		int result = 0;
		boolean negative = false;
		int i = 0, len = s.length();
		int limit = -Integer.MAX_VALUE;
		int multmin;
		int digit;

		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = Integer.MIN_VALUE;
				} else if (firstChar != '+')
					return forInputString(s);

				if (len == 1) // Cannot have lone "+" or "-"
					return forInputString(s);
				i++;
			}
			multmin = limit / radix;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++),radix);
				if (digit < 0) {
					return forInputString(s);
				}
				if (result < multmin) {
					return forInputString(s);
				}
				result *= radix;
				if (result < limit + digit) {
					return forInputString(s);
				}
				result -= digit;
			}
		} else {
			return forInputString(s);
		}
		return IntResult.ok(negative ? result : -result);
	}

	private static IntResult<String> forInputString(final String s) {
		return IntResult.err("For input string: \"" + s + "\"");
	}

	private static void writeToCSV(List<String[]> data) {
		final File file = new File("src/test/java/org/javafn/bench/ExceptionVsResult.csv");
		try {
			final FileWriter output = new FileWriter(file);
			for (final String[] line : data) {
				output.write(String.format("%s, %s, %s\n", line[0], line[1], line[2]));
			}
			output.flush();
			output.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
