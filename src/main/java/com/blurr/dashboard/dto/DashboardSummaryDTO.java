package com.blurr.dashboard.dto;

public class DashboardSummaryDTO {
    private double totalRevenue;
    private long totalOrders;
    private int totalRegions;
    private int topProductsCount;

    public DashboardSummaryDTO(double totalRevenue, long totalOrders,
                               int totalRegions, int topProductsCount) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalRegions = totalRegions;
        this.topProductsCount = topProductsCount;
    }

    // Getters
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public int getTotalRegions() {
        return totalRegions;
    }

    public int getTopProductsCount() {
        return topProductsCount;
    }
}
