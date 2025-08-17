package com.blurr.pipeline.models;

// Processed record model
public class ProcessedRecord {
    private String orderId;
    private String productName;
    private String category;
    private int quantity;
    private double unitPrice;
    private double discountPercent;
    private String region;
    private String saleDate;
    private String customerEmail;
    private double revenue;

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProcessedRecord record = new ProcessedRecord();

        public Builder orderId(String orderId) {
            record.orderId = orderId;
            return this;
        }

        public Builder productName(String productName) {
            record.productName = productName;
            return this;
        }

        public Builder category(String category) {
            record.category = category;
            return this;
        }

        public Builder quantity(int quantity) {
            record.quantity = quantity;
            return this;
        }

        public Builder unitPrice(double unitPrice) {
            record.unitPrice = unitPrice;
            return this;
        }

        public Builder discountPercent(double discountPercent) {
            record.discountPercent = discountPercent;
            return this;
        }

        public Builder region(String region) {
            record.region = region;
            return this;
        }

        public Builder saleDate(String saleDate) {
            record.saleDate = saleDate;
            return this;
        }

        public Builder customerEmail(String customerEmail) {
            record.customerEmail = customerEmail;
            return this;
        }

        public Builder revenue(double revenue) {
            record.revenue = revenue;
            return this;
        }

        public ProcessedRecord build() {
            return record;
        }
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getDiscountPercent() { return discountPercent; }
    public String getRegion() { return region; }
    public String getSaleDate() { return saleDate; }
    public String getCustomerEmail() { return customerEmail; }
    public double getRevenue() { return revenue; }

    // Setter for revenue as it is a computed field
    public void setRevenue(double revenue) { this.revenue = revenue; }
}
