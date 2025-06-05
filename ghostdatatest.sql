BEGIN TRANSACTION;

-- Insert new doctors based on existing users (from UserID 23)
INSERT INTO [Doctor] ([UserID], [BioDescription])
VALUES
    (22, 'Cardiologist with 15 years of experience in treating complex heart conditions.'),
    (23, 'Neurologist specializing in stroke treatment and neuro-rehabilitation.');

-- Update some patients with details (existing patients 6-10)
UPDATE [Patient]
SET [dateOfBirth] = '1987-06-22', [gender] = 'Female', [description] = 'Patient with history of migraines'
WHERE [PatientID] = 6;

UPDATE [Patient]
SET [dateOfBirth] = '1992-03-15', [gender] = 'Male', [description] = 'Patient with type 2 diabetes'
WHERE [PatientID] = 7;

-- Add patient contact information
INSERT INTO [PatientContact] ([PatientID], [AddressType], [StreetAddress], [City], [State], [PostalCode], [Country])
VALUES
    (6, 'Home', '123 Elm Street', 'Chicago', 'IL', '60601', 'USA'),
    (7, 'Home', '456 Oak Avenue', 'Boston', 'MA', '02108', 'USA');

-- Link doctors to existing specializations
INSERT INTO [DoctorSpecialization] ([DoctorID], [SpecID])
VALUES
    (5, 1),  -- New doctor 5 with Cardiology specialty (SpecID 1)
    (6, 3);  -- New doctor 6 with Neurology specialty (SpecID 3)

-- Add doctor education records
INSERT INTO [DoctorEducation] ([DoctorID], [Degree], [Institution], [Graduation], [Description])
VALUES
    (5, 'MD', 'Johns Hopkins University', 2005, 'Specialized in Cardiology'),
    (6, 'MD', 'Harvard Medical School', 2008, 'Specialized in Neurology');

-- Link doctors to departments
INSERT INTO [DoctorDepartment] ([DoctorID], [DepartmentID], [IsPrimary], [JoinDate])
VALUES
    (5, 5, 1, '2021-05-10'), -- Doctor 5 to Cardiology department
    (6, 2, 1, '2020-11-18'); -- Doctor 6 to Radiology department

-- Add schedules for new doctors
INSERT INTO [Schedule] ([DoctorID], [RoomID], [ScheduleDate], [startTime], [endTime])
VALUES
    (5, 2, DATEADD(day, 1, GETDATE()), '09:00', '17:00'),
    (6, 3, DATEADD(day, 2, GETDATE()), '08:00', '16:00');

-- Add appointments
INSERT INTO [Appointment] ([DoctorID], [PatientID], [RoomID], [Description], [DateTime], [Status], [Email], [PhoneNumber])
VALUES
    (5, 6, 2, 'Regular cardiac checkup', DATEADD(day, 3, GETDATE()), 'Scheduled', 'patient@example.com', '555-1234'),
    (6, 7, 3, 'Neurological assessment', DATEADD(day, 4, GETDATE()), 'Scheduled', 'patient2@example.com', '555-5678'),
    (5, 7, 2, 'Follow-up after medication change', DATEADD(day, -1, GETDATE()), 'Completed', 'patient2@example.com', '555-5678');

-- Add doctor notes for completed appointment
INSERT INTO [DoctorNotes] ([AppointmentID], [DoctorID], [NoteContent])
VALUES
    (12, 5, 'Patient reports improved cardiac function after medication adjustment. Blood pressure is now within normal range. Recommended continuing current regimen with follow-up in 3 months.');

-- Add medical results
INSERT INTO [MedicalResult] ([AppointmentID], [DoctorID], [ResultDate], [Description], [FileURL], [Status])
VALUES
    (12, 5, DATEADD(day, -1, GETDATE()), 'ECG Results', '/files/ecg_20230615.pdf', 'Completed'),
    (12, 5, DATEADD(day, -1, GETDATE()), 'Blood Test Results', '/files/bloodwork_20230615.pdf', 'Completed');

-- Add medical orders
INSERT INTO [MedicalOrder] ([AppointmentID], [DoctorID], [OrderType], [Description], [Status], [AssignedToDeptID])
VALUES
    (11, 5, 'LAB', 'Comprehensive Metabolic Panel and Lipid Profile', 'Pending', 1),
    (11, 5, 'IMAGING', 'Echocardiogram', 'Pending', 2);

-- Add prescription
INSERT INTO [Prescription] ([AppointmentID], [PatientID], [DoctorID], [Notes])
VALUES
    (12, 7, 5, 'Take medication with food. Monitor for any side effects.');

-- Add MedicalReport (using the current identity)
INSERT INTO [MedicalReport] ([AppointmentID], [DoctorID], [Summary], [Status], [FileURL])
VALUES
    (12, 5, 'Patient shows significant improvement in cardiac function after medication adjustment. ECG shows normal sinus rhythm. Blood pressure has normalized.', 'COMPLETE', '/files/reports/cardiac_20230615.pdf');

-- Commit the transaction if everything is successful
COMMIT TRANSACTION;