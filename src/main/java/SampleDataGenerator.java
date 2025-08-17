import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SampleDataGenerator {

    private static final String[] PRODUCT_NAMES = {
            "Smartphone", "Smart phone", "SMARTPHONE", "smart phone",
            "Laptop", "Lap top", "LAPTOP", "laptop computer",
            "Washing Machine", "Washing machine", "washing machine", "WASHING MACHINE",
            "Chair", "Chair set", "CHAIR", "office chair",
            "Television", "TV", "tv", "TELEVISION",
            "Refrigerator", "Fridge", "FRIDGE", "refridgerator"
    };

    private static final String[] CATEGORIES = {
            "electronics", "Electronics", "ELECTRONICS", "electronic",
            "home appliance", "Home Appliance", "home applicance", "HOME APPLIANCE",
            "furniture", "Furniture", "FURNITURE", "furnishing",
            "gadgets", "Gadgets", "electronics gadgets"
    };

    private static final String[] REGIONS = {
            "North", "nort", "NORTH", "north", "South", "south",
            "East", "east", "West", "west"
    };

    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd", "dd/MM/yyyy", "MM-dd-yyyy", "MMMM d, yyyy"
    };

    private static final Random RANDOM = new Random();

    private static String randomProductName() {
        return PRODUCT_NAMES[RANDOM.nextInt(PRODUCT_NAMES.length)];
    }

    private static String randomCategory() {
        return CATEGORIES[RANDOM.nextInt(CATEGORIES.length)];
    }

    private static String randomRegion() {
        return REGIONS[RANDOM.nextInt(REGIONS.length)];
    }

    private static String randomQuantity() {
        int mode = RANDOM.nextInt(3);
        if (mode == 0)
            return String.valueOf(RANDOM.nextInt(26) - 5); // -5 to 20
        if (mode == 1)
            return "0";
        return String.valueOf(RANDOM.nextInt(5) + 1); // 1 to 5
    }

    private static double randomUnitPrice() {
        return Math.round((RANDOM.nextDouble() * 495.0 + 5.0) * 100.0) / 100.0;
    }

    private static double randomDiscountPercent() {
        return Math.round((RANDOM.nextDouble() * 1.5) * 100.0) / 100.0; // 0.0–1.5, some >1 error
    }

    private static String randomRegionName() {
        return REGIONS[RANDOM.nextInt(REGIONS.length)];
    }

    private static String randomDate() {
        // Sometimes null
        if (RANDOM.nextInt(10) == 0) return ""; // 10% nulls
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2020 + RANDOM.nextInt(4)); // 2020–2023
        cal.set(Calendar.DAY_OF_YEAR, 1 + RANDOM.nextInt(cal.getActualMaximum(Calendar.DAY_OF_YEAR)));
        String format = DATE_FORMATS[RANDOM.nextInt(DATE_FORMATS.length)];
        return new SimpleDateFormat(format, Locale.US).format(cal.getTime());
    }

    private static String randomEmail() {
        // Sometimes null
        if (RANDOM.nextInt(4) == 0) return "";
        return "customer" + (1000 + RANDOM.nextInt(9000)) + "@example.com";
    }

    private static double calculateRevenue(String q, double price, double discount) {
        try {
            int quantity = Integer.parseInt(q.trim());
            double effectiveDiscount = (discount >= 0.0 && discount <= 1.0) ? discount : 0.0;
            return Math.round(quantity * price * (1.0 - effectiveDiscount) * 100.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String randomOrderId(List<String> prevOrderIds) {
        String base = randomAlphaNumeric(10);
        if (RANDOM.nextDouble() < 0.1 && !prevOrderIds.isEmpty()) { // 10% chance duplicate
            return prevOrderIds.get(RANDOM.nextInt(prevOrderIds.size()));
        }
        return base;
    }

    private static String randomAlphaNumeric(int count) {
        StringBuilder sb = new StringBuilder(count);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < count; i++)
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java SampleDataGenerator <number_of_rows>");
            return;
        }
        int numRows = Integer.parseInt(args[0]);
        String fileName = "sample_data_" + numRows + "_rows.csv";

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(fileName), StandardCharsets.UTF_8)) {
            writer.write("order_id,product_name,category,quantity,unit_price,discount_percent,region,sale_date,customer_email,revenue\n");

            List<String> orderIds = new ArrayList<>();
            for (int i = 0; i < numRows; i++) {
                String orderId = randomOrderId(orderIds);
                orderIds.add(orderId);

                String productName = randomProductName();
                String category = randomCategory();
                String quantity = randomQuantity();
                double unitPrice = randomUnitPrice();
                double discountPercent = randomDiscountPercent();
                String region = randomRegion();
                String saleDate = randomDate();
                String customerEmail = randomEmail();
                double revenue = calculateRevenue(quantity, unitPrice, discountPercent);

                // Escape CSV
                String[] fields = {
                        orderId,
                        productName,
                        category,
                        quantity,
                        String.valueOf(unitPrice),
                        String.valueOf(discountPercent),
                        region,
                        saleDate,
                        customerEmail,
                        String.valueOf(revenue)
                };
                writer.write(csvRow(fields));
                writer.newLine();
            }
        }

        System.out.println("Sample data created: " + fileName);
    }

    private static String csvRow(String[] fields) {
        // Simple CSV escaping: double quote if necessary
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == null) {
                sb.append("");
            } else if (fields[i].contains(",") || fields[i].contains("\"")) {
                sb.append('"')
                        .append(fields[i].replace("\"", "\"\""))
                        .append('"');
            } else {
                sb.append(fields[i]);
            }
            if (i < fields.length - 1)
                sb.append(',');
        }
        return sb.toString();
    }
}
