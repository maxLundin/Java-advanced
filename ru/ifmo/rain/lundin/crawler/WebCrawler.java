package ru.ifmo.rain.lundin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import ru.ifmo.rain.lundin.student.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler, AutoCloseable {
    private final Downloader downloader;
    private int perHost;
    private final ExecutorService downloadersPoll;
    private final ExecutorService extractorsPoll;
    private final Map<String, Integer> hostUsage;
    private final LinkedBlockingQueue<Pair<Runnable, String>> run;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPoll = Executors.newFixedThreadPool(downloaders);
        this.extractorsPoll = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;

        run = new LinkedBlockingQueue<>();

        hostUsage = new ConcurrentHashMap<>();
    }

    private Optional<String> getHost(String urlString, final Map<String, IOException> exceptionMap) {
        Optional<String> ret = Optional.empty();
        try {
            ret = Optional.of(URLUtils.getHost(urlString));
        } catch (MalformedURLException e) {
            System.err.println("Malformed URLUtils on " + urlString + " with message " + e.getMessage());
            exceptionMap.put(urlString, e);
        }
        return ret;
    }

    private void tryExec(String hostName, Runnable main, final Phaser phaser) {
        hostUsage.computeIfPresent(hostName, ((host, hostUse) -> {
            if (hostUse >= perHost) {
                run.add(new Pair<>(main, hostName));
            } else {
                phaser.register();
                hostUse++;
                downloadersPoll.submit(main);
            }
            return hostUse;
        }));
    }

    private void DownloadSite(String url, final Map<String, Boolean> urlUsed, final Map<String, IOException> exceptionMap, int depth) {
        Queue<Pair<Integer, List<String>>> queue = new ArrayDeque<>();
        queue.add(new Pair<>(depth, List.of(url)));

        while (!queue.isEmpty()) {
            Pair<Integer, List<String>> urlPair = queue.poll();
            List<String> urls = new CopyOnWriteArrayList<>();
            Phaser phaser = new Phaser(1);
            for (final String urlString : urlPair.getSecond()) {
                if (urlUsed.containsKey(urlString) || exceptionMap.keySet().contains(urlString)) {
                    continue;
                }
                urlUsed.put(urlString, true);
                Optional<String> host = getHost(urlString, exceptionMap);
                if (host.isPresent()) {
                    Runnable main = () -> {
                        try {
                            Document doc = downloader.download(urlString);
                            if (urlPair.getFirst() != 0) {
                                phaser.register();
                                extractorsPoll.submit(() -> {
                                    try {
                                        urls.addAll(doc.extractLinks());
                                    } catch (IOException e) {
                                        System.err.println("IOException on " + urlString + " with message " + e.getMessage());
                                        exceptionMap.put(urlString, e);
                                        urlUsed.remove(urlString);
                                    } finally {
                                        phaser.arrive();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            System.err.println("IOException on " + urlString + " with message " + e.getMessage());
                            exceptionMap.put(urlString, e);
                            urlUsed.remove(urlString);
                        }

                        hostUsage.computeIfPresent(host.get(), ((m_host, hostUse) -> {
                            hostUse--;
                            return hostUse;
                        }));


                        while (!run.isEmpty()) {
                            Pair<Runnable, String> runnable = null;
                            try {
                                runnable = run.poll(10, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException ignore) {
                            }
                            if (runnable != null) {
                                tryExec(runnable.getSecond(), runnable.getFirst(), phaser);
                            }
                        }
                        phaser.arrive();
                    };


                    if (!hostUsage.containsKey(host.get())) {
                        hostUsage.put(host.get(), 0);
                    }
                    tryExec(host.get(), main, phaser);
                }
            }

            phaser.arriveAndAwaitAdvance();

            if (urlPair.getFirst() != 0)
                queue.add(new Pair<>(urlPair.getFirst() - 1, urls));
        }
    }

    private static int checkArg(String st, String msg) throws IllegalArgumentException {
        try {
            return Integer.parseInt(st);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(msg);
        }
    }

    private static void checkArgs(final String[] args) throws IllegalArgumentException {
        if (args == null) {
            throw new IllegalArgumentException("Arguments are null");
        }
        for (final String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Argument is null");
            }
        }
        if (args.length != 5) {
            throw new IllegalArgumentException("Wrong argument count");
        }

    }

    /**
     * Download url recursively with given depth
     *
     * @param url   first link to download
     * @param depth depth of recursion needed
     * @return {@link Result} of all visited URLs
     */
    @Override
    public Result download(String url, int depth) {
        Map<String, Boolean> urlsSet = new ConcurrentHashMap<>();
        Map<String, IOException> exceptionMap = new ConcurrentHashMap<>();
        DownloadSite(url, urlsSet, exceptionMap, depth - 1);
        return new Result(new ArrayList<>(urlsSet.keySet()), exceptionMap);
    }

    /**
     * Closes all supporting threads
     */
    @Override
    public void close() {
        extractorsPoll.shutdownNow();
        downloadersPoll.shutdownNow();
    }

    /**
     * Launches downloader with parameters
     *
     * @param args url [depth [downloads [extractors [perHost]]]]
     */
    public static void main(String[] args) {
        try (WebCrawler wc = new WebCrawler(new CachingDownloader(),
                checkArg(args[2], "Wrong downloaders count argument " + args[2]),
                checkArg(args[3], "Wrong extractors count argument " + args[3]),
                checkArg(args[4], "Wrong perHost argument " + args[4]))) {

            checkArgs(args);

            wc.download(args[0], checkArg(args[1], "Wrong depth argument " + args[1]));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

}

