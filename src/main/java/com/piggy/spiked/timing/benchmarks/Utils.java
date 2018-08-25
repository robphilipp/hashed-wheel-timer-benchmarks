package com.piggy.spiked.timing.benchmarks;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

public class Utils {

    public static String stars(final long actual, final long expected, final long resolution) {
        final long delta = actual - expected;
        final int deviation = (int) (delta / resolution) + 1;
        final StringBuilder builder = new StringBuilder();
        if (deviation < 0) {
            builder.append("-");
        } else {
            builder.append(" ");
        }
        for (int i = 0; i < deviation; i++) {
            builder.append("*");
        }
        return builder.toString();
    }

    public static <T> Map<T, Integer> histogram(final Stream<T> stream) {
        return stream
                .collect(
                        groupingBy(
                                Function.identity(),
                                TreeMap::new,
                                mapping(i -> 1, Collectors.summingInt(Integer::intValue))
                        )
                );
    }

    public static <T extends Number> String printableHistogram(final Map<T, Integer> histogram) {
        final StringBuilder builder = new StringBuilder();
        for(Map.Entry<T, Integer> entry: histogram.entrySet()) {
            builder.append(String.format("%,10d | ", (long)entry.getKey()));
            for (int i = 0; i < entry.getValue(); i++) {
                builder.append("*");
            }
            builder.append(String.format("  (%d)", entry.getValue())).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
