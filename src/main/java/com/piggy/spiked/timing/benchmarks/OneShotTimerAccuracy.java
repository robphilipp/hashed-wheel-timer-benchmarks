package com.piggy.spiked.timing.benchmarks;

import com.piggy.spiked.timing.HashedWheelTimer;
import com.piggy.spiked.timing.WaitStrategies;
import com.piggy.spiked.timing.WaitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OneShotTimerAccuracy {

    public static void main(final String... args) {
        final long resolution = 200L;
        final TimeUnit resolutionUnits = TimeUnit.MICROSECONDS;
        final int numBuckets = 512;
        final WaitStrategy waitStrategy = WaitStrategies.busySpinWait();
//        final WaitStrategy waitStrategy = WaitStrategies.yieldingWait();
//        final WaitStrategy waitStrategy = WaitStrategies.sleepWait();

        final HashedWheelTimer timer = HashedWheelTimer.builder()
                .withTimerName("accuracy-test-timer")
                .withResolution(resolution, resolutionUnits)
                .withWheelSize(numBuckets)
                .withWaitStrategy(waitStrategy)
                .withDefaultExecutor()
                .build()
                .start();

        final long delay = 200;
        final TimeUnit delayUnits = TimeUnit.MICROSECONDS;
        final long expectedDelayMicros = TimeUnit.MICROSECONDS.convert(delay, delayUnits);
        final long resolutionMicros = TimeUnit.MICROSECONDS.convert(resolution, resolutionUnits);
        final List<Long> actualDelays = new ArrayList<>();
        System.out.println(String.format("resolution: %,10d µs", resolutionMicros));
        try {
            for (int i = 0; i < 100; i++) {
                final long startTime = System.nanoTime();
                final long executedTime = timer.schedule(System::nanoTime, delay, delayUnits).get();

                final long actualDelayMicros = TimeUnit.MICROSECONDS.convert(executedTime - startTime, TimeUnit.NANOSECONDS);
                System.out.println(String.format(
                        "expected delay: %,10d µs; actual delay: %,10d µs; %s",
                        expectedDelayMicros, actualDelayMicros,
                        Utils.stars(actualDelayMicros, expectedDelayMicros, resolutionMicros)

                ));
                actualDelays.add(actualDelayMicros);
                Thread.sleep(10);
            }
        }
        catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        finally {
            timer.shutdown();
        }

        final Map<Long, Integer> histogram = Utils.histogram(actualDelays.stream().map(d -> resolution * (d / resolution)));
        System.out.println(Utils.printableHistogram(histogram));
    }
}
