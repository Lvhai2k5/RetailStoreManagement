INSERT INTO ProductTypesMarkup (Name, MarkupPercent)
SELECT * FROM (VALUES
                   (N'Đồ uống', 20.00),
                   (N'Mì gói', 15.00),
                   (N'Bánh kẹo', 18.00),
                   (N'Đồ ăn vặt', 22.00),
                   (N'Sữa & chế phẩm từ sữa', 12.00),
                   (N'Đồ gia dụng', 25.00),
                   (N'Mỹ phẩm', 30.00),
                   (N'Vệ sinh cá nhân', 28.00),
                   (N'Đông lạnh', 10.00),
                   (N'Thực phẩm tươi sống', 8.00)
              ) AS v(Name, MarkupPercent)
WHERE NOT EXISTS (
    SELECT 1 FROM ProductTypesMarkup
);