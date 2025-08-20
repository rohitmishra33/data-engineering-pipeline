package com.blurr.dashboard.service;

import com.blurr.dashboard.dto.CategoryDiscountDTO;
import com.blurr.dashboard.dto.DashboardSummaryDTO;
import com.blurr.dashboard.dto.MonthlySalesDTO;
import com.blurr.dashboard.dto.RegionPerformanceDTO;
import com.blurr.dashboard.dto.TopProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<MonthlySalesDTO> getMonthlySalesData() {
        String sql = "SELECT year, month, total_revenue, total_quantity, avg_discount, order_count " +
                "FROM monthly_sales_summary ORDER BY year, month";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new MonthlySalesDTO(
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getDouble("total_revenue"),
                        rs.getLong("total_quantity"),
                        rs.getDouble("avg_discount"),
                        rs.getInt("order_count")
                ));
    }

    public List<TopProductDTO> getTopProductsData() {
        String sql = "SELECT product_name, category, total_revenue, total_units_sold, " +
                "avg_unit_price, revenue_rank FROM top_products " +
                "WHERE revenue_rank <= 10 ORDER BY revenue_rank";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new TopProductDTO(
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getDouble("total_revenue"),
                        rs.getLong("total_units_sold"),
                        rs.getDouble("avg_unit_price"),
                        rs.getInt("revenue_rank")
                ));
    }

    public List<RegionPerformanceDTO> getRegionPerformanceData() {
        String sql = "SELECT region, total_revenue, total_orders, market_share_pct, top_product " +
                "FROM region_wise_performance ORDER BY total_revenue DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new RegionPerformanceDTO(
                        rs.getString("region"),
                        rs.getDouble("total_revenue"),
                        rs.getInt("total_orders"),
                        rs.getDouble("market_share_pct"),
                        rs.getString("top_product")
                ));
    }

    public List<CategoryDiscountDTO> getCategoryDiscountData() {
        String sql = "SELECT category, avg_discount, total_revenue, total_orders, discount_impact " +
                "FROM category_discount_map ORDER BY avg_discount DESC LIMIT 10";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new CategoryDiscountDTO(
                        rs.getString("category"),
                        rs.getDouble("avg_discount"),
                        rs.getDouble("total_revenue"),
                        rs.getInt("total_orders"),
                        rs.getDouble("discount_impact")
                ));
    }

    public DashboardSummaryDTO getDashboardSummary() {
        String sql = "SELECT " +
                "(SELECT SUM(total_revenue) FROM monthly_sales_summary) as total_revenue, " +
                "(SELECT SUM(order_count) FROM monthly_sales_summary) as total_orders, " +
                "(SELECT COUNT(DISTINCT region) FROM region_wise_performance) as total_regions, " +
                "(SELECT COUNT(*) FROM top_products WHERE revenue_rank <= 10) as top_products_count";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new DashboardSummaryDTO(
                        rs.getDouble("total_revenue"),
                        rs.getLong("total_orders"),
                        rs.getInt("total_regions"),
                        rs.getInt("top_products_count")
                ));
    }
}

