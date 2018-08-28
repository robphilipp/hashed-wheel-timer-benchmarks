package com.digitalcipher.spiked.timing.benchmarks;

import com.digitalcipher.spiked.timing.HashedWheelTimer;
import com.digitalcipher.spiked.timing.WaitStrategies;
import com.digitalcipher.spiked.timing.WaitStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FixedDelayTimerAccuracy {

    private static String units(TimeUnit unit) {
        switch(unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "µs";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            default:
                return "";
        }
    }

    public static void main(final String... args) {
        final long resolution = 100L;
        final TimeUnit resolutionUnits = TimeUnit.MICROSECONDS;
        final int numBuckets = 512;
//        final WaitStrategy waitStrategy = WaitStrategies.busySpinWait();
        final WaitStrategy waitStrategy = WaitStrategies.yieldingWait();
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
        final long timeout = 1;
        final TimeUnit timeoutUnits = TimeUnit.SECONDS;
        final List<Long> executionTimes = new ArrayList<>((int)(TimeUnit.MICROSECONDS.convert(timeout, timeoutUnits) / TimeUnit.MICROSECONDS.convert(delay, delayUnits)));
        try {
            System.out.println(String.format("resolution: %,10d µs", TimeUnit.MICROSECONDS.convert(resolution, resolutionUnits)));
            final AtomicLong start = new AtomicLong(System.nanoTime());

            final CompletableFuture<List<Long>> future = timer.scheduleAtFixedRate(() -> {
                final long execTime = System.nanoTime();
                final long oldStart = start.getAndSet(execTime);
                executionTimes.add(execTime - oldStart);
                return executionTimes;
            }, timeout, timeoutUnits, delay, delay, delayUnits);

            final long futureStart = System.nanoTime();
            System.out.println("Future done: " + future.isDone());
            System.out.println("Future cancelled: " + future.isCancelled());
            System.out.println("Future completed exceptionally: " + future.isCompletedExceptionally());
            future.get();
            System.out.println(String.format(
                    "waited for future to complete: %,10d ms",
                    TimeUnit.MILLISECONDS.convert(System.nanoTime() - futureStart, TimeUnit.NANOSECONDS))
            );

            final AtomicInteger counter = new AtomicInteger(0);
            executionTimes.forEach(time ->
                    System.out.println(
                            String.format(
                                    "%,4d) %,10d %s",
                                    counter.incrementAndGet(),
                                    delayUnits.convert(time, TimeUnit.NANOSECONDS),
                                    units(delayUnits)
                            )
                    )
            );
        } catch(CancellationException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            timer.shutdown();
//            scheduledExecutorService.shutdownNow();
        }

        final Map<Long, Integer> histogram = Utils.histogram(executionTimes.stream().map(d -> resolution * (d / resolution)));
        System.out.println(Utils.printableHistogram(histogram));
    }
}
