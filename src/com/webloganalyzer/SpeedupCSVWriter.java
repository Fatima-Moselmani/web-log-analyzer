package com.webloganalyzer;

import java.io.*;
import java.util.*;

public class SpeedupCSVWriter {

    public static void write(Map<Integer, Double> threadSpeedups, File outputFile) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            pw.println("Threads,Speedup");
            threadSpeedups.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> pw.printf("%d,%.4f%n", entry.getKey(), entry.getValue()));
            System.out.println("✅ Speedup results written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}