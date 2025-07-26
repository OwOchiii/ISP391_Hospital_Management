create table [ReceiptService]
(id int primary key IDENTITY(1,1),
 ReceiptID int,
 ServiceID int)

ALTER TABLE [ReceiptService] ADD FOREIGN KEY ([ReceiptID]) REFERENCES [Receipt] ([ReceiptID])
GO

ALTER TABLE [ReceiptService] ADD FOREIGN KEY ([ServiceID]) REFERENCES [Service] ([ServiceID])
GO
