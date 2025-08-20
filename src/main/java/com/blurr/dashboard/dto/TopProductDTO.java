package com.blurr.dashboard.dto;

public class TopProductDTO {
    private String productName;
    private String category;
    private double totalRevenue;
    private long totalUnitsSold;
    private double avgUnitPrice;
    private int revenueRank;

    public TopProductDTO() {
    }

    public TopProductDTO(String productName, String category, double totalRevenue,
                         long totalUnitsSold, double avgUnitPrice, int revenueRank) {
        this.productName = productName;
        this.category = category;
        this.totalRevenue = totalRevenue;
        this.totalUnitsSold = totalUnitsSold;
        this.avgUnitPrice = avgUnitPrice;
        this.revenueRank = revenueRank;
    }

    // Getters and Setters
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(long totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public double getAvgUnitPrice() {
        return avgUnitPrice;
    }

    public void setAvgUnitPrice(double avgUnitPrice) {
        this.avgUnitPrice = avgUnitPrice;
    }

    public int getRevenueRank() {
        return revenueRank;
    }

    public void setRevenueRank(int revenueRank) {
        this.revenueRank = revenueRank;
    }
}

