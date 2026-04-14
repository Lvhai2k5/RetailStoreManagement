package ute.fit.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DefectiveQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public DefectiveQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Map<String, Object> getDashboardStats() {
        String sql = """
                SELECT
                    COUNT(*) AS totalBatches,
                    SUM(CASE WHEN b.ExpiryDate IS NOT NULL
                              AND DATE(b.ExpiryDate) BETWEEN CURRENT_DATE()
                                  AND DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY)
                             THEN 1 ELSE 0 END) AS expiringSoonBatches,
                    SUM(CASE WHEN b.ExpiryDate IS NOT NULL
                              AND DATE(b.ExpiryDate) < CURRENT_DATE()
                             THEN 1 ELSE 0 END) AS expiredBatches,
                    IFNULL((SELECT SUM(d.Quantity) FROM DefectiveProducts d), 0) AS totalDefectiveQuantity
                FROM ImportBatches b
                """;
        return jdbcTemplate.queryForMap(sql);
    }

    public List<Map<String, Object>> searchBatchDefectiveData(LocalDate fromDate,
                                                               LocalDate toDate,
                                                               String expiryStatus,
                                                               String reason,
                                                               String keyword) {
        String sql = """
                SELECT
                    b.BatchID,
                    b.BatchNumber,
                    p.ProductID,
                    p.Name AS ProductName,
                    b.Quantity AS ImportedQuantity,
                    b.ImportDate,
                    b.ExpiryDate,
                    DATE_FORMAT(b.ImportDate, '%d/%m/%Y') AS ImportDateText,
                    DATE_FORMAT(b.ExpiryDate, '%d/%m/%Y') AS ExpiryDateText,
                    b.ImportPrice,
                    b.SellingPrice,
                    IFNULL(stock.SellableStock, 0) AS SellableStock,
                    IFNULL(df.TotalDefectiveQuantity, 0) AS DefectiveQuantity,
                    df.LastReason,
                    DATE_FORMAT(df.LastDefectiveDate, '%d/%m/%Y') AS LastDefectiveDateText,
                    CASE
                        WHEN b.ExpiryDate IS NULL THEN 'NO_EXPIRY'
                        WHEN DATE(b.ExpiryDate) < CURRENT_DATE() THEN 'EXPIRED'
                        WHEN DATE(b.ExpiryDate) BETWEEN CURRENT_DATE() AND DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY) THEN 'EXPIRING'
                        ELSE 'VALID'
                    END AS ExpiryStatus
                FROM ImportBatches b
                JOIN Products p ON p.ProductID = b.ProductID
                LEFT JOIN (
                    SELECT it.BatchID, SUM(it.QuantityChange) AS SellableStock
                    FROM InventoryTransactions it
                    WHERE it.IsSellable = 1
                    GROUP BY it.BatchID
                ) stock ON stock.BatchID = b.BatchID
                LEFT JOIN (
                    SELECT
                        d.BatchID,
                        IFNULL(SUM(d.Quantity), 0) AS TotalDefectiveQuantity,
                        (
                            SELECT d2.Reason
                            FROM DefectiveProducts d2
                            WHERE d2.BatchID = d.BatchID
                            ORDER BY d2.CreatedDate DESC, d2.DefectiveID DESC
                            LIMIT 1
                        ) AS LastReason,
                        MAX(d.CreatedDate) AS LastDefectiveDate
                    FROM DefectiveProducts d
                    GROUP BY d.BatchID
                ) df ON df.BatchID = b.BatchID
                WHERE (:fromDate IS NULL OR DATE(COALESCE(df.LastDefectiveDate, b.ImportDate)) >= :fromDate)
                  AND (:toDate IS NULL OR DATE(COALESCE(df.LastDefectiveDate, b.ImportDate)) <= :toDate)
                  AND (:keyword IS NULL OR :keyword = ''
                       OR CAST(b.BatchID AS CHAR) LIKE CONCAT('%', :keyword, '%')
                       OR p.ProductID LIKE CONCAT('%', :keyword, '%')
                       OR p.Name LIKE CONCAT('%', :keyword, '%'))
                  AND (
                      :expiryStatus IS NULL OR :expiryStatus = '' OR :expiryStatus = 'ALL'
                      OR (
                          :expiryStatus = 'EXPIRED'
                          AND b.ExpiryDate IS NOT NULL
                          AND DATE(b.ExpiryDate) < CURRENT_DATE()
                      )
                      OR (
                          :expiryStatus = 'EXPIRING'
                          AND b.ExpiryDate IS NOT NULL
                          AND DATE(b.ExpiryDate) BETWEEN CURRENT_DATE()
                              AND DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY)
                      )
                      OR (
                          :expiryStatus = 'VALID'
                          AND (b.ExpiryDate IS NULL OR DATE(b.ExpiryDate) > DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY))
                      )
                  )
                  AND (
                      :reason IS NULL OR :reason = '' OR :reason = 'ALL'
                      OR EXISTS (
                          SELECT 1
                          FROM DefectiveProducts dr
                          WHERE dr.BatchID = b.BatchID
                            AND dr.Reason LIKE CONCAT('%', :reason, '%')
                      )
                  )
                ORDER BY b.BatchID DESC
                """;

        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate)
                .addValue("expiryStatus", expiryStatus)
                .addValue("reason", reason)
                .addValue("keyword", keyword);

        return namedJdbc.queryForList(sql, ps);
    }

    public Map<String, Object> getBatchHandleInfo(Integer batchId) {
        String sql = """
                SELECT
                    b.BatchID,
                    b.BatchNumber,
                    p.ProductID,
                    p.Name AS ProductName,
                    b.ImportPrice,
                    b.SellingPrice,
                    b.ImportDate,
                    b.ExpiryDate,
                    DATE_FORMAT(b.ImportDate, '%d/%m/%Y') AS ImportDateText,
                    DATE_FORMAT(b.ExpiryDate, '%d/%m/%Y') AS ExpiryDateText,
                    IFNULL(stock.SellableStock, 0) AS SellableStock
                FROM ImportBatches b
                JOIN Products p ON p.ProductID = b.ProductID
                LEFT JOIN (
                    SELECT it.BatchID, SUM(it.QuantityChange) AS SellableStock
                    FROM InventoryTransactions it
                    WHERE it.IsSellable = 1
                    GROUP BY it.BatchID
                ) stock ON stock.BatchID = b.BatchID
                WHERE b.BatchID = ?
                LIMIT 1
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, batchId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findExpiredBatchesWithSellableStock() {
        String sql = """
                SELECT
                    b.BatchID,
                    IFNULL(stock.SellableStock, 0) AS SellableStock
                FROM ImportBatches b
                LEFT JOIN (
                    SELECT it.BatchID, SUM(it.QuantityChange) AS SellableStock
                    FROM InventoryTransactions it
                    WHERE it.IsSellable = 1
                    GROUP BY it.BatchID
                ) stock ON stock.BatchID = b.BatchID
                WHERE b.ExpiryDate IS NOT NULL
                  AND DATE(b.ExpiryDate) < CURRENT_DATE()
                  AND IFNULL(stock.SellableStock, 0) > 0
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<String> findDistinctReasons() {
        String sql = """
                SELECT DISTINCT d.Reason
                FROM DefectiveProducts d
                WHERE d.Reason IS NOT NULL
                  AND TRIM(d.Reason) <> ''
                ORDER BY d.Reason
                """;
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
