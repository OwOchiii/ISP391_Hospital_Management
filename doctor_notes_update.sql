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
