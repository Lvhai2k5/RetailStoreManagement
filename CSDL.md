## SQL Server DDL

```sql
CREATE TABLE Suppliers (
    SupplierID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(255) NOT NULL,
    Phone NVARCHAR(20) NOT NULL,
    Address NVARCHAR(MAX) NOT NULL,
    Status NVARCHAR(10) NOT NULL DEFAULT 'COLLAB', -- Trạng thái: COLLAB (hợp tác), STOP (dừng)

    CONSTRAINT CHK_Supplier_Status
        CHECK (Status IN ('COLLAB', 'STOP'))
);

CREATE TABLE ProductTypesMarkup (
    ProductTypeID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(255) NOT NULL,
    MarkupPercent DECIMAL(5,2) NOT NULL -- Lợi nhuận theo loại sản phẩm
);

CREATE TABLE Products (
    ProductID VARCHAR(100) PRIMARY KEY, -- StingDo, HaoHaoChuaCay, BongTayTrang
    Name NVARCHAR(255) NOT NULL, -- Sting đỏ, mì Hảo Hảo chua cay, Bông tẩy trang
    ProductTypeID INT NOT NULL,
    DefaultSellingPrice DECIMAL(18,2), -- Giá bán thực tế (đã chia bình quân)
    Status NVARCHAR(10) NOT NULL DEFAULT 'BUYING', -- Trạng thái: BUYING (đang bán), HIDING (ẩn/ngừng bán)
    CreatedDate DATETIME2 DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_Products_ProductType
        FOREIGN KEY (ProductTypeID) REFERENCES ProductTypes(ProductTypeID),

    CONSTRAINT CHK_Product_Status
        CHECK (Status IN ('BUYING', 'HIDING'))
);

-- Một loại sản phẩm có thể được cung cấp từ nhiều nhà phân phối
CREATE TABLE SupplierProducts (
    SupplierProductID INT IDENTITY(1,1) PRIMARY KEY,
    SupplierID INT NOT NULL,
    ProductID VARCHAR(100) NOT NULL,
    SupplierProductCode NVARCHAR(100),

    CONSTRAINT FK_SupplierProducts_Supplier
        FOREIGN KEY (SupplierID) REFERENCES Suppliers(SupplierID),

    CONSTRAINT FK_SupplierProducts_Product
        FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

-- Log lịch sử thay đổi giá bán thực tế (giá ra thị trường)
CREATE TABLE ProductPriceHistory (
    PriceID INT IDENTITY(1,1) PRIMARY KEY,
    ProductID VARCHAR(100) NOT NULL,
    SellingPrice DECIMAL(18,2) NOT NULL, -- Giá bán áp dụng ra thị trường
    ChangeReason NVARCHAR(50) NOT NULL, -- Lý do thay đổi: MANUAL / NEW_BATCH
    EffectiveDate DATETIME2 DEFAULT SYSUTCDATETIME(), -- Thời gian áp dụng giá

    CONSTRAINT FK_Price_Product
        FOREIGN KEY (ProductID) REFERENCES Products(ProductID),

    CONSTRAINT CHK_PriceHistory_Reason
        CHECK (ChangeReason IN ('MANUAL', 'NEW_BATCH'))
);

CREATE TABLE ImportBatches (
    BatchID INT IDENTITY PRIMARY KEY,          -- ID lô nhập
    ProductID VARCHAR(100) NOT NULL,           -- sản phẩm của lô
    SupplierID INT NOT NULL,                   -- nhà cung cấp
    BatchNumber INT NOT NULL,                  -- số lô của sản phẩm
    Quantity INT NOT NULL,                     -- tổng số lượng nhập
    -- RemainingQuantity INT NOT NULL,            -- số lượng còn lại
    ImportPrice DECIMAL(18,2) NOT NULL,        -- giá nhập 1 sản phẩm trong lô
    SellingPrice DECIMAL(18,2) NOT NULL,       -- giá bán mặc định (được tính dựa theo MarkupPercent) 1 sản phẩm của 1 lô
    ImportDate DATETIME2 DEFAULT SYSUTCDATETIME(), -- ngày nhập
    ExpiryDate DATETIME2 NULL,                 -- hạn sử dụng

    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (SupplierID) REFERENCES Suppliers(SupplierID)
);

CREATE TABLE Orders (
    OrderID INT IDENTITY(1,1) PRIMARY KEY,
    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending', -- Pending / Paid / Cancelled
    PaymentMethod NVARCHAR(20),
    TotalAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
    CreatedDate DATETIME2 DEFAULT SYSUTCDATETIME(), -- ngày tạo đơn
    PaidDate DATETIME2 NULL,                        -- thời điểm thanh toán

    CONSTRAINT CHK_Order_Status
        CHECK (Status IN ('Pending','Paid', 'Cancelled')),

    CONSTRAINT CHK_Order_Payment
        CHECK (PaymentMethod IN ('Cash','Transfer') OR PaymentMethod IS NULL)
);

CREATE TABLE OrderDetails (
    OrderDetailID INT IDENTITY PRIMARY KEY,
    OrderID INT NOT NULL,
    ProductID VARCHAR(100) NOT NULL,
    Quantity INT NOT NULL,
    UnitPrice DECIMAL(18,2) NOT NULL, -- Giá bán của một sản phẩm tại thời điểm bán

    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

-- Bảng phân bổ bán hàng - xác định đơn đó lấy bao nhiêu sản phẩm gì thuộc lô nào
CREATE TABLE SaleAllocations (
    AllocationID INT IDENTITY PRIMARY KEY,
    OrderDetailID INT NOT NULL,
    BatchID INT NOT NULL,
    Quantity INT NOT NULL,

    FOREIGN KEY (OrderDetailID) REFERENCES OrderDetails(OrderDetailID),
    FOREIGN KEY (BatchID) REFERENCES ImportBatches(BatchID)
);

-- Phiếu trả hàng
CREATE TABLE Returns (
    ReturnID INT IDENTITY(1,1) PRIMARY KEY,
    OrderID INT NOT NULL,
    ReturnDate DATETIME2 DEFAULT SYSUTCDATETIME(),
    RefundAmount DECIMAL(18,2) NOT NULL, -- Tổng tiền hoàn

    CONSTRAINT FK_Return_Order
        FOREIGN KEY (OrderID) REFERENCES Orders(OrderID)
);

CREATE TABLE ReturnDetails (
    ReturnDetailID INT IDENTITY(1,1) PRIMARY KEY,   -- ID dòng trả hàng
    ReturnID INT NOT NULL,                          -- Phiếu trả
    OrderDetailID INT NOT NULL,                     -- Sản phẩm trong hóa đơn
    Quantity INT NOT NULL,                          -- Số lượng trả
    RefundUnitPrice DECIMAL(18,2) NOT NULL,         -- Giá hoàn tiền cho 1 sản phẩm
    Reason NVARCHAR(255),                           -- Lý do trả

    CONSTRAINT FK_ReturnDetail_Return
        FOREIGN KEY (ReturnID) REFERENCES Returns(ReturnID),

    CONSTRAINT FK_ReturnDetail_OrderDetail
        FOREIGN KEY (OrderDetailID) REFERENCES OrderDetails(OrderDetailID),

    CONSTRAINT FK_ReturnDetail_Batch
        FOREIGN KEY (BatchID) REFERENCES ImportBatches(BatchID)
);

CREATE TABLE DefectiveProducts (
    DefectiveID INT IDENTITY(1,1) PRIMARY KEY,
    BatchID INT NOT NULL,
    Quantity INT NOT NULL,
    Reason NVARCHAR(255),
    ImportPrice DECIMAL(18,2),
    CreatedDate DATETIME2 DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_Defective_Batch
        FOREIGN KEY (BatchID) REFERENCES ImportBatches(BatchID)
);

-- Log biến động tồn kho
CREATE TABLE InventoryTransactions (
    TransactionID INT IDENTITY(1,1) PRIMARY KEY,
    BatchID INT NOT NULL,
    QuantityChange INT NOT NULL,    --Số lượng thay đổi, + nhập/trả hàng, -bán/hàng lỗi
    TransactionType NVARCHAR(20) NOT NULL, -- IMPORT / SALE / RETURN / DEFECT / RESERVE / CANCEL_RESERVE
    IsSellable BIT NOT NULL DEFAULT 1
    ReferenceID INT NULL,   -- ID tham chiếu(OrderID, ReturnID...)
    TransactionDate DATETIME2 DEFAULT SYSUTCDATETIME(),

    CONSTRAINT FK_Inventory_Batch
        FOREIGN KEY (BatchID) REFERENCES ImportBatches(BatchID)
);

-- ======================TRIGGERS (UNFINISHED) ==========================

-- Trigger Ghi log khi import lô mới
CREATE OR ALTER TRIGGER TRG_ImportBatch_Inventory
ON ImportBatches
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO InventoryTransactions
    (BatchID, QuantityChange, TransactionType, IsSellable)
    SELECT i.BatchID, i.Quantity, 'IMPORT', 1
    FROM inserted i;
END

-- Trigger ghi log khi chủ tự đổi giá
CREATE OR ALTER TRIGGER TRG_ProductPrice_Update
ON Products
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF SESSION_CONTEXT(N'UpdateSource') = 'SYSTEM'
        RETURN;

    INSERT INTO ProductPriceHistory (ProductID, SellingPrice, ChangeReason)
    SELECT i.ProductID, i.DefaultSellingPrice, 'MANUAL'
    FROM inserted i
    JOIN deleted d ON i.ProductID = d.ProductID
    WHERE ISNULL(i.DefaultSellingPrice,0) <> ISNULL(d.DefaultSellingPrice,0);
END

-- Trigger ghi log đổi giá do nhập lô mới
CREATE OR ALTER TRIGGER TRG_ImportBatch_Insert
ON ImportBatches
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    EXEC sp_set_session_context 'UpdateSource', 'SYSTEM';

    ;WITH C AS (
        SELECT i.ProductID, i.BatchNumber, i.SellingPrice AS CurrentPrice
        FROM inserted i
    ),
    P AS (
        SELECT ProductID, BatchNumber, SellingPrice
        FROM ImportBatches
    ),
    F AS (
        SELECT
            C.ProductID,
            CASE
                WHEN P.SellingPrice IS NULL THEN C.CurrentPrice
                ELSE (C.CurrentPrice + P.SellingPrice)/2
            END AS NewPrice
        FROM C
        LEFT JOIN P
            ON C.ProductID = P.ProductID
            AND P.BatchNumber = C.BatchNumber - 1
    )
    UPDATE Products
    SET DefaultSellingPrice = F.NewPrice
    FROM Products
    JOIN F ON Products.ProductID = F.ProductID;

    INSERT INTO ProductPriceHistory (ProductID, SellingPrice, ChangeReason)
    SELECT ProductID, NewPrice, 'NEW_BATCH'
    FROM F;

    EXEC sp_set_session_context 'UpdateSource', NULL;
END

-- ================PROCEDURES===============
-- Tạo đơn
CREATE OR ALTER PROCEDURE PROC_CreateOrder
(
    @OrderID INT OUTPUT
)
AS
BEGIN
    INSERT INTO Orders(Status)
    VALUES ('Pending');

    SET @OrderID = SCOPE_IDENTITY();
END

-- Thêm sản phẩm vào Order
CREATE OR ALTER PROCEDURE PROC_AddOrderItem
(
    @OrderID INT,
    @ProductID VARCHAR(100),
    @Quantity INT
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Status NVARCHAR(20);

    SELECT @Status = Status FROM Orders WHERE OrderID = @OrderID;
    IF @Status <> 'Pending'
        THROW 50001, 'Order not editable', 1;

    DECLARE @Price DECIMAL(18,2);
    SELECT @Price = DefaultSellingPrice FROM Products WHERE ProductID = @ProductID;

    INSERT INTO OrderDetails(OrderID, ProductID, Quantity, UnitPrice)
    VALUES (@OrderID, @ProductID, @Quantity, @Price);

    DECLARE @ODID INT = SCOPE_IDENTITY();
    DECLARE @Remain INT = @Quantity;

    DECLARE cur CURSOR FOR
    SELECT BatchID
    FROM ImportBatches
    WHERE ProductID = @ProductID
    ORDER BY BatchNumber;

    DECLARE @BatchID INT;

    OPEN cur;
    FETCH NEXT FROM cur INTO @BatchID;

    WHILE @@FETCH_STATUS = 0 AND @Remain > 0
    BEGIN
        DECLARE @Stock INT;

        SELECT @Stock = ISNULL(SUM(QuantityChange),0)
        FROM InventoryTransactions
        WHERE BatchID = @BatchID AND IsSellable = 1;

        IF @Stock > 0
        BEGIN
            DECLARE @Take INT = CASE WHEN @Stock >= @Remain THEN @Remain ELSE @Stock END;

            INSERT INTO SaleAllocations(OrderDetailID, BatchID, Quantity)
            VALUES (@ODID, @BatchID, @Take);

            INSERT INTO InventoryTransactions
            VALUES (@BatchID, -@Take, 'RESERVE', 1, @OrderID, SYSUTCDATETIME());

            SET @Remain -= @Take;
        END

        FETCH NEXT FROM cur INTO @BatchID;
    END

    CLOSE cur;
    DEALLOCATE cur;

    IF @Remain > 0
        THROW 50002, 'Not enough stock', 1;
END

-- Cập nhật đơn pending
CREATE OR ALTER PROCEDURE PROC_UpdateOrderItem
(
    @OrderDetailID INT,
    @NewQuantity INT
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @OldQuantity INT, @OrderID INT, @ProductID VARCHAR(100);

    SELECT
        @OldQuantity = Quantity,
        @OrderID = OrderID,
        @ProductID = ProductID
    FROM OrderDetails
    WHERE OrderDetailID = @OrderDetailID;

    DECLARE @Diff INT = @NewQuantity - @OldQuantity;

    -- CASE 1: TĂNG SỐ LƯỢNG
    IF @Diff > 0
    BEGIN
        DECLARE @Remain INT = @Diff;

        -- Duyệt FIFO batch
        DECLARE cur CURSOR FOR
        SELECT B.BatchID
        FROM ImportBatches B
        WHERE B.ProductID = @ProductID
        ORDER BY B.ImportDate, B.BatchID;

        DECLARE @BatchID INT;

        OPEN cur;
        FETCH NEXT FROM cur INTO @BatchID;

        WHILE @@FETCH_STATUS = 0 AND @Remain > 0
        BEGIN
            DECLARE @Stock INT;

            SELECT @Stock = ISNULL(SUM(QuantityChange),0)
            FROM InventoryTransactions
            WHERE BatchID = @BatchID AND IsSellable = 1;

            IF @Stock > 0
            BEGIN
                DECLARE @Take INT = CASE WHEN @Stock >= @Remain THEN @Remain ELSE @Stock END;

                -- kiểm tra đã có allocation batch này chưa
                DECLARE @AllocID INT;

                SELECT @AllocID = AllocationID
                FROM SaleAllocations
                WHERE OrderDetailID = @OrderDetailID
                  AND BatchID = @BatchID;

                IF @AllocID IS NOT NULL
                BEGIN
                    -- update vào batch cũ
                    UPDATE SaleAllocations
                    SET Quantity = Quantity + @Take
                    WHERE AllocationID = @AllocID;
                END
                ELSE
                BEGIN
                    -- insert mới nếu chưa có
                    INSERT INTO SaleAllocations(OrderDetailID, BatchID, Quantity)
                    VALUES (@OrderDetailID, @BatchID, @Take);
                END

                INSERT INTO InventoryTransactions
                (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
                VALUES
                (@BatchID, -@Take, 'RESERVE', 1, @OrderID);

                SET @Remain -= @Take;
            END

            FETCH NEXT FROM cur INTO @BatchID;
        END

        CLOSE cur;
        DEALLOCATE cur;

        IF @Remain > 0
            THROW 50002, 'Not enough stock', 1;
    END

    -- CASE 2: GIẢM SỐ LƯỢNG (REVERSE FIFO)
    ELSE IF @Diff < 0
    BEGIN
        DECLARE @Need INT = ABS(@Diff);

        DECLARE cur2 CURSOR FOR
        SELECT AllocationID, BatchID, Quantity
        FROM SaleAllocations
        WHERE OrderDetailID = @OrderDetailID
        ORDER BY AllocationID DESC;

        DECLARE @AllocID INT, @BatchID INT, @Qty INT;

        OPEN cur2;
        FETCH NEXT FROM cur2 INTO @AllocID, @BatchID, @Qty;

        WHILE @@FETCH_STATUS = 0 AND @Need > 0
        BEGIN
            DECLARE @Take INT = CASE WHEN @Qty >= @Need THEN @Need ELSE @Qty END;

            UPDATE SaleAllocations
            SET Quantity = Quantity - @Take
            WHERE AllocationID = @AllocID;

            INSERT INTO InventoryTransactions
            (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
            VALUES
            (@BatchID, @Take, 'CANCEL_RESERVE', 1, @OrderID);

            SET @Need -= @Take;

            FETCH NEXT FROM cur2 INTO @AllocID, @BatchID, @Qty;
        END

        CLOSE cur2;
        DEALLOCATE cur2;

        -- dọn sạch dòng = 0
        DELETE FROM SaleAllocations
        WHERE OrderDetailID = @OrderDetailID
          AND Quantity = 0;
    END

    -- UPDATE OrderDetail
    UPDATE OrderDetails
    SET Quantity = @NewQuantity
    WHERE OrderDetailID = @OrderDetailID;
END

-- Xóa sản phẩm khỏi Order
CREATE OR ALTER PROCEDURE PROC_RemoveOrderItem
(
    @OrderDetailID INT
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @OrderID INT;

    SELECT @OrderID = OrderID
    FROM OrderDetails
    WHERE OrderDetailID = @OrderDetailID;

    IF @OrderID IS NULL
        THROW 50001, 'OrderDetail not found', 1;

    -- 1. Hoàn lại kho (CANCEL_RESERVE)
    INSERT INTO InventoryTransactions
    (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
    SELECT
        BatchID,
        Quantity,
        'CANCEL_RESERVE',
        1,
        @OrderID
    FROM SaleAllocations
    WHERE OrderDetailID = @OrderDetailID;

    -- 2. Xóa allocation
    DELETE FROM SaleAllocations
    WHERE OrderDetailID = @OrderDetailID;

    -- 3. Xóa order item
    DELETE FROM OrderDetails
    WHERE OrderDetailID = @OrderDetailID;
END

-- Thanh toán
CREATE OR ALTER PROCEDURE PROC_PayOrder
(
    @OrderID INT
)
AS
BEGIN
    UPDATE Orders
    SET Status = 'Paid',
        PaidDate = SYSUTCDATETIME()
    WHERE OrderID = @OrderID;

    INSERT INTO InventoryTransactions
    (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
    SELECT BatchID, 0, 'SALE', 1, @OrderID
    FROM SaleAllocations SA
    JOIN OrderDetails OD ON SA.OrderDetailID = OD.OrderDetailID
    WHERE OD.OrderID = @OrderID;
END

-- Hủy đơn
CREATE OR ALTER PROCEDURE PROC_CancelOrder
(
    @OrderID INT
)
AS
BEGIN
    INSERT INTO InventoryTransactions
    SELECT BatchID, Quantity, 'CANCEL_RESERVE', 1, @OrderID
    FROM SaleAllocations SA
    JOIN OrderDetails OD ON SA.OrderDetailID = OD.OrderDetailID
    WHERE OD.OrderID = @OrderID;

    DELETE SA
    FROM SaleAllocations SA
    JOIN OrderDetails OD ON SA.OrderDetailID = OD.OrderDetailID
    WHERE OD.OrderID = @OrderID;

    UPDATE Orders
    SET Status = 'Cancelled'
    WHERE OrderID = @OrderID;
END

-- Trả hàng
CREATE OR ALTER PROCEDURE PROC_ReturnOrder
(
    @OrderID INT,
    @ReturnItems ReturnItemType READONLY
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ReturnID INT;

    INSERT INTO Returns(OrderID, RefundAmount)
    VALUES (@OrderID, 0);

    SET @ReturnID = SCOPE_IDENTITY();

    INSERT INTO ReturnDetails
    (ReturnID, OrderDetailID, Quantity, RefundUnitPrice, Reason)
    SELECT @ReturnID, OrderDetailID, Quantity, RefundUnitPrice, Reason
    FROM @ReturnItems;

    DECLARE cur CURSOR FOR
    SELECT OrderDetailID, Quantity FROM @ReturnItems;

    DECLARE @ODID INT, @Qty INT;

    OPEN cur;
    FETCH NEXT FROM cur INTO @ODID, @Qty;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        DECLARE @Remain INT = @Qty;

        DECLARE batch_cur CURSOR FOR
        SELECT BatchID, Quantity
        FROM SaleAllocations
        WHERE OrderDetailID = @ODID
        ORDER BY AllocationID; -- FIFO

        DECLARE @BatchID INT, @AllocQty INT;

        OPEN batch_cur;
        FETCH NEXT FROM batch_cur INTO @BatchID, @AllocQty;

        WHILE @@FETCH_STATUS = 0 AND @Remain > 0
        BEGIN
            DECLARE @Take INT =
                CASE WHEN @AllocQty >= @Remain THEN @Remain ELSE @AllocQty END;

            INSERT INTO InventoryTransactions
            (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
            VALUES
            (@BatchID, @Take, 'RETURN', 0, @ReturnID);

            SET @Remain -= @Take;

            FETCH NEXT FROM batch_cur INTO @BatchID, @AllocQty;
        END

        CLOSE batch_cur;
        DEALLOCATE batch_cur;

        FETCH NEXT FROM cur INTO @ODID, @Qty;
    END

    CLOSE cur;
    DEALLOCATE cur;

    UPDATE Returns
    SET RefundAmount = (
        SELECT SUM(Quantity * RefundUnitPrice)
        FROM ReturnDetails
        WHERE ReturnID = @ReturnID
    )
    WHERE ReturnID = @ReturnID;
END

-- Xử lý hàng lỗi
CREATE OR ALTER PROCEDURE PROC_AddDefectiveProduct
(
    @BatchID INT,
    @Quantity INT,
    @Reason NVARCHAR(255),
)
AS
BEGIN
    SET NOCOUNT ON;

    --------------------------------------------------
    -- 0. Validate input
    --------------------------------------------------
    IF @Quantity <= 0
        THROW 50000, 'Quantity must be > 0', 1;

    -- 1. Kiểm tra tồn bán được
    DECLARE @Stock INT;

    SELECT @Stock = ISNULL(SUM(QuantityChange),0)
    FROM InventoryTransactions
    WHERE BatchID = @BatchID
      AND IsSellable = 1;

    IF @Stock < @Quantity
        THROW 50002, 'Not enough sellable stock', 1;

    -- 2. Lấy giá nhập (để báo cáo lỗ)
    DECLARE @ImportPrice DECIMAL(18,2);

    SELECT @ImportPrice = ImportPrice
    FROM ImportBatches
    WHERE BatchID = @BatchID;

    IF @ImportPrice IS NULL
        THROW 50003, 'Batch not found', 1;

    -- 3. Ghi nhận hàng lỗi
    DECLARE @DefectiveID INT;

    INSERT INTO DefectiveProducts
    (BatchID, Quantity, Reason, ImportPrice, CreatedDate)
    VALUES
    (@BatchID, @Quantity, @Reason, @ImportPrice, SYSUTCDATETIME());

    SET @DefectiveID = SCOPE_IDENTITY();

    -- 4. Trừ tồn kho (DEFECT)
    INSERT INTO InventoryTransactions
    (BatchID, QuantityChange, TransactionType, IsSellable, ReferenceID)
    VALUES
    (@BatchID, -@Quantity, 'DEFECT', 1, @DefectiveID);
END

-- ============== FUNCTION ==================
CREATE OR ALTER FUNCTION FN_GetNextBatchNumber
(
    @ProductID VARCHAR(100)
)
RETURNS INT
AS
BEGIN
    DECLARE @Next INT;

    SELECT @Next = ISNULL(MAX(BatchNumber), 0) + 1
    FROM ImportBatches
    WHERE ProductID = @ProductID;

    RETURN @Next;
END
```
