package com.webloganalyzer.service;

import com.webloganalyzer.model.LogEntry;

import java.io.File;
import java.util.List;

public interface LogProcessor {
    List<LogEntry> processLogFile(File logFile) throws Exception;
}