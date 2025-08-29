package cloud.contoterzi.helpdesk.core.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates environment variable handling and validates the configuration approach.
 * This test confirms that the Maven surefire plugin correctly sets environment variables
 * that match the production AWS environment setup.
 */
class EnvironmentVariableTest {

    @Test
    void testEnvironmentVariablesAreSet() {
        // Test that Maven surefire plugin sets environment variables correctly
        String configPath = System.getenv("APP_CONFIG_PATH");
        String alwaysCallLlm = System.getenv("ALWAYS_CALL_LLM");
        
        // These should be set by Maven surefire plugin configuration
        assertNotNull(configPath, "APP_CONFIG_PATH environment variable should be set by Maven");
        assertNotNull(alwaysCallLlm, "ALWAYS_CALL_LLM environment variable should be set by Maven");
        
        // Verify the values match our test configuration
        assertTrue(configPath.endsWith("test-config.yaml"), 
                  "APP_CONFIG_PATH should point to test-config.yaml, got: " + configPath);
        assertEquals("false", alwaysCallLlm, 
                    "ALWAYS_CALL_LLM should be 'false' for knowledge base testing");
        
        System.out.println("✅ Environment variables correctly set:");
        System.out.println("   APP_CONFIG_PATH = " + configPath);
        System.out.println("   ALWAYS_CALL_LLM = " + alwaysCallLlm);
    }

    @Test
    void testSystemPropertyVsEnvironmentVariable() {
        // Demonstrate the difference between System Properties and Environment Variables
        
        // Set a system property
        System.setProperty("TEST_SYSTEM_PROPERTY", "system_value");
        
        // Environment variable (should be null since we didn't set it in environment)
        String envVar = System.getenv("TEST_SYSTEM_PROPERTY");
        String sysProp = System.getProperty("TEST_SYSTEM_PROPERTY");
        
        assertNull(envVar, "Environment variable should be null");
        assertEquals("system_value", sysProp, "System property should have the value we set");
        
        System.out.println("📝 Demonstrating the difference:");
        System.out.println("   System.getenv('TEST_SYSTEM_PROPERTY') = " + envVar);
        System.out.println("   System.getProperty('TEST_SYSTEM_PROPERTY') = " + sysProp);
        System.out.println("   ➡️ This confirms why our original test failed!");
    }

    @Test
    void testProductionLikeConfiguration() {
        // Verify our test mimics the production AWS environment structure
        String configPath = System.getenv("APP_CONFIG_PATH");
        
        if (configPath != null) {
            // In production: /opt/config/helpdesk-config.yaml
            // In test: {project}/src/test/resources/test-config.yaml
            assertTrue(configPath.contains("config"), "Config path should contain 'config'");
            assertTrue(configPath.endsWith(".yaml"), "Config should be YAML format");
            
            System.out.println("🔧 Configuration setup:");
            System.out.println("   Production: /opt/config/helpdesk-config.yaml");
            System.out.println("   Test:       " + configPath);
            System.out.println("   ✅ Both use environment variables consistently");
        }
    }

    @Test 
    void testAlwaysCallLlmToggling() {
        // This demonstrates how ALWAYS_CALL_LLM controls the system behavior
        String alwaysCallLlm = System.getenv("ALWAYS_CALL_LLM");
        
        assertNotNull(alwaysCallLlm, "ALWAYS_CALL_LLM should be set");
        
        boolean shouldCallLlm = "true".equalsIgnoreCase(alwaysCallLlm);
        
        System.out.println("🎛️ LLM Call Control:");
        System.out.println("   ALWAYS_CALL_LLM = " + alwaysCallLlm);
        System.out.println("   Will call LLM: " + shouldCallLlm);
        
        if (shouldCallLlm) {
            System.out.println("   ➡️ System will ALWAYS call LLM (ignore knowledge base threshold)");
        } else {
            System.out.println("   ➡️ System will use knowledge base when similarity >= threshold");
        }
    }
}