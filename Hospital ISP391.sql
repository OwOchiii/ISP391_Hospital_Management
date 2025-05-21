CREATE TABLE [Users] (
  [UserID] int PRIMARY KEY IDENTITY(1,1),
  [FullName] varchar(255) NOT NULL,
  [Email] varchar(255) UNIQUE NULL,
  [PasswordHash] varchar(255) NULL,
  [PhoneNumber] varchar(20) UNIQUE NULL,
  [RoleID] int NOT NULL,
  [IsGuest] bit NOT NULL DEFAULT 0
)
GO

CREATE TABLE [Role] (
  [RoleID]      int PRIMARY KEY IDENTITY(1,1),
  [RoleName]    varchar(255) NOT NULL,
  [Description] varchar(max) NULL,
  [Permission] varchar(max) NULL
)
GO

CREATE TABLE [PatientContact] (
  [ContactID] int PRIMARY KEY IDENTITY(1,1),
  [PatientID] int NOT NULL,
  [AddressType] varchar(50) NOT NULL,
  [StreetAddress] varchar(255) NOT NULL,
  [City] varchar(100) NOT NULL,
  [State] varchar(100) NULL,
  [PostalCode] varchar(20) NULL,
  [Country] varchar(100) NOT NULL
)
GO

CREATE TABLE [Patient] (
  [PatientID] int PRIMARY KEY,
  [dateOfBirth] date NULL,
  [gender] varchar(10) NULL CHECK (gender IN ('Male', 'Female', 'Other')),
  [address] varchar(255) NULL,
  [description] varchar(max) NULL
)
GO

CREATE TABLE [Doctor] (
  [UserID] int PRIMARY KEY,
  [BioDescription] varchar(max) NULL
)
GO

CREATE TABLE [DoctorSpecialization] (
  [UserID] int NOT NULL,
  [SpecID] int NOT NULL,
  PRIMARY KEY ([UserID], [SpecID])
)
GO

CREATE TABLE [DoctorEducation] (
  [EducationID] INT PRIMARY KEY IDENTITY(1,1),
  [DoctorID] INT NOT NULL,
  [Degree] VARCHAR(255) NOT NULL,
  [Institution] VARCHAR(255) NOT NULL,
  [Graduation] int NULL,
  [Description] varchar(max) NULL
)
GO

CREATE TABLE [Specialization] (
  [SpecID] int PRIMARY KEY IDENTITY(1,1),
  [SpecName] varchar(255) NOT NULL,
  [Symptom] varchar(255) NULL,
  [Price] decimal(10,2) NULL
)
GO

CREATE TABLE [Service] (
  [ServiceID] int PRIMARY KEY IDENTITY(1,1),
  [SpecID] int NOT NULL,
  [ServiceName] varchar(255) NOT NULL,
  [Price] decimal(10,2) NOT NULL
)
GO

CREATE TABLE [Appointment] (
  [AppointmentID] int PRIMARY KEY IDENTITY(1,1),
  [WaitID] int NULL,
  [DoctorID] int NOT NULL,
  [PatientID] int NOT NULL,
  [RoomID] int NULL,
  [Description] varchar(255) NULL,
  [DateTime] datetime NOT NULL,
  [Status] varchar(20) NOT NULL CHECK (Status IN ('Scheduled', 'Completed', 'Cancel')) DEFAULT 'Scheduled'
)
GO

CREATE TABLE [WaitingList] (
  [WaitID] int PRIMARY KEY IDENTITY(1,1),
  [UserID] int NOT NULL,
  [Status] varchar(20) NOT NULL CHECK (Status IN ('Waiting', 'Called', 'Missed', 'Cancelled')) DEFAULT 'Waiting',
  [Comment] varchar(255) NULL,
  [CalledAt] datetime NULL,
  [CreateAt] datetime NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE [Transaction] (
  [TransactionID]     int PRIMARY KEY IDENTITY(1,1),
  [AppointmentID]     int          NULL,
  [WaitID]            int          NULL,
  [UserID]            int          NOT NULL,
  [Method]            varchar(20)  NOT NULL CHECK (Method IN ('Cash', 'Banking')),
  [TimeOfPayment]     datetime     NOT NULL,
  [Status]            varchar(20)  NOT NULL CHECK (Status IN ('Paid', 'Refunded', 'Pending')) DEFAULT 'Pending',
  [RefundReason]      varchar(max) NULL,
  [ProcessedByUserID] int          NULL
)
GO

CREATE TABLE [Schedule] (
  [ScheduleID] int PRIMARY KEY IDENTITY(1,1),
  [DoctorID] int NOT NULL,
  [RoomID] int NOT NULL,
  [ScheduleDate] date NOT NULL,
  [startTime] time NOT NULL,
  [endTime] time NOT NULL
)
GO

CREATE TABLE [Notification] (
  [NotificationID] int PRIMARY KEY IDENTITY(1,1),
  [UserID]         int          NOT NULL,
  [Message]        varchar(max) NOT NULL,
  [Type]           varchar(50)  NOT NULL,
  [IsRead]         bit          NOT NULL DEFAULT 0,
  [CreatedAt]      datetime     NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE [MedicalResult] (
  [ResultID]      int PRIMARY KEY IDENTITY(1,1),
  [AppointmentID] int          NOT NULL,
  [DoctorID]      int          NOT NULL,
  [ResultDate]    datetime     NOT NULL,
  [Description]   varchar(max) NULL,
  [FileURL]       varchar(255) NULL,
  [Status]        varchar(50)  NOT NULL
)
GO

CREATE TABLE [MedicalOrder] (
  [OrderID]          int PRIMARY KEY IDENTITY(1,1),
  [AppointmentID]    int          NOT NULL,
  [OrderByID]        int          NOT NULL,
  [OrderType]        varchar(50)  NOT NULL,
  [Description]      varchar(max) NULL,
  [Status]           varchar(50)  NOT NULL DEFAULT 'Pending',
  [AssignedToDeptID] int          NULL,
  [OrderDate]        date         NOT NULL DEFAULT GETDATE(),
  [resultID]         int          NULL
)
GO

CREATE TABLE [Receipt] (
  [ReceiptID]      int PRIMARY KEY IDENTITY(1,1),
  [TransactionID]  int           NOT NULL,
  [ReceiptNumber]  varchar(50)   NOT NULL UNIQUE,
  [IssuedDate]     datetime      NOT NULL DEFAULT GETDATE(),
  [Format]         varchar(10)   NOT NULL CHECK (Format IN ('Digital', 'Print', 'Both')) DEFAULT 'Digital',
  [TaxAmount]      decimal(10,2) NOT NULL DEFAULT 0,
  [DiscountAmount] decimal(10,2) NOT NULL DEFAULT 0,
  [TotalAmount]    decimal(10,2) NOT NULL,
  [Notes]          varchar(max)  NULL,
  [IssuerID]       int           NOT NULL,
  [PDFPath]        varchar(255)  NULL
)
GO

CREATE TABLE [Department] (
  [DepartmentID] int PRIMARY KEY IDENTITY(1,1),
  [DeptName]     varchar(255) NOT NULL,
  [Description]  varchar(max) NULL,
  [HeadDoctorID] int          NULL
)
GO

CREATE TABLE [Room] (
  [RoomID] int PRIMARY KEY IDENTITY(1,1),
  [RoomNumber] varchar(20) NOT NULL UNIQUE,
  [RoomName] varchar(100) NOT NULL,
  [Type] varchar(20) NOT NULL CHECK (Type IN ('Examination', 'Operation', 'Consultation', 'Emergency', 'Ward', 'Laboratory')),
  [Capacity] int NOT NULL DEFAULT 1,
  [Status] varchar(20) NOT NULL CHECK (Status IN ('Available', 'Occupied', 'Maintenance', 'Reserved')) DEFAULT 'Available',
  [DepartmentID] int NOT NULL,
  [Notes] varchar(max) NULL
)
GO

CREATE TABLE [Prescription] (
  [PrescriptionID] INT PRIMARY KEY IDENTITY(1,1),
  [AppointmentID] INT NOT NULL,
  [PatientID] INT NOT NULL,
  [DoctorID] INT NOT NULL,
  [PrescriptionDate] DATETIME NOT NULL DEFAULT GETDATE(),
  [Notes] varchar(max) NULL
)
GO

CREATE TABLE [Medicine] (
  [MedicationID] INT PRIMARY KEY IDENTITY(1,1),
  [PrescriptionID] INT NOT NULL,
  [InventoryID] int NOT NULL,
  [Dosage] VARCHAR(100) NOT NULL,
  [Frequency] VARCHAR(100) NOT NULL,
  [Duration] VARCHAR(100) NOT NULL,
  [Instructions] varchar(max) NULL
)
GO

CREATE TABLE [AuditLog] (
  [LogID]     int PRIMARY KEY IDENTITY(1,1),
  [UserID]    int          NOT NULL,
  [TableName] varchar(100) NOT NULL,
  [RecordID]  int          NOT NULL,
  [Action]    varchar(50)  NOT NULL,
  [OldValue]  varchar(max) NULL,
  [NewValue]  varchar(max) NULL,
  [Timestamp] datetime     NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE [MedicineInventory] (
  [InventoryID] int PRIMARY KEY IDENTITY(1,1),
  [MedicineName] varchar(255) UNIQUE NOT NULL,
  [GenericName] varchar(255) NULL,
  [Category] varchar(100) NULL,
  [UnitOfMeasure] varchar(50) NOT NULL,
  [CurrentStock] int NOT NULL DEFAULT 0,
  [ReorderLevel] int NOT NULL DEFAULT 10,
  [ExpiryDate] date NULL,
  [Cost] decimal(10,2) NOT NULL,
  [Supplier] varchar(255) NULL
)
GO

-- Foreign key relationships (with corrections)

ALTER TABLE [Users] ADD FOREIGN KEY ([RoleID]) REFERENCES [Role] ([RoleID])
GO

ALTER TABLE [PatientContact] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Room] ADD FOREIGN KEY ([DepartmentID]) REFERENCES [Department] ([DepartmentID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [Medicine] ADD FOREIGN KEY ([PrescriptionID]) REFERENCES [Prescription] ([PrescriptionID])
GO

ALTER TABLE [Medicine] ADD FOREIGN KEY ([InventoryID]) REFERENCES [MedicineInventory] ([InventoryID])
GO

ALTER TABLE [AuditLog] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Patient] ADD FOREIGN KEY ([PatientID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Doctor] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Department] ADD FOREIGN KEY ([HeadDoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [DoctorEducation] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [DoctorSpecialization] ADD FOREIGN KEY ([SpecID]) REFERENCES [Specialization] ([SpecID])
GO

ALTER TABLE [DoctorSpecialization] ADD FOREIGN KEY ([UserID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([WaitID]) REFERENCES [WaitingList] ([WaitID])
GO

ALTER TABLE [Service] ADD FOREIGN KEY ([SpecID]) REFERENCES [Specialization] ([SpecID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([WaitID]) REFERENCES [WaitingList] ([WaitID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([ProcessedByUserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Schedule] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [Notification] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [MedicalResult] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [MedicalResult] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([OrderByID]) REFERENCES [Doctor] ([UserID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([resultID]) REFERENCES [MedicalResult] ([ResultID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([AssignedToDeptID]) REFERENCES [Department] ([DepartmentID])
GO

ALTER TABLE [Receipt] ADD FOREIGN KEY ([IssuerID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Receipt] ADD FOREIGN KEY ([TransactionID]) REFERENCES [Transaction] ([TransactionID])
GO

ALTER TABLE [WaitingList] ADD FOREIGN KEY ([UserID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Schedule] ADD FOREIGN KEY ([RoomID]) REFERENCES [Room] ([RoomID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([RoomID]) REFERENCES [Room] ([RoomID])
GO

-- Keep your extended properties
EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'Nullable if on site registration',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Users',
     @level2type = N'Column', @level2name = 'Email';
GO

EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'Nullable if on site registration',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Users',
     @level2type = N'Column', @level2name = 'PasswordHash';
GO
-- Add other sp_addextendedproperty statements as in original script