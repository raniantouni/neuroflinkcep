import core.parser.dictionary.INFOREDictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the INFOREDictionary constructor that
 * understands the “sites as array / nested platforms” schema.
 * Tested with JUnit-Jupiter 5.7
 */
class INFOREDictionaryTest {

    private INFOREDictionary dictionary;     // fresh instance for each test

    @BeforeEach
    void setUp() throws IllegalAccessException {

        String json;
        try (InputStream in = getClass().getResourceAsStream("/test-dictionary.json")) {
            assertNotNull(in);
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (json.isBlank()) throw new IllegalAccessException("JSON content is empty");

        dictionary = assertDoesNotThrow(() -> new INFOREDictionary(json),
                "Constructor should not throw");
    }

    /* ------------------------------------------------------------------ */
    /*  Top-level attributes                                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Dictionary name is parsed")
    void testDictionaryName() {
        assertEquals("dict_3_2", dictionary.getName());
    }

    @Test
    @DisplayName("Operator key present and basic fields parsed")
    void testOperatorBasics() {
        String key = "streaming:map";
        assertTrue(dictionary.getCostCoefficients().containsKey(key));
        assertTrue(dictionary.getInputRate().containsKey(key));

        assertEquals(100, dictionary.getInputRate().get(key));
        assertIterableEquals(
                List.of(0, 1, 0, 0),
                dictionary.getCostCoefficients().get(key),
                "Cost-coefficient vector differs");
    }

    /* ------------------------------------------------------------------ */
    /*  Site-level parsing                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Site static costs and migration costs parsed")
    void testSites() {
        var siteStatic = dictionary.getOperatorSiteStaticCosts().get("streaming:map");
        var siteMigrations = dictionary.getOperatorSiteMigrationCosts().get("streaming:map");

        assertEquals(Set.of("site_0", "site_1"), siteStatic.keySet());
        assertEquals(10, siteStatic.get("site_0"));
        assertEquals(20, siteStatic.get("site_1"));

        assertEquals(Map.of("site_0", 0, "site_1", 1000), siteMigrations.get("site_0"));
        assertEquals(Map.of("site_0", 1000, "site_1", 0), siteMigrations.get("site_1"));
    }

    /* ------------------------------------------------------------------ */
    /*  Platform-level parsing                                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Platform data nested inside sites is parsed")
    void testPlatforms() {
        var names = dictionary.getOperatorPlatformOperatorNames().get("streaming:map");
        var statics = dictionary.getOperatorPlatformStaticCosts().get("streaming:map");
        var migrations = dictionary.getOperatorPlatformMigrationCosts().get("streaming:map");

        assertEquals(Set.of("platform_0", "platform_1"), names.keySet());

        assertEquals("platform_0_streaming:map", names.get("platform_0"));
        assertEquals("platform_1_streaming:map", names.get("platform_1"));

        assertEquals(20, statics.get("platform_0"));
        assertEquals(20, statics.get("platform_1"));

        assertEquals(Map.of("platform_0", 500, "platform_1", 0), migrations.get("platform_0"));
        assertEquals(Map.of("platform_0", 500, "platform_1", 0), migrations.get("platform_1"));
    }

    @Test
    @DisplayName("Operator implementations are parsed correctly")
    void testOperatorImplementations() {
        Map<String, List<String>> implementations = dictionary.getImplementationsForClassKey("streaming:map");

        for (Map.Entry<String, List<String>> entry : implementations.entrySet()) {
            String site = entry.getKey();
            List<String> platforms = entry.getValue();
            System.out.println("Site: " + site + ", Platforms: " + platforms);
        }

        // Check that platforms 0 is available on site 0 and both platforms are available on site 1
        assertTrue(implementations.containsKey("site_0"), "Site 0 should be present");
        assertTrue(implementations.containsKey("site_1"), "Site 1 should be present");
        assertEquals(List.of("platform_0"), implementations.get("site_0"),
                "Site 0 should have only platform_0 available");
        assertEquals(List.of("platform_0", "platform_1"), implementations.get("site_1"),
                "Site 1 should have both platforms available");

    }
}
