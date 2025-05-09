-- �إ߸�Ʈw���O
-- CREATE DATABASE shoppeDB COLLATE Chinese_Taiwan_Stroke_CI_AS; --

use shoppeDB;


--�����(�i�ާ@����ƽd��)
create table [Role](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --����id
[name] NVARCHAR(20), --����W��(�R�a�B��a�B�޲z�̡B�W�ź޲z��)
)

--�v����(�i���檺�ާ@)
create table [Permission](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�v��id
[name] NVARCHAR(20), --�v���W��(R0�BC3�BU5�BD7�Bsuper9)
)

--����_�v����
create table [Role_Permission](
[role_id] INT NOT NULL, --����id
[permission_id] INT NOT NULL, --�v��id
PRIMARY KEY([role_id],[permission_id]), --(�ƦX�D��)
FOREIGN KEY ([role_id]) REFERENCES [Role]([id]),
FOREIGN KEY ([permission_id]) REFERENCES [Permission]([id])
)

--�ϥΪ�_�����(�@�ӨϥΪ̥i�H���h�Ө���)
create table [User_Role](
[user_id] INT NOT NULL, --�ϥΪ�id
[role_id] INT NOT NULL, --����id
PRIMARY KEY([user_id],[role_id]), --(�ƦX�D��)
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([role_id]) REFERENCES [Role]([id])
)


--�ϥΪ̪�
create table [User](
[user_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�ϥΪ�id
[user_name] NVARCHAR(50) NOT NULL, --�ϥΪ̦W��
[email] NVARCHAR(255) NOT NULL, --email
[password] NVARCHAR(50) NOT NULL, --password_hash
[phone] NVARCHAR(20) NOT NULL, --phone
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE() --�̫��s�ɶ�
)


--�ʪ������Ӫ�
create table [Cart](
[cart_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�ʪ���id
[user_id] INT NOT NULL, --�ϥΪ�id
[SKU_id] INT NOT NULL, --SKU id
[quantity] INT NOT NULL, --�ӫ~�ƶq
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
CONSTRAINT [unique_cart] UNIQUE ([user_id], [SKU_id]), --�����W�١A�D���W�١A����Ƽƾ�(@Table(name = "[cart]", uniqueConstraints = @UniqueConstraint(columnNames = {"[user_id]", "[SKU_id]"})))
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([SKU_id]) REFERENCES [SKU]([SKU_id])
)


--�q���
create table [Order](
[order_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�q��id
[user_id] INT NOT NULL, --�R�aid(=�ϥΪ�id)
[order_address_id_billing] INT NOT NULL, --�q��a�}id(�b��)
[order_address_id_shipping] INT NOT NULL, --�q��a�}id(�t�e)
[order_status_id] INT NOT NULL, --��e�q�檬�Aid
[total_price] DECIMAL(10,2) NOT NULL, --�`���B
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([order_address_id_billing]) REFERENCES [OrderAddress]([order_address_id]),
FOREIGN KEY ([order_address_id_shipping]) REFERENCES [OrderAddress]([order_address_id]),
FOREIGN KEY ([order_status_id]) REFERENCES [OrderStatusCorrespond]([id])
)


--�q��ӫ~���Ӫ�
CREATE TABLE [OrderItem] (
[item_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�q��ӫ~����id
[order_id] INT NOT NULL, --�q��id
[sku_id] INT NOT NULL, --�ӫ~SKU id
[shop_id] INT NOT NULL, --���Qid
[unit_price] DECIMAL(10,2) NOT NULL, --�ӫ~����
[quantity] INT NOT NULL, --�ӫ~�ƶq
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([sku_id]) REFERENCES [SKU]([sku_id]),
FOREIGN KEY ([shop_id]) REFERENCES [Shop]([shop_id])
)


--���Q��
CREATE TABLE [Shop](
[shop_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --���Qid
[user_id] INT NOT NULL, --��aid(=�ϥΪ�id)
[c1_id] INT NOT NULL, -- ���Q���D�n����
[return_city] NVARCHAR(50) NOT NULL, --����(�h�f)
[return_district] NVARCHAR(50) NOT NULL, --��(�h�f)
[return_zip_code] NVARCHAR(50) NOT NULL, --�l���ϸ�(�h�f)
[return_street_etc] NVARCHAR(255), --��D,�ѧ�,����,�Ӽh(�h�f)
[return_recipient_name] NVARCHAR(50) NOT NULL, --����H�m�W(�h�f)
[return_recipient_phone] NVARCHAR(20) NOT NULL, --����H�q��(�h�f)
[shop_name] NVARCHAR(255) NOT NULL, --���Q�W��
[description] NVARCHAR(MAX), --���Q����
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]), --�@��@
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]) --�h��@
)


--�Τ�a�}��(���\�R�a�s�`�Φa�})
CREATE TABLE [UserAddress] (
[user_address_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�Τ�a�}id
[user_id] INT NOT NULL, --�ϥΪ�id
[address_type_id] INT, --�a�}����id(�b��/�t�e)
[is_default] BIT DEFAULT 1, --�O�_���w�]�a�}
[city] NVARCHAR(50) NOT NULL, --����
[district] NVARCHAR(50) NOT NULL, --��
[zip_code] NVARCHAR(50) NOT NULL, --�l���ϸ�
[street_etc] NVARCHAR(255), --��D,�ѧ�,����,�Ӽh
[recipient_name] NVARCHAR(50) NOT NULL, --����H�m�W
[recipient_phone] NVARCHAR(20) NOT NULL, --����H�q��
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
FOREIGN KEY ([user_id]) REFERENCES [User]([user_id]),
FOREIGN KEY ([address_type_id]) REFERENCES [AddressTypeCorrespond]([id]),
CONSTRAINT [unique_default_address] UNIQUE ([user_id], [address_type_id], [is_default]) --����C��ϥΪ̨C�ئa�}�����u�঳�@�ӹw�]�a�}
)



--�q��a�}��(���F���q��a�}�����ϥΪ��ܧ�`�Φa�}���v�T�ӫءC������q��إ����p�A�קK�`���̿�)
CREATE TABLE [OrderAddress] (
[order_address_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�q��a�}id
[address_type_id] INT NOT NULL, --�a�}����id
[city] NVARCHAR(50) NOT NULL, --����
[district] NVARCHAR(50) NOT NULL, --��
[zip_code] NVARCHAR(50) NOT NULL, --�l���ϸ�
[street_etc] NVARCHAR(255), --��D,�ѧ�,����,�Ӽh
[recipient_name] NVARCHAR(50) NOT NULL, --����H�m�W
[recipient_phone] NVARCHAR(50) NOT NULL, --����H�q��
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
FOREIGN KEY ([address_type_id]) REFERENCES [AddressTypeCorrespond]([id])
)

--�a�}������Ӫ�
CREATE TABLE [AddressTypeCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�a�}����id
[name] NVARCHAR(20) --�a�}����(�b��billing�B�t�eshipping)
)




--��I��(���ͤ@���q���ƮɡA�N�|���ͤ@����I���)
CREATE TABLE [Payment] (
[payment_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --��Iid
[order_id] INT NOT NULL, --�q��id(��I���B=�q����B)
[payment_method_id] INT NOT NULL, --��I�覡id
[payment_status_id] INT NOT NULL, --��e��I���Aid
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([payment_method_id]) REFERENCES [PaymentMethodCorrespond]([id]),
FOREIGN KEY ([payment_status_id]) REFERENCES [PaymentStatusCorrespond]([id])
)


--��I�覡��Ӫ�
CREATE TABLE [PaymentMethodCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --��I�覡id
[name] NVARCHAR(20) NOT NULL --��I�覡�W��(Visa�BPayPal�BApple pay�BLine pay�B�f��I��)
)


--��I���A��Ӫ�
CREATE TABLE [PaymentStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --��I���Aid
[name] NVARCHAR(20) --��I���A�W��(�|���I��Pending�B�w�I��Paid�B�w�h��Refunded)
)


--���y��
CREATE TABLE [Shipment] (
[shipment_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --���yid
[order_id] INT NOT NULL, --�q��id
[shipping_method_id] INT NOT NULL, --�t�e�覡id
[shipping_status_id] INT NOT NULL, --��e�t�e���Aid
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([shipping_method_id]) REFERENCES [ShipmentMethodCorrespond]([id]),
FOREIGN KEY ([shipping_status_id]) REFERENCES [ShipmentStatusCorrespond]([id])
)

--�t�e�覡��Ӫ�
CREATE TABLE [ShipmentMethodCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�t�e�覡id
[name] NVARCHAR(20) NOT NULL --�t�e�覡�W��(�v�t�B�W�Ө��f)
)

--�t�e���A��Ӫ�
CREATE TABLE [ShipmentStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�t�e���Aid
[name] NVARCHAR(20) --�t�e���A�W��(�ݥX�f�B�w�X�f�B�B�e���B�w�e�F)
)

--�q�檬�A���v��(�@���q�榳�h�Ӫ��A�ܧ󪺾��v����)
CREATE TABLE [OrderStatusHistory] (
[history_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�q�檬�A���vid
[order_id] INT NOT NULL, --�q��id
[order_status_id] INT NOT NULL, --�q�檬�A���id
[created_at] DATETIME DEFAULT GETDATE(), --���A�ܧ�ɶ�
FOREIGN KEY ([order_id]) REFERENCES [Order]([order_id]),
FOREIGN KEY ([order_status_id]) REFERENCES [OrderStatusCorrespond]([id])
)

--�q�檬�A��Ӫ�
CREATE TABLE [OrderStatusCorrespond] (
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�q�檬�Aid
[name] NVARCHAR(20) --�q�檬�A�W��(�ݥI�ڡB�ݥX�f�B�ݦ��f�B�h�f/�h�ڡB�q��w�����B������)
)






--�@�Ť�����
CREATE TABLE [Category1](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�@�Ť���id
[name] NVARCHAR(255) NOT NULL, --�@�Ť����W��
)

--�G�Ť�����
CREATE TABLE [Category2](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�G�Ť���id
[name] NVARCHAR(255) NOT NULL, --�G�Ť����W��
)

--�@��_�G�Ť������p��(����P�@�ӤG�Ť����]���J�ǡ^�i�H�ݩ�h�Ӥ@�Ť����]�k�ͦ�� / �k�ͦ�ۡ^)
CREATE TABLE [Category1_Category2](
[c1_id] INT NOT NULL, --�@�Ť���id
[c2_id] INT NOT NULL, --�G�Ť���id
PRIMARY KEY ([c1_id], [c2_id]), --(�ƦX�D��)
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]),
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id])
)

--�T�Ť�����
CREATE TABLE [Category3](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�T�Ť���id
[c2_id] INT, --�G�Ť���id
[name] NVARCHAR(255) NOT NULL, --�T�Ť����W��
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id])
)



--�ӫ~��
CREATE TABLE [Product](
[product_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�ӫ~id
[shop_id] INT NOT NULL, --���ݩ��Q
[c1_id] INT NOT NULL, --�ӫ~���@�Ť���
[c2_id] INT NOT NULL, --�ӫ~���G�Ť���
[c3_id] INT NULL,  --�ĤT�h����(�ܤֿ��Ť����A�ĤT�ťi���)(�i�H�ONULL!!)
[product_name] NVARCHAR(255) NOT NULL, --�ӫ~�W��
[description] NVARCHAR(MAX) NULL, --�ӫ~�y�z
[created_at] DATETIME DEFAULT GETDATE(), --�Ыخɶ�
[updated_at] DATETIME DEFAULT GETDATE(), --�̫��s�ɶ�
FOREIGN KEY ([shop_id]) REFERENCES [Shop]([shop_id]),
FOREIGN KEY ([c1_id]) REFERENCES [Category1]([id]),
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id]),
FOREIGN KEY ([c3_id]) REFERENCES [Category3]([id])
)

--�ݩʪ�
CREATE TABLE [Attribute](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�ݩ�id
[name] NVARCHAR(50) NOT NULL --�ݩʦW��
)

--(�G��)����_�ݩʪ�  (���ްӫ~�O�G�h�����٬O�T�h�����A���ھڤG�h�������ͥi����ݩ�)(�@�����: �Y�ӤG�Ť��� �P �Y���ݩ� ���������Y�A�ë��w���ݩʬO�_����)
CREATE TABLE [Category2_Attribute](
[c2_id] INT NOT NULL, --�ĤG�h����id
[attribute_id] INT NOT NULL, --�ݩ�id
[is_required] BIT DEFAULT 0, --�O�_����
PRIMARY KEY ([c2_id],[attribute_id]), --(�ƦX�D��)
FOREIGN KEY ([c2_id]) REFERENCES [Category2]([id]),
FOREIGN KEY ([attribute_id]) REFERENCES [Attribute]([id])
)

--�ݩʭȪ�
CREATE TABLE [AttributeValue](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�ݩʭ�id
[attribute_id] INT NOT NULL, --�ݩ�id
[name] NVARCHAR(50) NOT NULL, --�ݩʭȦW��
FOREIGN KEY ([attribute_id]) REFERENCES [Attribute]([id])
)

--�ӫ~_�ݩʭȪ� (�@�����: �Y�Ӱӫ~���Y���ݩʨ㦳�Y���ݩʭ�)
CREATE TABLE [ProductAttributeValue](
[product_id] INT NOT NULL, --�ӫ~id
[attribute_value_id] INT NOT NULL, --�ݩʭ�id
PRIMARY KEY([product_id],[attribute_value_id]), --(�ƦX�D��)
FOREIGN KEY ([product_id]) REFERENCES [Product]([product_id]),
FOREIGN KEY ([attribute_value_id]) REFERENCES [AttributeValue]([id])
)

--�W��� (�@�����: �@�ӳW��W��)
CREATE TABLE [Specification](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�W��id
[name] NVARCHAR(50) NOT NULL, --�W��W��
[is_default] BIT NOT NULL DEFAULT 0 --�O�_�����x�w�]�]1=�w�], 0=��a�ۭq�^
)

--�W��Ȫ� (�@�����: �Y�W�檺�Ҧ��i���)
CREATE TABLE [SpecificationValue](
[id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --�W���id
[specification_id] INT NOT NULL, --�W��id
[name] NVARCHAR(50) NOT NULL, --�W��ȦW��
FOREIGN KEY ([specification_id]) REFERENCES [Specification]([id])
)

--SKU�� (�ӫ~_�W��զX��)(�w�s����)(�@�����: �Y�Ӱӫ~���Y�ӳW��զX)
CREATE TABLE [SKU] (
[SKU_id] INT PRIMARY KEY IDENTITY(1,1) NOT NULL, --SKU id
[product_id] INT NOT NULL, --�ӫ~��id
[stock] INT NOT NULL DEFAULT 0, --�w�s
[price] DECIMAL(10,2) NOT NULL, --����
FOREIGN KEY ([product_id]) REFERENCES [Product]([product_id])
);

--SKU�Ӹ`��(SKU_�W��Ȫ�)
CREATE TABLE [SKU_SpecificationValue] (
[SKU_id] INT NOT NULL, --SKU id
[specification_value_id] INT NOT NULL, --�W���id
PRIMARY KEY ([SKU_id], [specification_value_id]), --(�ƦX�D��)
FOREIGN KEY ([SKU_id]) REFERENCES [SKU]([SKU_id]),
FOREIGN KEY ([specification_value_id]) REFERENCES [SpecificationValue]([id])
);