package com.blurr.dashboard.dto;

public class RegionPerformanceDTO {
    private String region;
    private double totalRevenue;
    private int totalOrders;
    private double marketSharePct;
    private String topProduct;

    public RegionPerformanceDTO() {
    }

    public RegionPerformanceDTO(String region, double totalRevenue, int totalOrders,
                                double marketSharePct, String topProduct) {
        this.region = region;
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.marketSharePct = marketSharePct;
        this.topProduct = topProduct;
    }

    // Getters and Setters
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getMarketSharePct() {
        return marketSharePct;
    }

    public void setMarketSharePct(double marketSharePct) {
        this.marketSharePct = marketSharePct;
    }

    public String getTopProduct() {
        return topProduct;
    }

    public void setTopProduct(String topProduct) {
        this.topProduct = topProduct;
    }
}
