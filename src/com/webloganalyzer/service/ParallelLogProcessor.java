package com.webloganalyzer.service;

import com.webloganalyzer.model.LogEntry;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelLogProcessor implements LogProcessor {

    private final int threadCount;

    public ParallelLogProcessor(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public List<LogEntry> processLogFile(File logFile) throws Exception {
        System.out.println("=== ParallelLogProcessor starting ===");
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Requested threads: " + threadCount);

        List<String> allLines = Files.readAllLines(logFile.toPath(), StandardCharsets.ISO_8859_1);
        int totalLines = allLines.size();
        System.out.println("Total lines in log file: " + totalLines);

        int chunkSize = Math.max(1, (int) Math.ceil((double) totalLines / threadCount));
        System.out.println("Calculated chunk size: " + chunkSize + " lines per thread.");

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        executor.prestartAllCoreThreads();  // warm up all threads now
        System.out.println("All " + threadCount + " threads prestarted.");

        List<Future<List<LogEntry>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int start = i * chunkSize;
            final int end = Math.min(start + chunkSize, totalLines);

            if (start >= end) {
                System.out.println("Worker " + i + ": no work assigned (start >= end), skipping.");
                continue;
            }

            System.out.println("Worker " + i + ": processing lines [" + start + " - " + (end - 1) + "]");

            List<String> chunk = allLines.subList(start, end);

            Callable<List<LogEntry>> task = () -> {
                List<LogEntry> localList = new ArrayList<>();
                for (String line : chunk) {
                    LogEntry entry = LogEntry.parseFromNasaLogLine(line);
                    if (entry != null) {
                        localList.add(entry);
                    }
                }
                return localList;
            };

            futures.add(executor.submit(task));
        }

        List<LogEntry> allEntries = new ArrayList<>();
        for (Future<List<LogEntry>> future : futures) {
            List<LogEntry> partialResult = future.get();  // blocks until each task completes
            allEntries.addAll(partialResult);
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);
        if (!finished) {
            System.err.println("Executor did not shut down cleanly within timeout.");
        } else {
            System.out.println("Executor shut down cleanly.");
        }

        System.out.println("=== ParallelLogProcessor completed ===");
        return allEntries;
    }
}