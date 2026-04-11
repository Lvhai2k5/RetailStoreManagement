package ute.fit.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
                              AND CAST(b.ExpiryDate AS date) BETWEEN CAST(GETDATE() AS date)
                                  AND DATEADD(day, 7, CAST(GETDATE() AS date))
                             THEN 1 ELSE 0 END) AS expiringSoonBatches,
                    SUM(CASE WHEN b.ExpiryDate IS NOT NULL
                              AND CAST(b.ExpiryDate AS date) < CAST(GETDATE() AS date)
                             THEN 1 ELSE 0 END) AS expiredBatches,
                    ISNULL((SELECT SUM(d.Quantity) FROM DefectiveProducts d), 0) AS totalDefectiveQuantity
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
                    CONVERT(varchar(10), b.ImportDate, 103) AS ImportDateText,
                    CONVERT(varchar(10), b.ExpiryDate, 103) AS ExpiryDateText,
                    b.ImportPrice,
                    b.SellingPrice,
                    ISNULL(stock.SellableStock, 0) AS SellableStock,
                    ISNULL(df.TotalDefectiveQuantity, 0) AS DefectiveQuantity,
                    df.LastReason,
                    CONVERT(varchar(10), df.LastDefectiveDate, 103) AS LastDefectiveDateText,
                    CASE
                        WHEN b.ExpiryDate IS NULL THEN 'NO_EXPIRY'
                        WHEN CAST(b.ExpiryDate AS date) < CAST(GETDATE() AS date) THEN 'EXPIRED'
                        WHEN CAST(b.ExpiryDate AS date) BETWEEN CAST(GETDATE() AS date) AND DATEADD(day, 7, CAST(GETDATE() AS date)) THEN 'EXPIRING'
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
                OUTER APPLY (
                    SELECT
                        ISNULL(SUM(d.Quantity), 0) AS TotalDefectiveQuantity,
                        (
                            SELECT TOP 1 d2.Reason
                            FROM DefectiveProducts d2
                            WHERE d2.BatchID = b.BatchID
                            ORDER BY d2.CreatedDate DESC, d2.DefectiveID DESC
                        ) AS LastReason,
                        MAX(d.CreatedDate) AS LastDefectiveDate
                    FROM DefectiveProducts d
                    WHERE d.BatchID = b.BatchID
                ) df
                WHERE (:fromDate IS NULL OR CAST(COALESCE(df.LastDefectiveDate, b.ImportDate) AS date) >= :fromDate)
                  AND (:toDate IS NULL OR CAST(COALESCE(df.LastDefectiveDate, b.ImportDate) AS date) <= :toDate)
                  AND (:keyword IS NULL OR :keyword = ''
                       OR CAST(b.BatchID AS VARCHAR(30)) LIKE '%' + :keyword + '%'
                       OR p.ProductID LIKE '%' + :keyword + '%'
                       OR p.Name LIKE '%' + :keyword + '%')
                  AND (
                      :expiryStatus IS NULL OR :expiryStatus = '' OR :expiryStatus = 'ALL'
                      OR (
                          :expiryStatus = 'EXPIRED'
                          AND b.ExpiryDate IS NOT NULL
                          AND CAST(b.ExpiryDate AS date) < CAST(GETDATE() AS date)
                      )
                      OR (
                          :expiryStatus = 'EXPIRING'
                          AND b.ExpiryDate IS NOT NULL
                          AND CAST(b.ExpiryDate AS date) BETWEEN CAST(GETDATE() AS date)
                              AND DATEADD(day, 7, CAST(GETDATE() AS date))
                      )
                      OR (
                          :expiryStatus = 'VALID'
                          AND (b.ExpiryDate IS NULL OR CAST(b.ExpiryDate AS date) > DATEADD(day, 7, CAST(GETDATE() AS date)))
                      )
                  )
                  AND (
                      :reason IS NULL OR :reason = '' OR :reason = 'ALL'
                      OR EXISTS (
                          SELECT 1
                          FROM DefectiveProducts dr
                          WHERE dr.BatchID = b.BatchID
                            AND dr.Reason LIKE '%' + :reason + '%'
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
                SELECT TOP 1
                    b.BatchID,
                    b.BatchNumber,
                    p.ProductID,
                    p.Name AS ProductName,
                    b.ImportPrice,
                    b.SellingPrice,
                    b.ImportDate,
                    b.ExpiryDate,
                    CONVERT(varchar(10), b.ImportDate, 103) AS ImportDateText,
                    CONVERT(varchar(10), b.ExpiryDate, 103) AS ExpiryDateText,
                    ISNULL(stock.SellableStock, 0) AS SellableStock
                FROM ImportBatches b
                JOIN Products p ON p.ProductID = b.ProductID
                LEFT JOIN (
                    SELECT it.BatchID, SUM(it.QuantityChange) AS SellableStock
                    FROM InventoryTransactions it
                    WHERE it.IsSellable = 1
                    GROUP BY it.BatchID
                ) stock ON stock.BatchID = b.BatchID
                WHERE b.BatchID = ?
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, batchId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findExpiredBatchesWithSellableStock() {
        String sql = """
                SELECT
                    b.BatchID,
                    ISNULL(stock.SellableStock, 0) AS SellableStock
                FROM ImportBatches b
                LEFT JOIN (
                    SELECT it.BatchID, SUM(it.QuantityChange) AS SellableStock
                    FROM InventoryTransactions it
                    WHERE it.IsSellable = 1
                    GROUP BY it.BatchID
                ) stock ON stock.BatchID = b.BatchID
                WHERE b.ExpiryDate IS NOT NULL
                  AND CAST(b.ExpiryDate AS date) < CAST(GETDATE() AS date)
                  AND ISNULL(stock.SellableStock, 0) > 0
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<String> findDistinctReasons() {
        String sql = """
                SELECT DISTINCT d.Reason
                FROM DefectiveProducts d
                WHERE d.Reason IS NOT NULL
                  AND LTRIM(RTRIM(d.Reason)) <> ''
                ORDER BY d.Reason
                """;
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
