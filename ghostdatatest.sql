BEGIN TRANSACTION;

-- First, insert the doctor records for existing users with doctor role (UserIDs 13-16)
INSERT INTO [Doctor] ([UserID], [BioDescription])
VALUES
    (13, 'Cardiologist with 10 years of experience specializing in heart disease and cardiac rehabilitation'),
    (14, 'Neurologist focused on headache disorders, epilepsy, and neurodegenerative diseases'),
    (15, 'Pediatrician with special interest in childhood development and preventive care'),
    (16, 'Orthopedic specialist with expertise in sports medicine and joint replacement surgery');

-- Now link doctors to specializations (using the newly created DoctorIDs)
INSERT INTO [DoctorSpecialization] ([DoctorID], [SpecID])
VALUES
    (17, 1),  -- Dr. Sarah Johnson - Cardiology
    (18, 3),  -- Dr. Robert Chen - Neurology
    (19, 5),  -- Dr. Emily Rodriguez - Pediatrics
    (20, 4);  -- Dr. Michael Barnes - Orthopedics

-- Add doctor education records
INSERT INTO [DoctorEducation] ([DoctorID], [Degree], [Institution], [Graduation], [Description])
VALUES
    (17, 'MD', 'Harvard Medical School', 2008, 'Graduated with honors'),
    (18, 'MD', 'Johns Hopkins University', 2010, 'Specialized in neuroscience'),
    (19, 'MD', 'Stanford Medical School', 2009, 'Focus on pediatric medicine'),
    (20, 'MD', 'Yale School of Medicine', 2007, 'Research in orthopedic surgery');

-- Associate doctors with departments
INSERT INTO [DoctorDepartment] ([DoctorID], [DepartmentID], [IsPrimary], [JoinDate])
VALUES
    (17, 5, 1, '2018-06-15'), -- Dr. Sarah Johnson - Cardiology (primary)
    (18, 2, 1, '2017-04-10'), -- Dr. Robert Chen - Radiology (primary)
    (19, 1, 1, '2019-08-20'), -- Dr. Emily Rodriguez - Laboratory (primary)
    (20, 4, 1, '2018-11-05'); -- Dr. Michael Barnes - Physical Therapy (primary)

-- Update existing patients
UPDATE [Patient]
SET [dateOfBirth] = '1985-04-12', [gender] = 'Female', [description] = 'History of hypertension'
WHERE [PatientID] = 8;

UPDATE [Patient]
SET [dateOfBirth] = '1978-09-27', [gender] = 'Male', [description] = 'Type 2 diabetes'
WHERE [PatientID] = 9;

UPDATE [Patient]
SET [dateOfBirth] = '1992-11-05', [gender] = 'Female', [description] = 'Chronic migraines'
WHERE [PatientID] = 10;

UPDATE [Patient]
SET [dateOfBirth] = '1965-03-18', [gender] = 'Male', [description] = 'Arthritis'
WHERE [PatientID] = 2;

UPDATE [Patient]
SET [dateOfBirth] = '2000-07-30', [gender] = 'Female', [description] = 'Mild asthma'
WHERE [PatientID] = 3;

-- Add patient contact information
INSERT INTO [PatientContact] ([PatientID], [AddressType], [StreetAddress], [City], [State], [PostalCode], [Country])
VALUES
    (6, 'Home', '123 Main Street', 'New York', 'NY', '10001', 'USA'),
    (7, 'Home', '456 Oak Avenue', 'Chicago', 'IL', '60601', 'USA'),
    (8, 'Home', '789 Pine Boulevard', 'San Francisco', 'CA', '94102', 'USA'),
    (9, 'Home', '101 Maple Drive', 'Boston', 'MA', '02108', 'USA'),
    (10, 'Home', '202 Elm Street', 'Seattle', 'WA', '98101', 'USA');

INSERT INTO [Room] ([RoomNumber], [RoomName], [Type], [Capacity], [Status], [DepartmentID], [Notes])
VALUES
    ('101', 'Exam Room 1', 'Examination', 3, 'Available', 5, 'Standard examination room'),
    ('102', 'Exam Room 2', 'Examination', 3, 'Available', 5, 'Standard examination room'),
    ('201', 'X-Ray Room', 'Laboratory', 2, 'Available', 2, 'X-ray imaging equipment'),
    ('202', 'MRI Room', 'Laboratory', 2, 'Available', 2, 'Magnetic resonance imaging'),
    ('301', 'Physical Therapy Room', 'Consultation', 5, 'Available', 4, 'Exercise equipment'),
    ('401', 'Emergency Room 1', 'Emergency', 4, 'Available', 5, 'Cardiac emergency equipment'),
    ('501', 'Lab Room 1', 'Laboratory', 3, 'Available', 1, 'Blood testing equipment');
GO

-- Add schedules for doctors
INSERT INTO [Schedule] ([DoctorID], [RoomID], [ScheduleDate], [startTime], [endTime])
VALUES
    (17, 14, DATEADD(day, 1, GETDATE()), '09:00', '17:00'),
    (18, 14, DATEADD(day, 1, GETDATE()), '08:00', '16:00'),
    (19, 15, DATEADD(day, 2, GETDATE()), '10:00', '18:00'),
    (20, 15, DATEADD(day, 2, GETDATE()), '09:00', '17:00');



-- Add appointments
INSERT INTO [Appointment] ([DoctorID], [PatientID], [RoomID], [Description], [DateTime], [Status], [Email], [PhoneNumber])
VALUES
    -- Upcoming appointments
    (17, 6, 14, 'Annual cardiac checkup', DATEADD(day, 7, GETDATE()), 'Scheduled', 'alice.smith@example.com', '555-0106'),
    (18, 7, 15, 'Headache evaluation', DATEADD(day, 8, GETDATE()), 'Scheduled', 'bob.johnson@example.com', '555-0107'),
    (19, 8, 16, 'Annual checkup', DATEADD(day, 9, GETDATE()), 'Scheduled', 'carol.williams@example.com', '555-0108'),
    (20, 9, 17, 'Joint pain follow-up', DATEADD(day, 10, GETDATE()), 'Scheduled', 'david.brown@example.com', '555-0109'),

    -- Past appointments
    (17, 6, 14, 'Initial cardiac assessment', DATEADD(day, -30, GETDATE()), 'Completed', 'alice.smith@example.com', '555-0106'),
    (18, 7, 15, 'MRI for recurring headaches', DATEADD(day, -45, GETDATE()), 'Completed', 'bob.johnson@example.com', '555-0107');

-- Add doctor notes for completed appointments
INSERT INTO [DoctorNotes] ([AppointmentID], [DoctorID], [NoteContent])
VALUES
    (21, 17, 'Patient presents with stable blood pressure at 138/88. ECG shows normal sinus rhythm. Mild concern about cholesterol levels, recommended diet modification and follow-up lipid panel in 3 months.'),
    (22, 18, 'Patient reports worsening headaches, 3-4 times per week. No aura or neurological deficits. MRI ordered to rule out structural abnormalities. Prescribed sumatriptan for acute attacks.');

-- Add medical results
INSERT INTO [MedicalResult] ([AppointmentID], [DoctorID], [ResultDate], [Description], [FileURL], [Status])
VALUES
    (21, 1, DATEADD(day, -29, GETDATE()), 'ECG Results', '/files/ecg_results.pdf', 'Completed'),
    (21, 1, DATEADD(day, -29, GETDATE()), 'Blood Pressure Readings', '/files/bp_readings.pdf', 'Completed'),
    (22, 2, DATEADD(day, -44, GETDATE()), 'MRI Brain Scan', '/files/mri_brain.pdf', 'Completed');

-- Add medical orders
INSERT INTO [MedicalOrder] ([AppointmentID], [DoctorID], [OrderType], [Description], [Status], [AssignedToDeptID])
VALUES
    -- Orders for upcoming appointments
    (17, 17, 'LAB', 'Comprehensive Metabolic Panel and Lipid Profile', 'Pending', 1),
    (18, 18, 'IMAGING', 'MRI Brain with contrast', 'Pending', 2),

    -- Orders from past appointments (completed)
    (21, 17, 'LAB', 'Lipid Panel', 'Completed', 1),
    (22, 18, 'IMAGING', 'MRI Brain without contrast', 'Completed', 2);

-- Add medical reports
INSERT INTO [MedicalReport] ([AppointmentID], [DoctorID], [Summary], [Status], [FileURL])
VALUES
    (21, 17, 'Patient has mild hypertension with BP reading of 138/88. ECG shows normal sinus rhythm. Lipid panel indicates borderline high cholesterol. Recommended lifestyle modifications including DASH diet and regular exercise.', 'COMPLETE', '/files/reports/cardiac_report.pdf'),
    (22, 18, 'Patient diagnosed with chronic migraine without aura. MRI brain was normal with no evidence of structural abnormality. Prescribed sumatriptan for acute attacks and recommended headache diary.', 'COMPLETE', '/files/reports/neuro_report.pdf');

-- Add prescriptions
INSERT INTO [Prescription] ([AppointmentID], [PatientID], [DoctorID], [Notes])
VALUES
    (22, 7, 18, 'Take at first sign of migraine. May repeat after 2 hours if needed. Maximum 9 tablets per month.');

-- Add medicine inventory
INSERT INTO [MedicineInventory] ([MedicineName], [GenericName], [Category], [UnitOfMeasure], [CurrentStock], [ReorderLevel], [ExpiryDate], [Cost], [Supplier])
VALUES
    ('Sumatriptan 50mg', 'Sumatriptan', 'Antimigraine', 'Tablet', 200, 50, DATEADD(year, 2, GETDATE()), 3.75, 'MediSource'),
    ('Atorvastatin 20mg', 'Atorvastatin', 'Statin', 'Tablet', 300, 75, DATEADD(year, 2, GETDATE()), 0.85, 'PharmaCorp'),
    ('Lisinopril 10mg', 'Lisinopril', 'Antihypertensive', 'Tablet', 250, 60, DATEADD(year, 2, GETDATE()), 0.65, 'PharmaCorp'),
    ('Albuterol Inhaler', 'Albuterol', 'Bronchodilator', 'Inhaler', 50, 15, DATEADD(year, 1, GETDATE()), 25.00, 'RespiCare'),
    ('Ibuprofen 200mg', 'Ibuprofen', 'NSAID', 'Tablet', 500, 100, DATEADD(year, 3, GETDATE()), 0.15, 'MediSource');

-- Add medicines to prescriptions
INSERT INTO [Medicine] ([PrescriptionID], [InventoryID], [Dosage], [Frequency], [Duration], [Instructions])
VALUES
    (10, 16, '50mg', 'As needed', '30 days', 'Take at first sign of migraine with full glass of water. May repeat after 2 hours if needed.');

-- Add audit logs


rollback