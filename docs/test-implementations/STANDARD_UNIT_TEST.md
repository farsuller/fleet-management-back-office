# Backoffice Portal Testing Standards (Kotlin Frontend & Business Logic)

These standards focus on **Pure Business Logic (Unit Tests)** and **Frontend Component Testing**, leveraging Kotlin Multiplatform (KMP) to ensure consistent validation and domain rules across the entire stack. Do not use wildcard imports in your tests.

---

## 1. Business Logic & Domain Unit Testing
**Goal:** High-speed verification of domain invariants and business rules without infrastructure overhead.

* **Pure JVM/JS Environment:** Tests must be deterministic with zero I/O, database, or framework engine dependencies.
* **Framework Stack:** Use **JUnit 5** as the runner, **MockK** for stubbing/verification, and **AssertJ** for fluent assertions.
* **Dependency Isolation:** Mock only direct internal dependencies (e.g., domain services, validators).
* **Strict Mocking Rules:**
    * Avoid generic matchers like `any()`; use exact values for stubs to prevent regression.
    * Use `slot` or `ArgumentCaptor` to inspect objects created internally and verify fields with AssertJ.
* **Domain Invariants:** Ensure 100% coverage of core logic rules and state transitions.
* **Naming Convention:** Follow the `should[ExpectedBehavior]_When[Scenario]` pattern.

---

## 2. Frontend Component Testing (Kotlin Web/Compose)
**Goal:** Verify UI state management and user interaction logic within the Backoffice Portal.

* **HttpClient Mocking:** Use Ktor's `MockEngine` to intercept network calls, testing UI components against predefined JSON responses without a live backend.
* **Component-Level Tests:**
    * Verify DOM element visibility and state (e.g., buttons being enabled/disabled based on current context).
    * Ensure the frontend correctly renders data received through shared DTOs.
* **State Management Verification:** Test that the UI transitions correctly between defined states based on internal state store updates.
* **Shared Logic Validation:** Use KMP to test validation logic (e.g., input formats) once in the shared module to ensure consistent behavior in the browser.

---

## 3. The Unified AAA Pattern (Strict)
All tests, whether for a frontend component or a business service, must follow the **AAA Pattern** with explicit comments:

1. **// Arrange:** Set up the initial state, initialize mocks, and define stub behaviors.
2. **// Act:** Execute the specific business function or trigger the UI event/action.
3. **// Assert:** Perform comprehensive assertions using AssertJ to verify the final state or output.

---

## 4. Summary of Key Library Alignment

| Testing Type | Recommended Library | Purpose |
| :--- | :--- | :--- |
| **Test Runner** | **JUnit 5** | Standard execution for JVM/JS tests. |
| **Mocking** | **MockK** | Mocking dependencies with native Coroutine support. |
| **Assertions** | **AssertJ** | Fluent, readable assertions for complex objects. |
| **API Interception** | **Ktor MockEngine** | Testing frontend API integration without a real server. |