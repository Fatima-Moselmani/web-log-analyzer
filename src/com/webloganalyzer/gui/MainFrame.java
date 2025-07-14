package com.webloganalyzer.gui;

import com.webloganalyzer.model.LogEntry;
import com.webloganalyzer.service.*;
import com.webloganalyzer.SpeedupCSVWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class MainFrame extends JFrame {

    private JTextPane sequentialTextPane;
    private JTabbedPane parallelTabs;
    private JButton loadButton;
    private JButton showChartButton;
    private JComboBox<String> roleComboBox;
    private JLabel roleIconLabel;
    private JPanel topPanel;

    private long sequentialTimeMs = 0;
    private File loadedFile;

    private final Map<Integer, JTextPane> parallelPanes = new HashMap<>();
    private final Map<Integer, Boolean> parallelRunStatus = new HashMap<>();
    private final Map<Integer, Double> threadSpeedups = new HashMap<>();

    public MainFrame() {
        super("Web Log Analyzer");

        sequentialTextPane = createStyledPane();
        parallelTabs = new JTabbedPane();

        JScrollPane sequentialScroll = new JScrollPane(sequentialTextPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sequentialScroll, parallelTabs);
        splitPane.setResizeWeight(0.5);

        loadButton = new JButton("Load Log File");
        loadButton.addActionListener(e -> loadLogFile());

        showChartButton = new JButton("Show Speed-Up Chart");
        showChartButton.addActionListener(e -> {
            File dir = new File("results");
            if (!dir.exists()) dir.mkdirs();
            File csvFile = new File(dir, "speedup_results.csv");
            SpeedupCSVWriter.write(threadSpeedups, csvFile);

         // Sort the thread counts before adding to dataset
            List<Integer> sortedThreads = new ArrayList<>(threadSpeedups.keySet());
            Collections.sort(sortedThreads);

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Integer threads : sortedThreads) {
                dataset.addValue(threadSpeedups.get(threads), "Speedup", threads);
            }

            JFreeChart chart = ChartFactory.createLineChart(
                    "Thread Speedup",
                    "Threads",
                    "Speedup",
                    dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 600));

            JDialog dialog = new JDialog(this, "Speedup Chart", true);
            dialog.getContentPane().add(chartPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });

        roleComboBox = new JComboBox<>(new String[]{
                "System Administrator",
                "Web Developer",
                "Security Analyst"
        });
        roleComboBox.addActionListener(e -> updateStyleForRole());

        roleIconLabel = new JLabel();

        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(roleIconLabel);
        topPanel.add(new JLabel("Select Role: "));
        topPanel.add(roleComboBox);
        topPanel.add(loadButton);
        topPanel.add(showChartButton);

        this.setLayout(new BorderLayout());
        this.add(topPanel, BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);

        updateStyleForRole();

        this.setSize(1200, 700);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        parallelTabs.addChangeListener(e -> {
            int index = parallelTabs.getSelectedIndex();
            if (index <= 0 || loadedFile == null) return;

            String title = parallelTabs.getTitleAt(index);
            int threads = Integer.parseInt(title.split(" ")[0]);

            if (!parallelPanes.containsKey(threads)) return;

            if (!parallelRunStatus.getOrDefault(threads, false)) {
                runParallelAnalysis(threads);
            }
        });
    }

    private JTextPane createStyledPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return pane;
    }

    private void updateStyleForRole() {
        String role = (String) roleComboBox.getSelectedItem();
        if (role == null) return;

        Color bgColor;
        Color fgColor;
        String iconPath;

        switch (role) {
            case "System Administrator" -> {
                bgColor = new Color(25, 25, 112);
                fgColor = Color.WHITE;
                iconPath = "/icons/admin.png";
            }
            case "Web Developer" -> {
                bgColor = new Color(0, 100, 0);
                fgColor = Color.WHITE;
                iconPath = "/icons/dev.png";
            }
            case "Security Analyst" -> {
                bgColor = new Color(139, 0, 0);
                fgColor = Color.WHITE;
                iconPath = "/icons/security.png";
            }
            default -> {
                bgColor = Color.LIGHT_GRAY;
                fgColor = Color.BLACK;
                iconPath = null;
            }
        }

        topPanel.setBackground(bgColor);
        for (Component comp : topPanel.getComponents()) {
            comp.setForeground(fgColor);
            if (comp instanceof JLabel lbl) lbl.setForeground(fgColor);
        }
        loadButton.setBackground(fgColor);
        loadButton.setForeground(bgColor);
        showChartButton.setBackground(fgColor);
        showChartButton.setForeground(bgColor);
        roleComboBox.setBackground(fgColor);
        roleComboBox.setForeground(bgColor);

        if (iconPath != null) {
            try {
                roleIconLabel.setIcon(new ImageIcon(ImageIO.read(getClass().getResourceAsStream(iconPath))));
            } catch (IOException | NullPointerException e) {
                roleIconLabel.setIcon(null);
            }
        } else {
            roleIconLabel.setIcon(null);
        }
    }

    private void loadLogFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Log files", "log", "txt"));
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            loadedFile = chooser.getSelectedFile();

            sequentialTextPane.setText("");
            parallelTabs.removeAll();
            parallelPanes.clear();
            parallelRunStatus.clear();
            threadSpeedups.clear();

            processSequentialAndPrepareTabs(loadedFile);
        }
    }

    private void processSequentialAndPrepareTabs(File file) {
        parallelTabs.addTab("Select threads", new JPanel());
        parallelPanes.put(-1, null);
        parallelRunStatus.put(-1, true);

        int[] threadCounts = {4, 8, 12, 16};
        for (int threads : threadCounts) {
            JTextPane parallelTextPane = createStyledPane();
            JScrollPane scrollPane = new JScrollPane(parallelTextPane);
            parallelTabs.addTab(threads + " threads", scrollPane);
            appendToPane(parallelTextPane, "--- Not yet run --- Click this tab to run.", false, Color.GRAY, 14);
            parallelPanes.put(threads, parallelTextPane);
            parallelRunStatus.put(threads, false);
        }

        parallelTabs.setSelectedIndex(0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    LogProcessor sequentialProcessor = new SequentialLogProcessor();
                    Instant startSeq = Instant.now();
                    List<LogEntry> sequentialEntries = sequentialProcessor.processLogFile(file);
                    sequentialTimeMs = Duration.between(startSeq, Instant.now()).toMillis();

                    SwingUtilities.invokeLater(() -> {
                        appendToPane(sequentialTextPane, "--- SEQUENTIAL ANALYSIS ---", true, Color.BLUE, 16);
                        analyzeAndDisplay(sequentialEntries, sequentialTextPane);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            appendToPane(sequentialTextPane, "Error: " + e.getMessage(), true, Color.RED, 14));
                }
                return null;
            }
        };
        worker.execute();
    }

    private void runParallelAnalysis(int threads) {
        JTextPane pane = parallelPanes.get(threads);
        if (pane == null) return;

        pane.setText("");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    ParallelLogProcessor parallelProcessor = new ParallelLogProcessor(threads);
                    Instant startPar = Instant.now();
                    List<LogEntry> parallelEntries = parallelProcessor.processLogFile(loadedFile);
                    long parallelTime = Duration.between(startPar, Instant.now()).toMillis();
                    double speedup = (double) sequentialTimeMs / Math.max(parallelTime, 1);

                    threadSpeedups.put(threads, speedup);

                    SwingUtilities.invokeLater(() -> {
                        appendToPane(pane, "--- PARALLEL ANALYSIS with " + threads + " threads ---", true, new Color(0, 128, 0), 16);
                        appendToPane(pane, "Time: " + parallelTime + " ms", false, Color.BLACK, 14);
                        appendToPane(pane, "Speed-up: " + String.format("%.2fx", speedup), false, Color.BLACK, 14);
                        analyzeAndDisplay(parallelEntries, pane);
                    });

                    parallelRunStatus.put(threads, true);

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            appendToPane(pane, "Error: " + e.getMessage(), true, Color.RED, 14));
                    e.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void analyzeAndDisplay(List<LogEntry> entries, JTextPane pane) {
        appendToPane(pane, "Total requests: " + entries.size(), true, Color.BLACK, 14);
        String selectedRole = (String) roleComboBox.getSelectedItem();
        if (selectedRole == null) selectedRole = "System Administrator";

        switch (selectedRole) {
            case "System Administrator" -> doSystemAdminAnalysis(entries, pane);
            case "Web Developer" -> doWebDeveloperAnalysis(entries, pane);
            case "Security Analyst" -> doSecurityAnalystAnalysis(entries, pane);
        }
    }
    private void doSystemAdminAnalysis(List<LogEntry> entries, JTextPane pane) {
        Set<String> uniqueIps = new HashSet<>();
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger failedLoginCount = new AtomicInteger(0);
        Map<String, Integer> ipRequestCounts = new HashMap<>();

        for (LogEntry e : entries) {
            uniqueIps.add(e.getIpAddress());
            if (e.getStatusCode() >= 500) errorCount.incrementAndGet();
            ipRequestCounts.merge(e.getIpAddress(), 1, Integer::sum);

            if ("POST".equalsIgnoreCase(e.getRequestMethod())
                    && e.getResource().toLowerCase().contains("/login")
                    && (e.getStatusCode() == 401 || e.getStatusCode() == 403)) {
                failedLoginCount.incrementAndGet();
            }
        }

        List<String> suspiciousIps = ipRequestCounts.entrySet().stream()
                .filter(e -> e.getValue() > 100)
                .map(Map.Entry::getKey)
                .toList();

        appendToPane(pane, "\nUnique IPs: " + uniqueIps.size(), false, Color.BLUE, 13);
        appendToPane(pane, "Server errors (500s): " + errorCount.get(), false, Color.RED, 13);
        appendToPane(pane, "Failed login attempts: " + failedLoginCount.get(), false, Color.ORANGE, 13);

        appendToPane(pane, "Suspicious IPs (>100 requests):", true, Color.MAGENTA, 13);
        if (suspiciousIps.isEmpty()) {
            appendToPane(pane, "None detected", false, Color.GRAY, 12);
        } else {
            suspiciousIps.forEach(ip ->
                    appendToPane(pane, " - " + ip, false, Color.MAGENTA, 12));
        }

        JButton chartButton = new JButton("Show System Admin Chart");
        chartButton.addActionListener(ev -> {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            ipRequestCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)  // ✅ Only top 10 IPs
                .forEach(entry -> dataset.addValue(entry.getValue(), "Requests", entry.getKey()));

            JFreeChart chart = ChartFactory.createBarChart(
                    "Top 10 IPs by Requests",
                    "IP Address",
                    "Requests",
                    dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 600));

            JDialog dialog = new JDialog(this, "System Admin Chart", true);
            dialog.getContentPane().add(chartPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
        pane.insertComponent(chartButton);
    }

    private void doWebDeveloperAnalysis(List<LogEntry> entries, JTextPane pane) {
        Map<String, Integer> methodCounts = new HashMap<>();
        Map<String, Integer> urlCounts = new HashMap<>();
        Map<String, Integer> errorPerResource = new HashMap<>();

        for (LogEntry e : entries) {
            methodCounts.merge(e.getRequestMethod(), 1, Integer::sum);
            urlCounts.merge(e.getResource(), 1, Integer::sum);

            if (e.getStatusCode() >= 400) {
                errorPerResource.merge(e.getResource(), 1, Integer::sum);
            }
        }

        appendToPane(pane, "\nRequest counts by HTTP method:", true, Color.BLUE, 13);
        methodCounts.forEach((method, count) ->
                appendToPane(pane, " - " + method + ": " + count, false, Color.BLACK, 12));

        appendToPane(pane, "\nTop 5 URLs:", true, Color.BLUE, 13);
        urlCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e ->
                        appendToPane(pane, " - " + e.getKey() + ": " + e.getValue(), false, Color.BLACK, 12));

        appendToPane(pane, "\nResources with errors (4xx/5xx):", true, Color.RED, 13);
        if (errorPerResource.isEmpty()) {
            appendToPane(pane, "None", false, Color.GRAY, 12);
        } else {
            errorPerResource.forEach((res, count) ->
                    appendToPane(pane, " - " + res + ": " + count, false, Color.RED, 12));
        }
        JButton chartButton = new JButton("Show Web Dev Chart");
        chartButton.addActionListener(ev -> {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            methodCounts.forEach((method, count) -> {
                dataset.addValue(count, "Count", method);
            });

            JFreeChart chart = ChartFactory.createBarChart(
                    "HTTP Methods Count",
                    "Method",
                    "Count",
                    dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 600));

            JDialog dialog = new JDialog(this, "Web Developer Chart", true);
            dialog.getContentPane().add(chartPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });

        pane.insertComponent(chartButton);
    }

    private void doSecurityAnalystAnalysis(List<LogEntry> entries, JTextPane pane) {
        Set<String> sensitiveEndpoints = new HashSet<>();
        Set<String> suspiciousMethods = new HashSet<>();

        for (LogEntry e : entries) {
            if (e.getResource().toLowerCase().contains("/admin")
                    || e.getResource().toLowerCase().contains("/login")) {
                sensitiveEndpoints.add(e.getResource());
            }

            if (Set.of("DELETE", "PUT", "TRACE").contains(e.getRequestMethod().toUpperCase())) {
                suspiciousMethods.add(e.getRequestMethod() + " " + e.getResource());
            }
        }

        appendToPane(pane, "\nSensitive endpoints accessed:", true, Color.RED, 13);
        if (sensitiveEndpoints.isEmpty()) {
            appendToPane(pane, "None", false, Color.GRAY, 12);
        } else {
            sensitiveEndpoints.forEach(ep ->
                    appendToPane(pane, " - " + ep, false, Color.RED, 12));
        }

        appendToPane(pane, "\nSuspicious HTTP methods:", true, Color.ORANGE, 13);
        if (suspiciousMethods.isEmpty()) {
            appendToPane(pane, "None", false, Color.GRAY, 12);
        } else {
            suspiciousMethods.forEach(m ->
                    appendToPane(pane, " - " + m, false, Color.ORANGE, 12));
        }
        JButton chartButton = new JButton("Show Security Chart");
        chartButton.addActionListener(ev -> {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            sensitiveEndpoints.forEach(ep -> {
                dataset.addValue(1, "Sensitive", ep);
            });
            suspiciousMethods.forEach(m -> {
                dataset.addValue(1, "Suspicious", m);
            });

            JFreeChart chart = ChartFactory.createBarChart(
                    "Security Events",
                    "Event",
                    "Count",
                    dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 600));

            JDialog dialog = new JDialog(this, "Security Analyst Chart", true);
            dialog.getContentPane().add(chartPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });


        pane.insertComponent(chartButton);
    }

    private void appendToPane(JTextPane pane, String text, boolean bold, Color color, int fontSize) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("Style", null);
        StyleConstants.setBold(style, bold);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontSize(style, fontSize);
        try {
            doc.insertString(doc.getLength(), text + "\n", style);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}