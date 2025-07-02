-- Create MedicalRecord table to store patient medical records
CREATE TABLE [MedicalRecord] (
    [RecordID] int PRIMARY KEY IDENTITY(1,1),
    [PatientID] int NOT NULL,
    [DoctorID] int NOT NULL,
    [RecordDate] datetime NOT NULL DEFAULT GETDATE(),
    [LastUpdated] datetime NOT NULL DEFAULT GETDATE(),
    [ConditionOverview] varchar(max) NULL,
    [MedicalConditions] varchar(max) NULL,
    [MedicalHistory] varchar(max) NULL,
    [Allergies] varchar(max) NULL,
    [CurrentMedications] varchar(max) NULL,
    [BloodType] varchar(10) NULL,
    [HealthStatus] varchar(50) NULL,
    CONSTRAINT [FK_MedicalRecord_Patient] FOREIGN KEY ([PatientID]) REFERENCES [Patient]([PatientID]),
    CONSTRAINT [FK_MedicalRecord_Doctor] FOREIGN KEY ([DoctorID]) REFERENCES [Doctor]([DoctorID])
)
GO

-- Create index for faster queries
CREATE INDEX [IX_MedicalRecord_PatientID] ON [MedicalRecord] ([PatientID])
GO

CREATE INDEX [IX_MedicalRecord_DoctorID] ON [MedicalRecord] ([DoctorID])
GO

-- Create Medication table to store medications for each medical record
CREATE TABLE [RecordMedication] (
    [MedicationID] int PRIMARY KEY IDENTITY(1,1),
    [RecordID] int NOT NULL,
    [Name] varchar(255) NOT NULL,
    [Dosage] varchar(100) NOT NULL,
    [Frequency] varchar(100) NOT NULL,
    [StartDate] date NOT NULL,
    [EndDate] date NULL,
    CONSTRAINT [FK_RecordMedication_MedicalRecord] FOREIGN KEY ([RecordID]) REFERENCES [MedicalRecord]([RecordID])
)
GO

-- Create index for faster queries
CREATE INDEX [IX_RecordMedication_RecordID] ON [RecordMedication] ([RecordID])
GO

-- Add table descriptions
EXEC sp_addextendedproperty
     @name = N'Table_Description',
     @value = 'Stores comprehensive medical records for patients',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'MedicalRecord'
GO

EXEC sp_addextendedproperty
     @name = N'Table_Description',
     @value = 'Stores medications associated with patient medical records',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'RecordMedication'
GO

-- Add trigger to update LastUpdated timestamp when record is modified
CREATE TRIGGER [trg_UpdateMedicalRecordTimestamp]
ON [MedicalRecord]
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE [MedicalRecord]
    SET [LastUpdated] = GETDATE()
    FROM [MedicalRecord] m
    INNER JOIN inserted i ON m.[RecordID] = i.[RecordID];
END
GO
