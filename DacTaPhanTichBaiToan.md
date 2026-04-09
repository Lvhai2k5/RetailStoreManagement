## ĐẶC TẢ NGHIỆP VỤ (BUSINESS ONLY) _ReatilStoreManagement

Hệ thống được xây dựng nhằm hỗ trợ quản lý hoạt động kinh doanh của một cửa hàng bán lẻ với hai quy trình chính: mua tại chỗ và đặt trước. Trong quy trình mua tại chỗ, khách hàng trực tiếp đến cửa hàng, chủ cửa hàng tạo đơn hàng, hệ thống ghi nhận các sản phẩm được chọn, tính toán giá bán hiện tại và thực hiện thanh toán ngay bằng tiền mặt hoặc chuyển khoản. Sau khi thanh toán hoàn tất, đơn hàng được xác nhận và có thể in hóa đơn.

Trong quy trình đặt trước, khách hàng liên hệ qua điện thoại để đặt hàng. Chủ cửa hàng tạo đơn hàng ở trạng thái chờ thanh toán. Trong giai đoạn này, đơn hàng có thể được chỉnh sửa linh hoạt: thêm sản phẩm, thay đổi số lượng hoặc loại bỏ sản phẩm theo yêu cầu của khách. Hệ thống đảm bảo giữ trước số lượng hàng hóa tương ứng để tránh việc bán vượt tồn kho. Khi khách đến nhận hàng, đơn hàng được thanh toán và hoàn tất. Nếu khách không còn nhu cầu, đơn hàng có thể bị hủy và số lượng hàng đã giữ sẽ được trả lại kho.

Hệ thống quản lý hàng hóa theo từng lô nhập. Mỗi lần nhập hàng tạo ra một lô mới với thông tin về số lượng, giá nhập và giá bán áp dụng cho lô đó. Giá bán của từng lô là cố định sau khi nhập. Tuy nhiên, giá bán chung của sản phẩm trên thị trường được xác định động dựa trên giá bán của hai lô gần nhất, giúp phản ánh biến động giá nhập. Ngoài ra, chủ cửa hàng có thể điều chỉnh giá bán này thủ công khi cần thiết, và mọi thay đổi đều được lưu lại để theo dõi lịch sử.

Tồn kho không được lưu dưới dạng một con số tĩnh mà được xác định dựa trên toàn bộ các biến động phát sinh trong hệ thống. Các hoạt động như nhập hàng, giữ hàng cho đơn đặt trước, bán hàng, hủy giữ hàng, trả hàng hoặc ghi nhận hàng lỗi đều ảnh hưởng đến số lượng tồn. Điều này giúp đảm bảo tính chính xác và khả năng truy vết toàn bộ lịch sử thay đổi của kho.

Hệ thống cho phép khách hàng trả lại một phần hoặc toàn bộ sản phẩm trong một đơn hàng đã thanh toán. Khi thực hiện trả hàng, người dùng có thể chọn một hoặc nhiều sản phẩm trong đơn, nhập số lượng trả (không vượt quá số lượng đã mua) và xác định giá hoàn tiền cho từng sản phẩm. Hệ thống sẽ ghi nhận thông tin trả hàng và tính toán tổng số tiền hoàn trả tương ứng.

Các sản phẩm được trả sẽ được đưa trở lại kho nhưng không được phép bán lại, nhằm tránh các vấn đề liên quan đến chất lượng hoặc kiểm soát hàng hóa. Đồng thời, hệ thống ghi nhận khoản hoàn tiền để điều chỉnh doanh thu. Doanh thu thực tế được tính bằng tổng giá trị các đơn hàng đã thanh toán trừ đi tổng số tiền hoàn trả từ các phiếu trả hàng.

Hệ thống cũng hỗ trợ quản lý nhà cung cấp, cho phép lưu trữ và cập nhật thông tin như tên, số điện thoại, địa chỉ và danh sách sản phẩm cung cấp. Ngoài ra, hệ thống quản lý hàng lỗi hoặc hết hạn bằng cách ghi nhận thông tin sản phẩm, lý do và giá nhập, đồng thời loại bỏ các sản phẩm này khỏi lượng hàng có thể bán.

Cách tiếp cận này đảm bảo phản ánh chính xác tình hình kinh doanh thực tế và tách biệt rõ ràng giữa hoạt động quản lý tồn kho và hoạt động tài chính.

## ĐẶC TẢ NGHIỆP VỤ LIÊN HỆ CSDL (TECHNICAL VIEW) _ReatilStoreManagement

Hệ thống quản lý bán lẻ được thiết kế dựa trên mô hình dữ liệu quan hệ, trong đó các thực thể chính bao gồm Products, ImportBatches, Orders, OrderDetails, InventoryTransactions, SaleAllocations, ProductPriceHistory, Suppliers, Returns và các bảng liên quan khác.

Mỗi sản phẩm được lưu trong bảng Products, bao gồm giá bán mặc định (DefaultSellingPrice). Giá này không cố định mà được cập nhật tự động khi có lô hàng mới được nhập vào bảng ImportBatches. Cụ thể, khi một bản ghi mới được thêm vào ImportBatches, hệ thống tính toán giá bán mới bằng trung bình cộng giữa giá bán của lô vừa nhập và lô trước đó (nếu tồn tại), sau đó cập nhật vào Products. Đồng thời, mọi thay đổi giá đều được ghi nhận vào bảng ProductPriceHistory với lý do tương ứng như thay đổi do nhập lô mới hoặc do người dùng chỉnh sửa.

Bảng ImportBatches lưu thông tin từng lô hàng bao gồm ProductID, BatchNumber, Quantity, ImportPrice và SellingPrice. Khi một lô được nhập, trigger sẽ tự động tạo một bản ghi trong bảng InventoryTransactions với TransactionType là 'IMPORT' và QuantityChange dương, thể hiện việc tăng tồn kho.

Tồn kho của hệ thống không được lưu trực tiếp mà được tính toán dựa trên bảng InventoryTransactions. Mỗi bản ghi trong bảng này thể hiện một biến động kho với các thuộc tính như BatchID, QuantityChange, TransactionType và IsSellable. Các loại giao dịch bao gồm: IMPORT (tăng tồn), RESERVE (giữ hàng), SALE (bán hàng), CANCEL_RESERVE (hoàn giữ), RETURN (trả hàng) và các loại khác như hàng lỗi. Trường IsSellable cho biết số lượng đó còn có thể bán hay không.

Quy trình tạo đơn hàng được tách thành nhiều bước. Khi tạo một đơn mới trong bảng Orders, hệ thống thiết lập trạng thái là 'Pending' hoặc 'Paid' tùy theo hình thức mua. Các sản phẩm trong đơn được lưu tại bảng OrderDetails, mỗi bản ghi đại diện cho một sản phẩm với số lượng và giá bán tại thời điểm thêm vào đơn.

Khi một sản phẩm được thêm vào đơn (thông qua procedure), hệ thống thực hiện phân bổ hàng từ các lô trong ImportBatches theo nguyên tắc FIFO và ghi nhận kết quả vào bảng SaleAllocations. Đồng thời, hệ thống tạo các bản ghi tương ứng trong InventoryTransactions với TransactionType là 'RESERVE' nếu đơn ở trạng thái Pending hoặc 'SALE' nếu là mua tại chỗ. Điều này đảm bảo hàng được giữ trước hoặc bán ngay tùy theo trạng thái đơn.

Khi chỉnh sửa đơn hàng (chỉ áp dụng cho trạng thái Pending), hệ thống so sánh số lượng mới với số lượng cũ trong OrderDetails. Nếu số lượng tăng, hệ thống thực hiện thêm RESERVE thông qua việc phân bổ thêm từ các lô. Nếu số lượng giảm, hệ thống tạo các bản ghi CANCEL_RESERVE để hoàn lại phần đã giữ. Nếu một sản phẩm bị xóa khỏi đơn, toàn bộ số lượng đã phân bổ trong SaleAllocations sẽ được hoàn trả về kho thông qua các transaction tương ứng.

Khi đơn hàng được thanh toán, hệ thống cập nhật trạng thái của Orders sang 'Paid' và tạo các bản ghi SALE trong InventoryTransactions dựa trên dữ liệu từ SaleAllocations. Điều này đánh dấu việc hàng hóa chính thức được bán và có thể dùng để tính doanh thu. Nếu đơn hàng bị hủy, hệ thống tạo các bản ghi CANCEL_RESERVE để hoàn trả toàn bộ số lượng đã giữ trước đó, đồng thời cập nhật trạng thái đơn thành 'Cancelled'.

Quy trình trả hàng được thực hiện thông qua bảng Returns và ReturnDetails. Khi người dùng thực hiện trả hàng, hệ thống tạo một bản ghi trong bảng Returns và nhiều bản ghi tương ứng trong ReturnDetails, mỗi bản ghi đại diện cho một sản phẩm được trả. Mỗi ReturnDetail liên kết với một OrderDetail để đảm bảo việc kiểm soát số lượng trả không vượt quá số lượng đã mua.

Sau đó, hệ thống tạo các bản ghi trong InventoryTransactions với TransactionType là RETURN, QuantityChange dương và IsSellable = 0. Điều này phản ánh việc hàng hóa được đưa trở lại kho nhưng không thể bán lại. ReferenceID của các transaction này trỏ đến ReturnID để phục vụ truy vết.

Tổng số tiền hoàn trả được tính bằng tổng của Quantity nhân với RefundUnitPrice trong bảng ReturnDetails và được lưu tại trường RefundAmount trong bảng Returns.

Doanh thu được tính toán dựa trên bảng Orders và OrderDetails, cụ thể là tổng giá trị của các đơn có trạng thái 'Paid', trừ đi tổng số tiền hoàn trả trong bảng Returns. Bảng InventoryTransactions chỉ được sử dụng để theo dõi biến động kho và không được dùng trực tiếp để tính doanh thu nhằm tránh sai lệch do các giao dịch trung gian như RESERVE hoặc CANCEL_RESERVE.

Thiết kế này đảm bảo tách biệt rõ ràng giữa quản lý tồn kho và quản lý tài chính, đồng thời hỗ trợ đầy đủ các nghiệp vụ thực tế như đặt trước, chỉnh sửa đơn, nhập hàng theo lô, quản lý giá động và truy vết lịch sử hệ thống.
