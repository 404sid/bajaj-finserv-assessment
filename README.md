# 🏆 SRM Quiz Leaderboard System
**Registration Number:** RA2311003010935

## 📖 Overview
The **SRM Quiz Leaderboard System** is a production-ready backend integration designed to process real-time quiz show events from a validator API. The system fetches paginated event data across a strict 10-poll constraint, intelligently deduplicates overlapping telemetry using a composite key (`roundId + participant`), safely aggregates final scores, and idempotently submits the generated JSON leaderboard payload to the final POST endpoint.

## 🚀 Key Features & Resiliency
This project goes significantly beyond basic API consumption by introducing several fault-tolerant and developer-experience improvements:

* **Network Resiliency (Retry with Backoff)**: Implemented a custom retry mechanism. If the external API gateway throws `5xx` errors (like `502` or `503`) under heavy load, the HTTP client automatically waits 2 seconds and retries the specific poll index up to 4 times to ensure 0% data loss.
* **Idempotency Protection & Dry-Run Mode**: Built an internal execution flag (`isDryRun`) to safely evaluate the polling constraints, deduplication logic, and aggregation math without prematurely burning the strict single-use submission token on the live validator server.
* **Professional Console UI**: Replaced raw JSON console dumps with a dynamically generated, perfectly aligned ASCII leaderboard table, offering clean observability directly inside standard terminal logs.
* **Intelligent Deduplication**: Uses an `O(1)` memory-efficient `HashSet` lookup against a composite key (`roundId-participant`) to strictly guarantee duplicated data payload drops.

## ⚙️ How to Run

By default, the application runs in **Live Mode** (`isDryRun = false`), meaning it will fetch from the API and execute the final HTTP POST submission.

### 1. Build and Run (Live Mode)
Download the dependencies, compile the source code, and execute the leaderboard sequence:
```bash
mvn clean compile exec:java
```

### 2. Testing Safely (Dry-Run Mode)
To safely run the code without triggering an idempotency lockout on the validator server, open `src/main/java/com/quiz/Main.java` and toggle the boolean flag:
```java
boolean isDryRun = true;
```
Then execute `mvn clean compile exec:java`. The application will perform all network polls, compute the deduplication, print the ASCII UI, and log the JSON payload—but it will skip the final POST execution.

### 3. Run the Test Suite
Validate the core logic locally using Mockito:
```bash
mvn test
```

## 🏗 Technical Stack
* **Language**: Java 17+
* **Build Tool**: Apache Maven
* **HTTP Client**: Built-in `java.net.http.HttpClient`
* **JSON Processing**: Jackson (`com.fasterxml.jackson`)
* **Logging**: SLF4J with Logback
* **Testing**: JUnit 5 & Mockito
