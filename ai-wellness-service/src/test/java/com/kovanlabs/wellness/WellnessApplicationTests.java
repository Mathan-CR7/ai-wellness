package com.kovanlabs.wellness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 1 smoke test — verifies that the Spring application context loads
 * successfully with the test profile (H2 in-memory database, mocked AI).
 *
 * <p>If this test passes:
 * <ul>
 *   <li>All Spring beans wire correctly.</li>
 *   <li>Configuration properties are bound without errors.</li>
 *   <li>No circular dependencies exist.</li>
 *   <li>Security configuration loads correctly.</li>
 * </ul>
 * </p>
 *
 * <p><b>No business data is asserted here</b> — this is purely an
 * infrastructure/wiring test.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 1 — Application Context Smoke Test")
class WellnessApplicationTests {

    @Test
    @DisplayName("Spring application context loads successfully")
    void contextLoads() {
        // If the context fails to load, this test throws an exception
        // and fails with a clear Spring error message.
        // No assertions needed — context load is the test.
    }
}
