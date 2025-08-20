package com.blurr.dashboard.dto;

public class MonthlySalesDTO {
    private int year;
    private int month;
    private double totalRevenue;
    private long totalQuantity;
    private double avgDiscount;
    private int orderCount;

    // Constructors
    public MonthlySalesDTO() {
    }

    public MonthlySalesDTO(int year, int month, double totalRevenue,
                           long totalQuantity, double avgDiscount, int orderCount) {
        this.year = year;
        this.month = month;
        this.totalRevenue = totalRevenue;
        this.totalQuantity = totalQuantity;
        this.avgDiscount = avgDiscount;
        this.orderCount = orderCount;
    }

    // Getters and Setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public double getAvgDiscount() {
        return avgDiscount;
    }

    public void setAvgDiscount(double avgDiscount) {
        this.avgDiscount = avgDiscount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public String getMonthLabel() {
        return year + "-" + String.format("%02d", month);
    }
}
