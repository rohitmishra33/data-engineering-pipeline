import com.blurr.pipeline.config.IngestionConfig;
import com.blurr.pipeline.core.DataProcessor;
import com.blurr.pipeline.models.DataBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDataProcessor {

    // Assuming MAX_DISCOUNT_PERCENT is a constant
    private static final double MAX_DISCOUNT_PERCENT = 100.0;

    private DataProcessor instance; // Replace with your actual class name

    @BeforeEach
    void setUp() {
        BlockingQueue<DataBatch> queue = new LinkedBlockingQueue<>();
        AtomicLong processedRows = new AtomicLong(0);
        IngestionConfig config = new IngestionConfig.Builder().build();
        instance = new DataProcessor(queue, processedRows, config);
    }

    // ========== validateDiscount Tests ==========

    @Test
    @DisplayName("validateDiscount should return 0 for negative values")
    void validateDiscount_NegativeValue_ReturnsZero() {
        assertEquals(0.0, instance.validateDiscount(-5.0));
        assertEquals(0.0, instance.validateDiscount(-0.1));
        assertEquals(0.0, instance.validateDiscount(-100.0));
    }

    @Test
    @DisplayName("validateDiscount should return MAX_DISCOUNT_PERCENT for values exceeding maximum")
    void validateDiscount_ExceedsMax_ReturnsMaxDiscountPercent() {
        assertEquals(MAX_DISCOUNT_PERCENT, instance.validateDiscount(150.0));
        assertEquals(MAX_DISCOUNT_PERCENT, instance.validateDiscount(100.1));
        assertEquals(MAX_DISCOUNT_PERCENT, instance.validateDiscount(1000.0));
    }

    @Test
    @DisplayName("validateDiscount should return same value for valid range")
    void validateDiscount_ValidRange_ReturnsSameValue() {
        assertEquals(0.0, instance.validateDiscount(0.0));
        assertEquals(25.5, instance.validateDiscount(25.5));
        assertEquals(50.0, instance.validateDiscount(50.0));
        assertEquals(99.9, instance.validateDiscount(99.9));
        assertEquals(MAX_DISCOUNT_PERCENT, instance.validateDiscount(MAX_DISCOUNT_PERCENT));
    }

    // ========== normalizeRegion Tests ==========

    @Test
    @DisplayName("normalizeRegion should return 'unknown' for null or empty strings")
    void normalizeRegion_NullOrEmpty_ReturnsUnknown() {
        assertEquals("unknown", instance.normalizeRegion(null));
        assertEquals("unknown", instance.normalizeRegion(""));
        assertEquals("unknown", instance.normalizeRegion("   "));
    }

    @ParameterizedTest
    @CsvSource({
            "North, North",
            "north, North",
            "Northern, North",
            "NEW YORK, North",
            "n, North",
            "N, North"
    })
    @DisplayName("normalizeRegion should normalize regions starting with 'n' to 'North'")
    void normalizeRegion_StartsWithN_ReturnsNorth(String input, String expected) {
        assertEquals(expected, instance.normalizeRegion(input));
    }

    @ParameterizedTest
    @CsvSource({
            "East, East",
            "east, East",
            "Eastern, East",
            "Europe, East",
            "e, East",
            "E, East"
    })
    @DisplayName("normalizeRegion should normalize regions starting with 'e' to 'East'")
    void normalizeRegion_StartsWithE_ReturnsEast(String input, String expected) {
        assertEquals(expected, instance.normalizeRegion(input));
    }

    @ParameterizedTest
    @CsvSource({
            "West, West",
            "west, West",
            "Western, West",
            "Washington, West",
            "w, West",
            "W, West"
    })
    @DisplayName("normalizeRegion should normalize regions starting with 'w' to 'West'")
    void normalizeRegion_StartsWithW_ReturnsWest(String input, String expected) {
        assertEquals(expected, instance.normalizeRegion(input));
    }

    @ParameterizedTest
    @CsvSource({
            "South, South",
            "south, South",
            "Southern, South",
            "Singapore, South",
            "s, South",
            "S, South"
    })
    @DisplayName("normalizeRegion should normalize regions starting with 's' to 'South'")
    void normalizeRegion_StartsWithS_ReturnsSouth(String input, String expected) {
        assertEquals(expected, instance.normalizeRegion(input));
    }

    @ParameterizedTest
    @CsvSource({
            "Central, central",
            "MIDWEST, midwest",
            "Africa, africa",
            "123Region, 123region",
            "'  ASIA  ', asia"
    })
    @DisplayName("normalizeRegion should return lowercase trimmed for other regions")
    void normalizeRegion_OtherRegions_ReturnsLowercaseTrimmed(String input, String expected) {
        assertEquals(expected, instance.normalizeRegion(input));
    }

    // ========== validateEmail Tests ==========

    @Test
    @DisplayName("validateEmail should return null for null or empty strings")
    void validateEmail_NullOrEmpty_ReturnsNull() {
        assertNull(instance.validateEmail(null));
        assertNull(instance.validateEmail(""));
        assertNull(instance.validateEmail("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "test@domain.com",
            "  admin@company.com  ",
            "john.doe@website.com",
            "user123@test.com"
    })
    @DisplayName("validateEmail should return trimmed email for valid format")
    void validateEmail_ValidFormat_ReturnsTrimmedEmail(String email) {
        String result = instance.validateEmail(email);
        assertNotNull(result);
        assertEquals(email.trim(), result);
        assertTrue(result.contains("@"));
        assertTrue(result.contains(".com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-email",
            "user@domain",
            "user.domain.com",
            "@domain.com",
            "user@.com",
            "user@domain.net",
            "user@domain.co.uk"
    })
    @DisplayName("validateEmail should return null for invalid format")
    void validateEmail_InvalidFormat_ReturnsNull(String email) {
        assertNull(instance.validateEmail(email));
    }

    // ========== calculateRevenue Tests ==========

    @Test
    @DisplayName("calculateRevenue should calculate correctly with no discount")
    void calculateRevenue_NoDiscount_CalculatesCorrectly() {
        assertEquals(100.0, instance.calculateRevenue(2, 50.0, 0.0));
        assertEquals(299.97, instance.calculateRevenue(3, 99.99, 0.0));
    }

    @Test
    @DisplayName("calculateRevenue should calculate correctly with discount")
    void calculateRevenue_WithDiscount_CalculatesCorrectly() {
        // 2 * 50.0 * (100 - 10) / 100 = 90.0
        assertEquals(90.0, instance.calculateRevenue(2, 50.0, 10.0));

        // 1 * 100.0 * (100 - 25) / 100 = 75.0
        assertEquals(75.0, instance.calculateRevenue(1, 100.0, 25.0));

        // 5 * 20.0 * (100 - 50) / 100 = 50.0
        assertEquals(50.0, instance.calculateRevenue(5, 20.0, 50.0));
    }

    @Test
    @DisplayName("calculateRevenue should handle 100% discount")
    void calculateRevenue_FullDiscount_ReturnsZero() {
        assertEquals(0.0, instance.calculateRevenue(2, 50.0, 100.0));
        assertEquals(0.0, instance.calculateRevenue(10, 999.99, 100.0));
    }

    @Test
    @DisplayName("calculateRevenue should handle zero quantity")
    void calculateRevenue_ZeroQuantity_ReturnsZero() {
        assertEquals(0.0, instance.calculateRevenue(0, 50.0, 10.0));
        assertEquals(0.0, instance.calculateRevenue(0, 999.99, 0.0));
    }

    @Test
    @DisplayName("calculateRevenue should handle zero price")
    void calculateRevenue_ZeroPrice_ReturnsZero() {
        assertEquals(0.0, instance.calculateRevenue(5, 0.0, 10.0));
        assertEquals(0.0, instance.calculateRevenue(100, 0.0, 0.0));
    }

    @ParameterizedTest
    @CsvSource({
            "3, 33.33, 10.0, 89.99",
            "2, 25.50, 5.0, 48.45",
            "1, 10.99, 15.0, 9.34"
    })
    @DisplayName("calculateRevenue should round to 2 decimal places")
    void calculateRevenue_RoundsCorrectly(int quantity, double unitPrice, double discount, double expected) {
        assertEquals(expected, instance.calculateRevenue(quantity, unitPrice, discount), 0.01);
    }

    // ========== parseDate Tests ==========

    @Test
    @DisplayName("parseDate should return null for null or empty strings")
    void parseDate_NullOrEmpty_ReturnsNull() {
        assertNull(instance.parseDate(null));
        assertNull(instance.parseDate(""));
        assertNull(instance.parseDate("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2024-01-15",
            "2023-12-25",
            "2025-06-30"
    })
    @DisplayName("parseDate should parse ISO format dates correctly")
    void parseDate_ISOFormat_ParsesCorrectly(String dateStr) {
        String result = instance.parseDate(dateStr);
        assertNotNull(result);
        // Assuming output format is YYYY-MM-DD
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "15/01/2024",
            "25/12/2023",
            "30/06/2025"
    })
    @DisplayName("parseDate should parse DD/MM/YYYY format dates correctly")
    void parseDate_DDMMYYYYFormat_ParsesCorrectly(String dateStr) {
        String result = instance.parseDate(dateStr);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "01/15/2024",
            "12/25/2023",
            "06/30/2025"
    })
    @DisplayName("parseDate should parse MM/DD/YYYY format dates correctly")
    void parseDate_MMDDYYYYFormat_ParsesCorrectly(String dateStr) {
        String result = instance.parseDate(dateStr);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    @DisplayName("parseDate should handle whitespace around dates")
    void parseDate_WithWhitespace_ParsesCorrectly() {
        assertEquals(instance.parseDate("2024-01-15"), instance.parseDate("  2024-01-15  "));
        assertNotNull(instance.parseDate("  15/01/2024  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-date",
            "2024-13-45",
            "32/12/2024",
            "abc/def/ghij",
            "2024/15/32",
            "not-a-date"
    })
    @DisplayName("parseDate should return null for invalid date formats")
    void parseDate_InvalidFormat_ReturnsNull(String dateStr) {
        assertNull(instance.parseDate(dateStr));
    }

    @Test
    @DisplayName("parseDate should handle edge case dates")
    void parseDate_EdgeCases_HandlesCorrectly() {
        // Leap year
        assertNotNull(instance.parseDate("2024-02-29"));

        // Year boundaries
        assertNotNull(instance.parseDate("2023-12-31"));
        assertNotNull(instance.parseDate("2024-01-01"));

        // Month boundaries
        assertNotNull(instance.parseDate("2024-01-31"));
        assertNotNull(instance.parseDate("2024-02-01"));
    }
}
