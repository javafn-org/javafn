package org.javafn.bench;

import org.javafn.lang.Integers;
import org.javafn.result.IntResult;

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
	static int numRunsPerProbability = 200;

	public static void main(String[] args) {
		final List<String[]> data = new ArrayList<>();
		data.add(new String[] {"Probabilities",
				"Exceptions Min", "Exceptions Max", "Exceptions Avg",
				"Results Min", "Results Max", "Results Avg"});

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

			// So we're "using" the results
			final long[] goodValSum = {0, 0};
			final List<String> exceptions = new ArrayList<>(numBadStrings);
			final List<String> results = new ArrayList<>(numBadStrings);

			final IntConsumer testUsingException = index -> {
				Collections.shuffle(stringList);
				exceptions.clear();
				goodValSum[0] = 0;
				final long startExceptions = System.nanoTime();
				for (final String s : stringList) {
					try {
						goodValSum[0] += Integer.parseInt(s, 10);
					} catch (final NumberFormatException x) {
						exceptions.add(x.getMessage());
					}
				}
				if (index < 0) return;  // warmup
				exceptionTimes[index] = System.nanoTime() - startExceptions;
			};

			final IntConsumer testUsingResult = index -> {
				Collections.shuffle(stringList);
				results.clear();
				goodValSum[1] = 0;
				final long startResult = System.nanoTime();
				for (final String s : stringList) {
					Integers.parseInt(s, 10)
							.asErr().peek(results::add)
							.asOk().peek(v -> goodValSum[1] += v);
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
				if (goodValSum[0] != goodValSum[1]) {
					System.err.println("Something's wrong; Sums aren't equal!");
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
					Long.toString(Arrays.stream(exceptionTimes).min().orElse(-1L)),
					Long.toString(Arrays.stream(exceptionTimes).max().orElse(-1L)),
					Long.toString(Arrays.stream(exceptionTimes).sum() / numRunsPerProbability),
					Long.toString(Arrays.stream(resultTimes).min().orElse(-1L)),
					Long.toString(Arrays.stream(resultTimes).max().orElse(-1L)),
					Long.toString(Arrays.stream(resultTimes).sum() / numRunsPerProbability)
			});
		}
		writeToCSV(data);
	}

	private static void writeToCSV(List<String[]> data) {
		// It can be done in a single statement, but this may not be the best example of how.
//		Result.<Exception, File>ok(new File("src/test/java/org/javafn/bench/ExceptionVsResult.csv"))
//				.asOk().flatMap(Try.Map(FileWriter::new))
//				.asOk().map(output -> Pair.of(output,
//						data.stream().map(line -> String.join(", ", line)).collect(Collectors.joining("\n"))))
//				.asOk().flatMap(Pair.Map((out, csv) -> Try.get(() -> { out.write(csv); return out; })))
//				.asOk().flatMap(out -> Try.get(() -> { out.flush(); return out; }))
//				.asOk().flatMapToVoid(out -> Try.run(out::close))
//				.asErr().peek(ex -> System.err.println("An exception occurred writing to CSV: " + ex.getMessage()));

		final File file = new File("src/test/java/org/javafn/bench/ExceptionVsResult.csv");
		try {
			final FileWriter output = new FileWriter(file);
			for (final String[] line : data) {
				output.write(String.join(", ", line) + '\n');
			}
			output.flush();
			output.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
