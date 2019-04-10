package ru.ifmo.rain.lundin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private int perHost;
    private final ExecutorService downloadersPoll;
    private final ExecutorService extractorsPoll;


    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloadersPoll = Executors.newFixedThreadPool(downloaders);
        this.extractorsPoll = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private Optional<String> getHost(String urlString, final Map<String, IOException> exceptionMap) {
        Optional<String> ret = Optional.empty();
        try {
            ret = Optional.of(URLUtils.getHost(urlString));
        } catch (MalformedURLException e) {
            exceptionMap.put(urlString, e);
        }
        return ret;
    }

    private void DownloadSite(String urlString, final Set<String> urlUsed, final Map<String, IOException> exceptionMap, int depth, final Phaser phaser) {
        if (urlUsed.contains(urlString) || exceptionMap.keySet().contains(urlString)) {
            return;
        }
        urlUsed.add(urlString);

        if (getHost(urlString, exceptionMap).isPresent()) {
            phaser.register();
            downloadersPoll.submit(() -> {
                try {
                    Document doc = downloader.download(urlString);
                    phaser.register();
                    extractorsPoll.submit(() -> {
                        try {
                            if (depth != 0) {
                                for (String link : doc.extractLinks()) {
                                    DownloadSite(link, urlUsed, exceptionMap, depth - 1, phaser);
                                }
                            }
                        } catch (IOException e) {
                            exceptionMap.put(urlString, e);
                            urlUsed.remove(urlString);
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (IOException e) {
                    exceptionMap.put(urlString, e);
                    urlUsed.remove(urlString);

                } finally {
                    phaser.arrive();
                }
            });
        }
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> urlsSet = new ConcurrentSkipListSet<>();
        Map<String, IOException> exceptionMap = new ConcurrentHashMap<>();
        Phaser phaser = new Phaser(1);
        DownloadSite(url, urlsSet, exceptionMap, depth - 1, phaser);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(urlsSet), exceptionMap);
    }

    @Override
    public void close() {
        extractorsPoll.shutdownNow();
        downloadersPoll.shutdownNow();
    }
}
