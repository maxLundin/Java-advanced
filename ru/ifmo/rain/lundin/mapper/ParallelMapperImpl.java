package ru.ifmo.rain.lundin.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> threadList;
    private final Queue<Task> queue;

    public ParallelMapperImpl(int threads) {
        threadList = new ArrayList<>(threads);
        queue = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        doJob();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            }
            ));
            threadList.get(i).start();
        }
    }


    private void doJob() throws InterruptedException {
        Task runnable;
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }
            runnable = queue.poll();
        }

        runnable.runnable.run();

        synchronized (runnable.counter) {
            runnable.counter.increment();
            runnable.counter.notify();
        }

    }

    private class Counter {
        Counter(int value) {
            val = value;
        }

        void increment() {
            val++;
        }

        private int val;

        int getVal() {
            return val;
        }
    }

    private class Task {
        Task(Runnable runnable, Counter counter) {
            this.runnable = runnable;
            this.counter = counter;
        }

        private Runnable runnable;
        final Counter counter;
    }

    private class TaskPool {
        TaskPool(List<Runnable> tasks) {
            this.counter = new Counter(0);
            this.poolSize = tasks.size();
            for (Runnable task : tasks) {
                synchronized (queue) {
                    queue.add(new Task(task, counter));
                    if (check()) {
                        queue.notify();
                    }
                }
            }
        }

        private boolean check() {
            return counter.val < poolSize;
        }

        void waitForWorksDone() throws InterruptedException {
            synchronized (counter) {
                while (check()) {
                    counter.wait();
                }
            }
        }

        private int poolSize;
        final Counter counter;
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> resList = new ArrayList<>(Collections.nCopies(args.size(), null));

        List<Runnable> runnableArrayList = new ArrayList<>(args.size());

        for (int i = 0; i < args.size(); i++) {
            final int final_i = i;
            runnableArrayList.add(() -> resList.set(final_i, f.apply(args.get(final_i))));
        }

        TaskPool tp = new TaskPool(runnableArrayList);

        tp.waitForWorksDone();

        return resList;
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        threadList.forEach(Thread::interrupt);

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}

