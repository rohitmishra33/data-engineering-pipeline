package com.blurr.dashboard.controller;

import com.blurr.dashboard.dto.CategoryDiscountDTO;
import com.blurr.dashboard.dto.DashboardSummaryDTO;
import com.blurr.dashboard.dto.MonthlySalesDTO;
import com.blurr.dashboard.dto.RegionPerformanceDTO;
import com.blurr.dashboard.dto.TopProductDTO;
import com.blurr.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*") // Enable CORS for frontend
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/monthly-sales")
    public ResponseEntity<List<MonthlySalesDTO>> getMonthlySales() {
        try {
            List<MonthlySalesDTO> data = dashboardService.getMonthlySalesData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts() {
        try {
            List<TopProductDTO> data = dashboardService.getTopProductsData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/region-performance")
    public ResponseEntity<List<RegionPerformanceDTO>> getRegionPerformance() {
        try {
            List<RegionPerformanceDTO> data = dashboardService.getRegionPerformanceData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/category-discounts")
    public ResponseEntity<List<CategoryDiscountDTO>> getCategoryDiscounts() {
        try {
            List<CategoryDiscountDTO> data = dashboardService.getCategoryDiscountData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        try {
            DashboardSummaryDTO data = dashboardService.getDashboardSummary();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
