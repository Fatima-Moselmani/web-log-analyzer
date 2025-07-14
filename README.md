# Web Log Analyzer

A Java-based desktop application that analyzes large web server log files (e.g., NASA logs) using both **sequential and parallel processing**. It displays statistics for different user roles (System Administrator, Web Developer, Security Analyst) and visualizes speedup results using charts.

---

## 📌 Features

- Analyze NASA web log files line-by-line.
- Role-specific analysis for:
  - System Administrator
  - Web Developer
  - Security Analyst
- Parallel processing using multithreading (ForkJoinPool).
- Speedup chart generation (JFreeChart).
- GUI built with Java Swing.
- Supports log files selected at runtime.

---

## 🛠️ Technologies Used

- **Java 17**
- **Swing** (GUI)
- **JFreeChart** (for visual charts)
- **ForkJoinPool** (for parallelism)
- **Git & GitHub** (version control)

---

## 🚀 How to Run

1. Open the project in **Eclipse** or **IntelliJ**.
2. Run `App.java` (in `com.webloganalyzer` package).
3. Select a `.log` or `.txt` file when prompted.
4. View analysis results and speedup chart in the GUI.

> 💡 **Note**: The large NASA log file (`access_log_Jul95.txt`) is excluded from this repository due to GitHub file size limits. You can download it separately from [NASA Logs Dataset](https://ita.ee.lbl.gov/html/contrib/NASA-HTTP.html).

---

## 📊 Speedup Results Example

The application measures and compares sequential vs. parallel processing times, and plots a **speedup chart** based on thread count (4, 8, 12, 16).

---

## 👩‍💻 Team Members

- **Fatima Moselmani**
- **Fatima Jawad**

---

## 📄 License

MIT License (or per course/instructor policy)

