-- 建立資料庫指令
-- CREATE DATABASE shoppeDB COLLATE Chinese_Taiwan_Stroke_CI_AS; --

use shoppeDB;


--角色表(可操作的資料範圍)
create table [Role](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --角色id
[name] NVARCHAR(20), --角色名稱(買家、賣家、管理者、超級管理者)
)

--權限表(可執行的操作)
create table [Permission](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --權限id
[name] NVARCHAR(20), --權限名稱(R0、C3、U5、D7、super9)
)

--角色_權限表
create table [Role_Permission](
[role_id] INT NOT NULL, --角色id
[permission_id] INT NOT NULL, --權限id
PRIMARY KEY([role_id],[permission_id]), --(複合主鍵)
FOREIGN KEY ([role_id]) REFERENCES [Role]([id]),
FOREIGN KEY ([permission_id]) REFERENCES [Permission]([id])
)

--使用者_角色表(一個使用者可以有多個角色)
create table [User_Role](
[user_id] INT NOT NULL, --使用者id
[role_id] INT NOT NULL, --角色id
PRIMARY KEY([user_id],[role_id]), --(複合主鍵)
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([role_id]) REFERENCES [Role]([id])
)


--使用者表
create table [User](
[user_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --使用者id
[user_name] NVARCHAR(50) NOT NULL, --使用者名稱
[email] NVARCHAR(255) NOT NULL, --email
[password] NVARCHAR(50) NOT NULL, --password_hash
[phone] NVARCHAR(20) NOT NULL, --phone
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE() --最後更新時間
)


--購物車明細表
create table [Cart](
[cart_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --購物車id
[user_id] INT NOT NULL, --使用者id
[SKU_id] INT NOT NULL, --SKU id
[quantity] INT NOT NULL, --商品數量
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
CONSTRAINT [unique_cart] UNIQUE ([user_id], [SKU_id]), --約束名稱，非欄位名稱，防止重複數據(@Table(name = "[cart]", uniqueConstraints = @UniqueConstraint(columnNames = {"[user_id]", "[SKU_id]"})))
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([SKU_id]) REFERENCES [SKU]([SKU_id])
)


--訂單表
create table [Order](
[order_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --訂單id
[user_id] INT NOT NULL, --買家id(=使用者id)
[order_address_id_billing] INT NOT NULL, --訂單地址id(帳單)
[order_address_id_shipping] INT NOT NULL, --訂單地址id(配送)
[order_status_id] INT NOT NULL, --當前訂單狀態id
[total_price] DECIMAL(10,2) NOT NULL, --總金額
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([order_address_id_billing]) REFERENCES [OrderAddress]([order_address_id]),
FOREIGN KEY ([order_address_id_shipping]) REFERENCES [OrderAddress]([order_address_id]),
FOREIGN KEY ([order_status_id]) REFERENCES [OrderStatusCorrespond]([id])
)


--訂單商品明細表
CREATE TABLE [OrderItem] (
[item_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --訂單商品明細id
[order_id] INT NOT NULL, --訂單id
[sku_id] INT NOT NULL, --商品SKU id
[shop_id] INT NOT NULL, --店鋪id
[unit_price] DECIMAL(10,2) NOT NULL, --商品價格
[quantity] INT NOT NULL, --商品數量
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([sku_id]) REFERENCES [SKU]([sku_id]),
FOREIGN KEY ([shop_id]) REFERENCES [Shop]([shop_id])
)


--店鋪表
CREATE TABLE [Shop](
[shop_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --店鋪id
[user_id] INT NOT NULL, --賣家id(=使用者id)
[c1_id] INT NOT NULL, -- 店鋪的主要分類
[return_city] NVARCHAR(50) NOT NULL, --城市(退貨)
[return_district] NVARCHAR(50) NOT NULL, --區(退貨)
[return_zip_code] NVARCHAR(50) NOT NULL, --郵遞區號(退貨)
[return_street_etc] NVARCHAR(255), --街道,巷弄,門號,樓層(退貨)
[return_recipient_name] NVARCHAR(50) NOT NULL, --收件人姓名(退貨)
[return_recipient_phone] NVARCHAR(20) NOT NULL, --收件人電話(退貨)
[shop_name] NVARCHAR(255) NOT NULL, --店鋪名稱
[description] NVARCHAR(MAX), --店鋪介紹
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]), --一對一
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]) --多對一
)


--用戶地址表(允許買家存常用地址)
CREATE TABLE [UserAddress] (
[user_address_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --用戶地址id
[user_id] INT NOT NULL, --使用者id
[address_type_id] INT, --地址類型id(帳單/配送)
[is_default] BIT DEFAULT 1, --是否為預設地址
[city] NVARCHAR(50) NOT NULL, --城市
[district] NVARCHAR(50) NOT NULL, --區
[zip_code] NVARCHAR(50) NOT NULL, --郵遞區號
[street_etc] NVARCHAR(255), --街道,巷弄,門號,樓層
[recipient_name] NVARCHAR(50) NOT NULL, --收件人姓名
[recipient_phone] NVARCHAR(20) NOT NULL, --收件人電話
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([address_type_id]) REFERENCES [AddressTypeCorrespond]([id]),
CONSTRAINT [unique_default_address] UNIQUE ([user_id], [address_type_id], [is_default]) --限制每位使用者每種地址類型只能有一個預設地址
)



--訂單地址表(為了讓訂單地址不受使用者變更常用地址的影響而建。不須跟訂單建立關聯，避免循環依賴)
CREATE TABLE [OrderAddress] (
[order_address_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --訂單地址id
[address_type_id] INT NOT NULL, --地址類型id
[city] NVARCHAR(50) NOT NULL, --城市
[district] NVARCHAR(50) NOT NULL, --區
[zip_code] NVARCHAR(50) NOT NULL, --郵遞區號
[street_etc] NVARCHAR(255), --街道,巷弄,門號,樓層
[recipient_name] NVARCHAR(50) NOT NULL, --收件人姓名
[recipient_phone] NVARCHAR(50) NOT NULL, --收件人電話
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
FOREIGN KEY ([address_type_id]) REFERENCES [AddressTypeCorrespond]([id])
)

--地址類型對照表
CREATE TABLE [AddressTypeCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --地址類型id
[name] NVARCHAR(20) --地址類型(帳單billing、配送shipping)
)




--支付表(產生一筆訂單資料時，就會產生一筆支付資料)
CREATE TABLE [Payment] (
[payment_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --支付id
[order_id] INT NOT NULL, --訂單id(支付金額=訂單金額)
[payment_method_id] INT NOT NULL, --支付方式id
[payment_status_id] INT NOT NULL, --當前支付狀態id
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([payment_method_id]) REFERENCES [PaymentMethodCorrespond]([id]),
FOREIGN KEY ([payment_status_id]) REFERENCES [PaymentStatusCorrespond]([id])
)


--支付方式對照表
CREATE TABLE [PaymentMethodCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --支付方式id
[name] NVARCHAR(20) NOT NULL --支付方式名稱(Visa、PayPal、Apple pay、Line pay、貨到付款)
)


--支付狀態對照表
CREATE TABLE [PaymentStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --支付狀態id
[name] NVARCHAR(20) --支付狀態名稱(尚未付款Pending、已付款Paid、已退款Refunded)
)


--物流表
CREATE TABLE [Shipment] (
[shipment_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --物流id
[order_id] INT NOT NULL, --訂單id
[shipping_method_id] INT NOT NULL, --配送方式id
[shipping_status_id] INT NOT NULL, --當前配送狀態id
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([shipping_method_id]) REFERENCES [ShipmentMethodCorrespond]([id]),
FOREIGN KEY ([shipping_status_id]) REFERENCES [ShipmentStatusCorrespond]([id])
)

--配送方式對照表
CREATE TABLE [ShipmentMethodCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --配送方式id
[name] NVARCHAR(20) NOT NULL --配送方式名稱(宅配、超商取貨)
)

--配送狀態對照表
CREATE TABLE [ShipmentStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --配送狀態id
[name] NVARCHAR(20) --配送狀態名稱(待出貨、已出貨、運送中、已送達)
)

--訂單狀態歷史表(一筆訂單有多個狀態變更的歷史紀錄)
CREATE TABLE [OrderStatusHistory] (
[history_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --訂單狀態歷史id
[order_id] INT NOT NULL, --訂單id
[order_status_id] INT NOT NULL, --訂單狀態對照id
[created_at] DATETIME DEFAULT GETDATE(), --狀態變更時間
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([order_status_id]) REFERENCES [OrderStatusCorrespond]([id])
)

--訂單狀態對照表
CREATE TABLE [OrderStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --訂單狀態id
[name] NVARCHAR(20) --訂單狀態名稱(待付款、待出貨、待收貨、退貨/退款、訂單已完成、不成立)
)






--一級分類表
CREATE TABLE [Category1](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --一級分類id
[name] NVARCHAR(255) NOT NULL, --一級分類名稱
)

--二級分類表
CREATE TABLE [Category2](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --二級分類id
[name] NVARCHAR(255) NOT NULL, --二級分類名稱
)

--一級_二級分類關聯表(支持同一個二級分類（牛仔褲）可以屬於多個一級分類（女生衣著 / 男生衣著）)
CREATE TABLE [Category1_Category2](
[c1_id] INT NOT NULL, --一級分類id
[c2_id] INT NOT NULL, --二級分類id
PRIMARY KEY ([c1_id], [c2_id]), --(複合主鍵)
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]),
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id])
)

--三級分類表
CREATE TABLE [Category3](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --三級分類id
[c2_id] INT, --二級分類id
[name] NVARCHAR(255) NOT NULL, --三級分類名稱
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id])
)



--商品表
CREATE TABLE [Product](
[product_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --商品id
[shop_id] INT NOT NULL, --所屬店鋪
[c1_id] INT NOT NULL, --商品的一級分類
[c2_id] INT NOT NULL, --商品的二級分類
[c3_id] INT NULL,  --第三層分類(至少選兩級分類，第三級可選填)(可以是NULL!!)
[product_name] NVARCHAR(255) NOT NULL, --商品名稱
[description] NVARCHAR(MAX) NULL, --商品描述
[created_at] DATETIME DEFAULT GETDATE(), --創建時間
[updated_at] DATETIME DEFAULT GETDATE(), --最後更新時間
FOREIGN KEY ([shop_id]) REFERENCES [Shop]([shop_id]),
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]),
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id]),
FOREIGN KEY ([c3_id]) REFERENCES [Category3]([id])
)

--屬性表
CREATE TABLE [Attribute](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --屬性id
[name] NVARCHAR(50) NOT NULL --屬性名稱
)

--(二級)分類_屬性表  (不管商品是二層分類還是三層分類，都根據二層分類產生可選填的屬性)(一筆資料: 某個二級分類 與 某個屬性 之間的關係，並指定該屬性是否必填)
CREATE TABLE [Category2_Attribute](
[c2_id] INT NOT NULL, --第二層分類id
[attribute_id] INT NOT NULL, --屬性id
[is_required] BIT DEFAULT 0, --是否必填
PRIMARY KEY ([c2_id],[attribute_id]), --(複合主鍵)
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id]),
FOREIGN KEY ([attribute_id]) REFERENCES [Attribute]([id])
)

--屬性值表
CREATE TABLE [AttributeValue](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --屬性值id
[attribute_id] INT NOT NULL, --屬性id
[name] NVARCHAR(50) NOT NULL, --屬性值名稱
FOREIGN KEY ([attribute_id]) REFERENCES [Attribute]([id])
)

--商品_屬性值表 (一筆資料: 某個商品的某個屬性具有某個屬性值)
CREATE TABLE [ProductAttributeValue](
[product_id] INT NOT NULL, --商品id
[attribute_value_id] INT NOT NULL, --屬性值id
PRIMARY KEY([product_id],[attribute_value_id]), --(複合主鍵)
FOREIGN KEY ([product_id]) REFERENCES [Product]([product_id]),
FOREIGN KEY ([attribute_value_id]) REFERENCES [AttributeValue]([id])
)

--規格表 (一筆資料: 一個規格名稱)
CREATE TABLE [Specification](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --規格id
[name] NVARCHAR(50) NOT NULL, --規格名稱
[is_default] BIT NOT NULL DEFAULT 0 --是否為平台預設（1=預設, 0=賣家自訂）
)

--規格值表 (一筆資料: 某規格的所有可能值)
CREATE TABLE [SpecificationValue](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --規格值id
[specification_id] INT NOT NULL, --規格id
[name] NVARCHAR(50) NOT NULL, --規格值名稱
FOREIGN KEY ([specification_id]) REFERENCES [Specification]([id])
)

--SKU表 (商品_規格組合表)(庫存單位表)(一筆資料: 某個商品的某個規格組合)
CREATE TABLE [SKU] (
[SKU_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --SKU id
[product_id] INT NOT NULL, --商品表id
[stock] INT NOT NULL DEFAULT 0, --庫存
[price] DECIMAL(10,2) NOT NULL, --價錢
FOREIGN KEY ([product_id]) REFERENCES [Product]([product_id])
);

--SKU細節表(SKU_規格值表)
CREATE TABLE [SKU_SpecificationValue] (
[SKU_id] INT NOT NULL, --SKU id
[specification_value_id] INT NOT NULL, --規格值id
PRIMARY KEY ([SKU_id], [specification_value_id]), --(複合主鍵)
FOREIGN KEY ([SKU_id]) REFERENCES [SKU]([SKU_id]),
FOREIGN KEY ([specification_value_id]) REFERENCES [SpecificationValue]([id])
);