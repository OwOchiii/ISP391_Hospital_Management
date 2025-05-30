CREATE TABLE [Users] (
  [UserID] int PRIMARY KEY IDENTITY(1,1),
  [FullName] varchar(255) NOT NULL,
  [Email] varchar(255) UNIQUE NULL,
  [PasswordHash] varchar(255) NULL,
  [PhoneNumber] varchar(20) UNIQUE NULL,
  [RoleID] int NOT NULL,
  [IsGuest] bit NOT NULL DEFAULT 0,
  [created_at] DATETIME2 NOT NULL DEFAULT GETDATE()
)
GO

CREATE TABLE [Role] (
  [RoleID]      int PRIMARY KEY IDENTITY(1,1),
  [RoleName]    varchar(255) NOT NULL,
  [Description] varchar(max) NULL,
  [Permission] varchar(max) NULL
)
GO

CREATE TABLE [Patient] (
  [PatientID] int PRIMARY KEY IDENTITY(1,1),
  [UserID] int NOT NULL UNIQUE,
  [dateOfBirth] date NULL,
  [gender] varchar(10) NULL CHECK (gender IN ('Male', 'Female', 'Other')),
  [address] varchar(255) NULL,
  [description] varchar(max) NULL
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

CREATE TABLE [Doctor] (
  [DoctorID] int PRIMARY KEY IDENTITY(1,1),
  [UserID] int NOT NULL UNIQUE,
  [BioDescription] varchar(max) NULL
)
GO

CREATE TABLE [DoctorSpecialization] (
  [DoctorID] int NOT NULL,
  [SpecID] int NOT NULL,
  PRIMARY KEY ([DoctorID], [SpecID])
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
  [DoctorID] int NOT NULL,
  [PatientID] int NOT NULL,
  [RoomID] int NULL,
  [Description] varchar(255) NULL,
  [DateTime] datetime NOT NULL,
  [Status] varchar(20) NOT NULL CHECK (Status IN ('Scheduled', 'Completed', 'Cancel')) DEFAULT 'Scheduled',
  [Email] varchar(255) NULL,
  [PhoneNumber] varchar (20) NULL
)
GO

CREATE TABLE [Transaction] (
  [TransactionID]     int PRIMARY KEY IDENTITY(1,1),
  [AppointmentID]     int          NULL,
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
  [DoctorID]         int          NOT NULL,
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

CREATE TABLE [PasswordResetToken] (
  [TokenID] int PRIMARY KEY IDENTITY(1,1),
  [UserID] int NOT NULL,
  [Token] varchar(255) NOT NULL,
  [ExpiryDate] datetime NOT NULL,
  [CreatedAt] datetime NOT NULL DEFAULT GETDATE(),
  [IsUsed] bit NOT NULL DEFAULT 0,
  CONSTRAINT UC_UserID UNIQUE ([UserID])
)
GO

-- Foreign key relationships

ALTER TABLE [Users] ADD FOREIGN KEY ([RoleID]) REFERENCES [Role] ([RoleID])
GO

ALTER TABLE [Patient] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Doctor] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [PatientContact] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [DoctorSpecialization] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [DoctorSpecialization] ADD FOREIGN KEY ([SpecID]) REFERENCES [Specialization] ([SpecID])
GO

ALTER TABLE [DoctorEducation] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [Department] ADD FOREIGN KEY ([HeadDoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [Room] ADD FOREIGN KEY ([DepartmentID]) REFERENCES [Department] ([DepartmentID])
GO

ALTER TABLE [Service] ADD FOREIGN KEY ([SpecID]) REFERENCES [Specialization] ([SpecID])
GO

ALTER TABLE [Schedule] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [Schedule] ADD FOREIGN KEY ([RoomID]) REFERENCES [Room] ([RoomID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Appointment] ADD FOREIGN KEY ([RoomID]) REFERENCES [Room] ([RoomID])
GO

ALTER TABLE [MedicalResult] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [MedicalResult] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([resultID]) REFERENCES [MedicalResult] ([ResultID])
GO

ALTER TABLE [MedicalOrder] ADD FOREIGN KEY ([AssignedToDeptID]) REFERENCES [Department] ([DepartmentID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([PatientID]) REFERENCES [Patient] ([PatientID])
GO

ALTER TABLE [Prescription] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

ALTER TABLE [Medicine] ADD FOREIGN KEY ([PrescriptionID]) REFERENCES [Prescription] ([PrescriptionID])
GO

ALTER TABLE [Medicine] ADD FOREIGN KEY ([InventoryID]) REFERENCES [MedicineInventory] ([InventoryID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Transaction] ADD FOREIGN KEY ([ProcessedByUserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Receipt] ADD FOREIGN KEY ([TransactionID]) REFERENCES [Transaction] ([TransactionID])
GO

ALTER TABLE [Receipt] ADD FOREIGN KEY ([IssuerID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [Notification] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [AuditLog] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

ALTER TABLE [PasswordResetToken] ADD FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
GO

CREATE INDEX [IX_PasswordResetToken_Token] ON [PasswordResetToken] ([Token])
GO

-- Extended properties
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

-- Add roles
INSERT INTO [Role] ([RoleName], [Description], [Permission])
VALUES
    ('ADMIN', 'System administrator with full access to all features', 'full_access'),
    ('DOCTOR', 'Medical professional who can manage appointments and patient records', 'patient_management,appointment_management,prescription_management'),
    ('RECEPTIONIST', 'Front desk staff who manage scheduling and basic patient information', 'appointment_scheduling,patient_registration'),
    ('PATIENT', 'Registered user who can book appointments and view their medical records', 'appointment_booking,view_own_records');
GO

-- Create improved trigger for user creation
CREATE TRIGGER trg_AddPatientOnUserCreation
    ON [Users]
    AFTER INSERT
    AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @RoleIDForPatient INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'PATIENT');

    -- Insert into Patient table for new users with PATIENT role
    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [address], [description])
    SELECT i.[UserID], NULL, NULL, NULL, NULL
    FROM inserted i
    WHERE i.[RoleID] = @RoleIDForPatient;
END;
GO

-- Create improved trigger for role changes
CREATE TRIGGER trg_ManageUserRoleChanges
    ON [Users]
    AFTER UPDATE
    AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(RoleID) -- Only execute if RoleID was updated
        BEGIN
            DECLARE @RoleIDForPatient INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'PATIENT');
            DECLARE @RoleIDForDoctor INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'DOCTOR');

            -- Process each updated row individually to avoid conflicts
            DECLARE @UserID INT, @NewRoleID INT, @OldRoleID INT
            DECLARE user_cursor CURSOR FOR
                SELECT i.[UserID], i.[RoleID], d.[RoleID]
                FROM inserted i
                         INNER JOIN deleted d ON i.[UserID] = d.[UserID]
                WHERE i.[RoleID] <> d.[RoleID]; -- Only process rows where role actually changed

            OPEN user_cursor
            FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID

            WHILE @@FETCH_STATUS = 0
                BEGIN
                    -- Check if changing TO doctor role
                    IF @NewRoleID = @RoleIDForDoctor
                        BEGIN
                            -- First, safely remove from Patient if exists
                            IF EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    -- Check if patient has any dependencies
                                    IF EXISTS (
                                        SELECT 1 FROM [Appointment] a
                                                          INNER JOIN [Patient] p ON a.[PatientID] = p.[PatientID]
                                        WHERE p.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [Prescription] pr
                                                          INNER JOIN [Patient] p ON pr.[PatientID] = p.[PatientID]
                                        WHERE p.[UserID] = @UserID
                                    )
                                        BEGIN
                                            RAISERROR('Cannot change role: User has existing patient records.', 16, 1);
                                            ROLLBACK TRANSACTION;
                                            CLOSE user_cursor;
                                            DEALLOCATE user_cursor;
                                            RETURN;
                                        END
                                    ELSE
                                        BEGIN
                                            DELETE FROM [Patient] WHERE [UserID] = @UserID;
                                        END
                                END

                            -- Then add to Doctor
                            IF NOT EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Doctor] ([UserID], [BioDescription])
                                    VALUES (@UserID, NULL);
                                END
                        END

                        -- Check if changing TO patient role
                    ELSE IF @NewRoleID = @RoleIDForPatient
                        BEGIN
                            -- First, safely remove from Doctor if exists
                            IF EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    -- Check if doctor has any dependencies
                                    IF EXISTS (
                                        SELECT 1 FROM [Appointment] a
                                                          INNER JOIN [Doctor] d ON a.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [MedicalResult] mr
                                                          INNER JOIN [Doctor] d ON mr.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [MedicalOrder] mo
                                                          INNER JOIN [Doctor] d ON mo.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [Schedule] s
                                                          INNER JOIN [Doctor] d ON s.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    )
                                        BEGIN
                                            RAISERROR('Cannot change role: User has existing doctor records.', 16, 1);
                                            ROLLBACK TRANSACTION;
                                            CLOSE user_cursor;
                                            DEALLOCATE user_cursor;
                                            RETURN;
                                        END
                                    ELSE
                                        BEGIN
                                            DELETE FROM [Doctor] WHERE [UserID] = @UserID;
                                        END
                                END

                            -- Then add to Patient
                            IF NOT EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [address], [description])
                                    VALUES (@UserID, NULL, NULL, NULL, NULL);
                                END
                        END

                        -- If changing to any other role (ADMIN, RECEPTIONIST, etc.)
                    ELSE
                        BEGIN
                            -- Check if in Patient table and can be safely removed
                            IF EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    IF EXISTS (
                                        SELECT 1 FROM [Appointment] a
                                                          INNER JOIN [Patient] p ON a.[PatientID] = p.[PatientID]
                                        WHERE p.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [Prescription] pr
                                                          INNER JOIN [Patient] p ON pr.[PatientID] = p.[PatientID]
                                        WHERE p.[UserID] = @UserID
                                    )
                                        BEGIN
                                            RAISERROR('Cannot change role: User has existing patient records.', 16, 1);
                                            ROLLBACK TRANSACTION;
                                            CLOSE user_cursor;
                                            DEALLOCATE user_cursor;
                                            RETURN;
                                        END
                                    ELSE
                                        BEGIN
                                            DELETE FROM [Patient] WHERE [UserID] = @UserID;
                                        END
                                END

                            -- Check if in Doctor table and can be safely removed
                            IF EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    IF EXISTS (
                                        SELECT 1 FROM [Appointment] a
                                                          INNER JOIN [Doctor] d ON a.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [MedicalResult] mr
                                                          INNER JOIN [Doctor] d ON mr.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [MedicalOrder] mo
                                                          INNER JOIN [Doctor] d ON mo.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    ) OR EXISTS (
                                        SELECT 1 FROM [Schedule] s
                                                          INNER JOIN [Doctor] d ON s.[DoctorID] = d.[DoctorID]
                                        WHERE d.[UserID] = @UserID
                                    )
                                        BEGIN
                                            RAISERROR('Cannot change role: User has existing doctor records.', 16, 1);
                                            ROLLBACK TRANSACTION;
                                            CLOSE user_cursor;
                                            DEALLOCATE user_cursor;
                                            RETURN;
                                        END
                                    ELSE
                                        BEGIN
                                            DELETE FROM [Doctor] WHERE [UserID] = @UserID;
                                        END
                                END
                        END

                    FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID
                END

            CLOSE user_cursor
            DEALLOCATE user_cursor
        END
END;
GO

-- Add email and phone number fields to Appointment table
ALTER TABLE [Appointment]
    ADD [Email] varchar(255) NULL,
        [PhoneNumber] varchar(20) NULL;
GO

-- Add comment explaining the purpose of these fields
EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'Optional contact email for appointment notifications',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Appointment',
     @level2type = N'Column', @level2name = 'Email';
GO

EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'Optional contact phone number for appointment notifications',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Appointment',
     @level2type = N'Column', @level2name = 'PhoneNumber';
GO

ALTER TABLE [Appointment] DROP CONSTRAINT [CK__Appointme__Statu__4F7CD00D];

-- Step 2: Add the updated CHECK constraint (Check your own constraint name and replace it if necessary)
ALTER TABLE [Appointment] ADD CONSTRAINT [CK__Appointme__Statu__4F7CD00D]
    CHECK (Status IN ('Scheduled', 'Completed', 'Cancel', 'Pending'));

-- Add DoctorNotes table to store notes for appointments
CREATE TABLE [DoctorNotes] (
                               [NoteID] int PRIMARY KEY IDENTITY(1,1),
                               [AppointmentID] int NOT NULL,
                               [DoctorID] int NOT NULL,
                               [NoteContent] varchar(max) NOT NULL,
                               [CreatedAt] datetime NOT NULL DEFAULT GETDATE(),
                               [UpdatedAt] datetime NULL
)
GO

-- Add foreign key constraints
ALTER TABLE [DoctorNotes] ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID])
GO

ALTER TABLE [DoctorNotes] ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID])
GO

-- Add index for faster queries
CREATE INDEX [IX_DoctorNotes_AppointmentID] ON [DoctorNotes] ([AppointmentID])
GO

-- Add description
EXEC sp_addextendedproperty
     @name = N'Table_Description',
     @value = 'Stores doctor notes for patient appointments',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'DoctorNotes'
GO

-- Sample data (optional, for testing)
-- INSERT INTO [DoctorNotes] ([AppointmentID], [DoctorID], [NoteContent])
-- VALUES (1, 1, 'Patient reported mild improvements after starting new medication. Recommended continuing current treatment plan with follow-up in 2 weeks.');


-- Updated trigger that allows role changes but keeps associated records
CREATE OR ALTER TRIGGER trg_ManageUserRoleChanges
    ON [Users]
    AFTER UPDATE
    AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(RoleID) -- Only execute if RoleID was updated
        BEGIN
            DECLARE @RoleIDForPatient INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'PATIENT');
            DECLARE @RoleIDForDoctor INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'DOCTOR');

            -- Process each updated row individually
            DECLARE @UserID INT, @NewRoleID INT, @OldRoleID INT
            DECLARE user_cursor CURSOR FOR
                SELECT i.[UserID], i.[RoleID], d.[RoleID]
                FROM inserted i
                         INNER JOIN deleted d ON i.[UserID] = d.[UserID]
                WHERE i.[RoleID] <> d.[RoleID]; -- Only process rows where role actually changed

            OPEN user_cursor
            FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID

            WHILE @@FETCH_STATUS = 0
                BEGIN
                    -- Check if changing TO doctor role
                    IF @NewRoleID = @RoleIDForDoctor AND @OldRoleID = @RoleIDForPatient
                        BEGIN
                            -- Keep the Patient record but also add Doctor record
                            IF NOT EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Doctor] ([UserID], [BioDescription])
                                    VALUES (@UserID, NULL);
                                END
                        END
                        -- Check if changing TO patient role
                    ELSE IF @NewRoleID = @RoleIDForPatient AND @OldRoleID = @RoleIDForDoctor
                        BEGIN
                            -- Keep the Doctor record but also add Patient record
                            IF NOT EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [address], [description])
                                    VALUES (@UserID, NULL, NULL, NULL, NULL);
                                END
                        END
                        -- If changing to any other role, just keep existing records
                    ELSE
                        BEGIN
                            -- Make sure appropriate role-specific record exists
                            IF @NewRoleID = @RoleIDForPatient AND NOT EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [address], [description])
                                    VALUES (@UserID, NULL, NULL, NULL, NULL);
                                END
                            ELSE IF @NewRoleID = @RoleIDForDoctor AND NOT EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Doctor] ([UserID], [BioDescription])
                                    VALUES (@UserID, NULL);
                                END
                        END

                    FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID
                END

            CLOSE user_cursor
            DEALLOCATE user_cursor
        END
END;