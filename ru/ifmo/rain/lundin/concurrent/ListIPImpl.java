package ru.ifmo.rain.lundin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListIPImpl implements ListIP {

    private <T, U> U doParallel(int numThreads, List<? extends T> values, Function<Stream<? extends T>, ? extends U> func, Function<Stream<? extends U>, ? extends U> func1) throws InterruptedException {
        if (values.size() == 0) {
            throw new InterruptedException("List is empty!");
        }
        if (numThreads == 0) {
            throw new InterruptedException("Cant't perform operation with 0 threads!");
        }

        numThreads = Math.min(numThreads, values.size());

        ArrayList<Thread> treads = new ArrayList<>(numThreads);

        int blockSize = values.size() / numThreads;
        int extra = values.size() % numThreads;

        int leftb = 0;
        List<Stream<? extends T>> listParts = new ArrayList<>();
        for (int j = 0; j < numThreads; j++) {
            int left = leftb;
            int right = leftb + blockSize + (j < extra ? 1 : 0);

            listParts.add(values.subList(left, right).stream());

            leftb = right;
        }

        ArrayList<U> results = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            results.add(null);
        }

        for (int j = 0; j < numThreads; j++) {
            final int pos = j;
            treads.add(new Thread(() -> results.set(pos, func.apply(listParts.get(pos)))));
            treads.get(j).start();
        }

        InterruptedException exception = null;
        for (int j = 0; j < numThreads; j++) {
            try {
                treads.get(j).join();
            } catch (InterruptedException e) {
                exception = e;
            }
        }
        if (exception != null) {
            throw exception;
        }
        return func1.apply(results.stream());
    }

    /**
     * Join values to string.
     *
     * @param threads number or concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.map(Object::toString).collect(Collectors.joining()), stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.filter(predicate).collect(Collectors.toList()), stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * Mas values.
     *
     * @param threads  number or concurrent threads.
     * @param values   values to filter.
     * @param function mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> function) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.map(function).collect(Collectors.toList()), stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }


    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.max(comparator).orElse(null), stream -> stream.max(comparator).orElse(null));
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.min(comparator).orElse(null), stream -> stream.min(comparator).orElse(null));
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(o -> o));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doParallel(threads, values, stream -> stream.anyMatch(predicate), stream -> stream.anyMatch(o -> o));
    }
}
