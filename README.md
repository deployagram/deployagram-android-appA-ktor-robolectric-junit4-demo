# Deployagram Android AppA Ktor Robolectric JUnit 4 Demo

The same small Android app as the other AppA demos, with its HTTP calls made by **Ktor** instead of hand-written OkHttp
code. AppA has one screen. The user types an email and presses **Recommend book**, and the app asks BookOverflow for a book
suggestion.

The screen is the View-based one from the Robolectric demo, so the only thing that changes is the HTTP client. The tests run
on the JVM with **Robolectric** in **JUnit 4**, and need no emulator.

## What it does

AppA calls `GET /BookOverflow/suggestion?email=<email>` through a Ktor client, with the OkHttp engine underneath. It shows the
answer:

| Situation                                      | What the screen shows                                        |
|------------------------------------------------|--------------------------------------------------------------|
| BookOverflow suggests a book                   | The book's name, edition, format and authors                 |
| Email is blank, and BookOverflow refuses (400) | `You must enter a valid email!`                              |
| Any other failure                              | `Sorry, no suggestion right now (<reason>)`                  |

## What it is for

* show how an app calls a service with **Ktor**, and how a test stands in for that service with Ktor's **MockEngine**;
* replace the client's engine in the test, so no network or HTTP server is used;
* check what the app sent, and what the screen shows, from a JVM test.

The test never sends an HTTP request. It types an email and presses the button, and AppA makes the request.

## Tech

* Kotlin 2.1.21 with the serialization plugin, Android Gradle Plugin 8.9.0, Gradle 8.12 (checked-in wrapper)
* Java 17 to build the app, and Java 21 to run the tests (Robolectric's Android 16 runtime needs Java 21)
* Min SDK 26, compile and target SDK 36
* Ktor client 3.1.3 with the OkHttp engine, content negotiation and kotlinx.serialization JSON
* Robolectric 4.18.1, JUnit 4.13.2, Espresso 3.6.1, AndroidX Test core 1.6.1
* Ktor's `ktor-client-mock` 3.1.3 (MockEngine) in the tests, in place of MockWebServer

## Run the tests

```bash
./gradlew testDebugUnitTest
```

The test report is in `app/build/reports/tests/testDebugUnitTest/index.html`.

## Project layout

```
app/src/main/java/com/deployagram/demo/appa/
    MainActivity.kt          the one screen: email field, button, result
    BookOverflowClient.kt    GET /BookOverflow/suggestion, with a Ktor client
    BookSuggestion.kt        the suggestion, serialised with kotlinx.serialization
    BookOverflowConfig.kt    where BookOverflow is, and the engine that sends requests (the test changes both)
app/src/test/java/com/deployagram/demo/appa/
    RecommendBookScreenTest.kt   the two JVM tests, and the MockEngine double for BookOverflow
```

## Deployagram instrumentation

The tests are instrumented with two Deployagram packages:

* `deployagram-junit4`: `DeployagramRule`, a JUnit 4 rule that ends each test's sequence diagram in the Collector. It is
  declared on the test class, because Robolectric runs on the JVM and has no run-level listener to register.
* `deployagram-mockwebserver`: `RecordingDispatcher`, which wraps the BookOverflow double. It records the request AppA
  sends through Ktor, and the answer BookOverflow gives.

The Collector runs on the host, and the tests reach it at `http://localhost:1152`. The caller and callee are named as in
the Java repo: `AliceA` calls `ORG_ctm_01m1j08sx96792d2tysabvyadp:BookOverflow`.

The Deployagram packages are on Maven Central, so the build needs no local install. Start the Collector, with the licence
it needs, and run:

```bash
./gradlew testDebugUnitTest
```

Each test's entries are under its own marker, `RecommendBookScreenTest.<test name>`.
