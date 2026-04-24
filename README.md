# 🏆 SRM Quiz Leaderboard System

## 📖 Project Overview
The **SRM Quiz Leaderboard System** is a robust, production-ready backend integration designed to process real-time quiz show events from a validator API. The system fetches paginated event data across a strict 10-poll constraint, intelligently deduplicates overlapping telemetry using a composite key (`roundId + participant`), safely aggregates final scores, and idempotently submits the generated JSON leaderboard payload to the final POST endpoint.

## 🏗 Architecture & Tech Stack
The application is strictly designed following **Clean Architecture** principles to separate the underlying data models, the HTTP communication protocols, and the core business logic.

* **Language**: Java 17+
* **Build Tool**: Apache Maven
* **HTTP Client**: Built-in `java.net.http.HttpClient`
* **JSON Processing**: Jackson (`com.fasterxml.jackson`)
* **Logging**: SLF4J with Logback
* **Testing**: JUnit 5 & Mockito

## 🚀 Enterprise Features & Resiliency
This project goes significantly beyond basic API consumption by introducing several fault-tolerant and developer-experience improvements:

* **Idempotency Protection & Dry-Run Mode**: Built an internal `isDryRun` execution flag to safely evaluate the polling constraints, deduplication logic, and aggregation math without prematurely burning the strict single-use submission token on the live validator server.
* **Network Resiliency (Retry with Backoff)**: Implemented a custom retry mechanism. If the external API gateway throws `5xx` errors (like `502` or `503`) under heavy load, the HTTP client automatically waits and retries the specific poll index up to 4 times to ensure 0% data loss.
* **Professional Console UI**: Replaced raw JSON console dumps with a dynamically generated, perfectly padded ASCII leaderboard table, offering clean, professional observability directly inside standard terminal logs.
* **Clean Architecture & Logging**: Structured the codebase with a clear separation of concerns (Models, Service, Client). Replaced standard output prints with `SLF4J/Logback` for professional, timestamped runtime monitoring and granular log levels.
* **Unit Testing**: Integrated `JUnit 5` and `Mockito` to mathematically prove the composite key deduplication rules and descending sort logic operate flawlessly without relying on live network conditions.

## ⚙️ How to Run

### 1. Build and Compile
Download the dependencies and compile the source code:
```bash
mvn clean compile
```

### 2. Run the Application
Execute the main program (configured out-of-the-box to use the safe Dry-Run mode):
```bash
mvn exec:java
```

### 3. Run the Test Suite
Validate the deduplication and sorting algorithms against the mocked HTTP layers:
```bash
mvn test
```
