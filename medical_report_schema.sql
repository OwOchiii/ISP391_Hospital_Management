-- Add MedicalReport table to store compilations of test results
CREATE TABLE [MedicalReport] (
  [ReportID] int PRIMARY KEY IDENTITY(1,1),
  [AppointmentID] int NOT NULL,
  [DoctorID] int NOT NULL,
  [ReportDate] datetime NOT NULL DEFAULT GETDATE(),
  [Summary] varchar(max) NULL,
  [Status] varchar(50) NOT NULL DEFAULT 'DRAFT',
  [FileURL] varchar(255) NULL
);

-- Create relationship between MedicalReport and Appointment
ALTER TABLE [MedicalReport]
ADD FOREIGN KEY ([AppointmentID]) REFERENCES [Appointment] ([AppointmentID]);

-- Create relationship between MedicalReport and Doctor
ALTER TABLE [MedicalReport]
ADD FOREIGN KEY ([DoctorID]) REFERENCES [Doctor] ([DoctorID]);

-- Create linking table to associate multiple results with a report
CREATE TABLE [ReportResults] (
  [ReportID] int NOT NULL,
  [ResultID] int NOT NULL,
  PRIMARY KEY ([ReportID], [ResultID])
);

-- Create foreign key relationships
ALTER TABLE [ReportResults]
ADD FOREIGN KEY ([ReportID]) REFERENCES [MedicalReport] ([ReportID]);

ALTER TABLE [ReportResults]
ADD FOREIGN KEY ([ResultID]) REFERENCES [MedicalResult] ([ResultID]);
