package com.blurr.dashboard.dto;

public class CategoryDiscountDTO {
    private String category;
    private double avgDiscount;
    private double totalRevenue;
    private int totalOrders;
    private double discountImpact;

    public CategoryDiscountDTO() {
    }

    public CategoryDiscountDTO(String category, double avgDiscount, double totalRevenue,
                               int totalOrders, double discountImpact) {
        this.category = category;
        this.avgDiscount = avgDiscount;
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.discountImpact = discountImpact;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAvgDiscount() {
        return avgDiscount;
    }

    public void setAvgDiscount(double avgDiscount) {
        this.avgDiscount = avgDiscount;
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

    public double getDiscountImpact() {
        return discountImpact;
    }

    public void setDiscountImpact(double discountImpact) {
        this.discountImpact = discountImpact;
    }
}
