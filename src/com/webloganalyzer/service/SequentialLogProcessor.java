package com.webloganalyzer.service;

import com.webloganalyzer.model.LogEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SequentialLogProcessor implements LogProcessor {
    @Override
    public List<LogEntry> processLogFile(File logFile) throws Exception {
        List<LogEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry entry = LogEntry.parseFromNasaLogLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }
}