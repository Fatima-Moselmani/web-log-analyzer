package com.webloganalyzer.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LogEntry {
    private String ipAddress;
    private OffsetDateTime timestamp;
    private String requestMethod;
    private String resource;
    private int statusCode;
    private int bytes;

    public LogEntry(String ipAddress, OffsetDateTime timestamp, String requestMethod,
                    String resource, int statusCode, int bytes) {
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.requestMethod = requestMethod;
        this.resource = resource;
        this.statusCode = statusCode;
        this.bytes = bytes;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getResource() {
        return resource;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getBytes() {
        return bytes;
    }

    public static LogEntry parseFromNasaLogLine(String line) {
        try {
            String regex = "^(\\S+) \\S+ \\S+ \\[(.+?)\\] \"(\\S+) (\\S+) \\S+\" (\\d{3}) (\\S+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
            java.util.regex.Matcher matcher = pattern.matcher(line);

            if (!matcher.find()) {
                return null;
            }

            String ip = matcher.group(1);
            String dateStr = matcher.group(2);
            String method = matcher.group(3);
            String resource = matcher.group(4);
            int status = Integer.parseInt(matcher.group(5));
            String bytesStr = matcher.group(6);
            int bytes = bytesStr.equals("-") ? 0 : Integer.parseInt(bytesStr);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
            OffsetDateTime timestamp = OffsetDateTime.parse(dateStr, formatter);

            return new LogEntry(ip, timestamp, method, resource, status, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("%s [%s] \"%s %s\" %d %d",
                ipAddress, timestamp, requestMethod, resource, statusCode, bytes);
    }
}