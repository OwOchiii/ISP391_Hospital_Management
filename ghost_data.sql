-- Disable foreign key constraints temporarily
EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT all"

-- Delete data from all tables (in dependency order)
DELETE FROM [ReportResults]
DELETE FROM [MedicalReport]
DELETE FROM [DoctorNotes]
DELETE FROM [RecordMedication]
DELETE FROM [MedicalRecord]
DELETE FROM [Medicine]
DELETE FROM [Prescription]
DELETE FROM [Receipt]
DELETE FROM [Transaction]
DELETE FROM [MedicalOrder]
DELETE FROM [MedicalResult]
DELETE FROM [Notification]
DELETE FROM [AuditLog]
DELETE FROM [Appointment]
DELETE FROM [Schedule]
DELETE FROM [DoctorEducation]
DELETE FROM [DoctorSpecialization]
DELETE FROM [Service]
DELETE FROM [PatientContact]
DELETE FROM [Receptionist]
DELETE FROM [Doctor]
DELETE FROM [Patient]
DELETE FROM [PasswordResetToken]
DELETE FROM [Feedback]
DELETE FROM [doctor_support_tickets]
DELETE FROM [Users]
DELETE FROM [Room]
DELETE FROM [Department]
DELETE FROM [Specialization]
DELETE FROM [MedicineInventory]
-- Keep Role table data as it contains system roles

-- Re-enable foreign key constraints
EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all"

-- Reset identity columns
DBCC CHECKIDENT ('[Users]', RESEED, 0)
DBCC CHECKIDENT ('[Patient]', RESEED, 0)
DBCC CHECKIDENT ('[Doctor]', RESEED, 0)
DBCC CHECKIDENT ('[Appointment]', RESEED, 0)
DBCC CHECKIDENT ('[Transaction]', RESEED, 0)
DBCC CHECKIDENT ('[Schedule]', RESEED, 0)
DBCC CHECKIDENT ('[Specialization]', RESEED, 0)
DBCC CHECKIDENT ('[Department]', RESEED, 0)
DBCC CHECKIDENT ('[Room]', RESEED, 0)
DBCC CHECKIDENT ('[MedicineInventory]', RESEED, 0)

-- Insert Specializations
INSERT INTO [Specialization] ([SpecName], [Symptom], [Price]) VALUES
('Cardiology', 'Heart problems, chest pain, irregular heartbeat', 150.00),
('Dermatology', 'Skin conditions, rashes, acne', 100.00),
('Neurology', 'Headaches, seizures, memory issues', 180.00),
('Pediatrics', 'Child health issues, growth concerns', 120.00),
('Orthopedics', 'Bone and joint problems, injuries', 160.00);

-- Insert Departments
INSERT INTO [Department] ([DeptName], [Description]) VALUES
('Emergency', 'Emergency medical services'),
('Cardiology', 'Heart and cardiovascular treatments'),
('Pediatrics', 'Children medical care'),
('Surgery', 'Surgical procedures'),
('Radiology', 'Medical imaging services');

-- Insert Rooms
INSERT INTO [Room] ([RoomNumber], [RoomName], [Type], [Capacity], [Status], [DepartmentID]) VALUES
('101', 'Consultation Room 1', 'Consultation', 1, 'Available', 1),
('102', 'Examination Room 1', 'Examination', 1, 'Available', 2),
('201', 'Surgery Room 1', 'Operation', 4, 'Available', 4),
('301', 'Pediatric Room 1', 'Examination', 1, 'Available', 3),
('401', 'X-Ray Room', 'Laboratory', 2, 'Available', 5);

-- Insert Demo Users (Admin)
INSERT INTO [Users] ([FullName], [Email], [PasswordHash], [PhoneNumber], [RoleID], [IsGuest], [Status]) VALUES
('System Admin', 'admin@hospital.com', '$2a$10$hashedpassword1', '+1234567890', 1, 0, 'Active');

-- Insert Demo Doctors
INSERT INTO [Users] ([FullName], [Email], [PasswordHash], [PhoneNumber], [RoleID], [IsGuest], [Status]) VALUES
('John Smith', 'john.smith@hospital.com', '$2a$10$hashedpassword2', '+1234567891', 2, 0, 'Active'),
('Sarah Johnson', 'sarah.johnson@hospital.com', '$2a$10$hashedpassword3', '+1234567892', 2, 0, 'Active'),
('Michael Brown', 'michael.brown@hospital.com', '$2a$10$hashedpassword4', '+1234567893', 2, 0, 'Active');

-- Insert Demo Patients
INSERT INTO [Users] ([FullName], [Email], [PasswordHash], [PhoneNumber], [RoleID], [IsGuest], [Status]) VALUES
('Alice Williams', 'alice.williams@email.com', '$2a$10$hashedpassword5', '+1234567894', 4, 0, 'Active'),
('Bob Davis', 'bob.davis@email.com', '$2a$10$hashedpassword6', '+1234567895', 4, 0, 'Active'),
('Carol Miller', 'carol.miller@email.com', '$2a$10$hashedpassword7', '+1234567896', 4, 0, 'Active'),
('David Wilson', 'david.wilson@email.com', '$2a$10$hashedpassword8', '+1234567897', 4, 0, 'Active');

-- Insert Demo Receptionist
INSERT INTO [Users] ([FullName], [Email], [PasswordHash], [PhoneNumber], [RoleID], [IsGuest], [Status]) VALUES
('Emma Thompson', 'emma.thompson@hospital.com', '$2a$10$hashedpassword9', '+1234567898', 3, 0, 'Active');

-- Update Doctor table with bio descriptions
UPDATE [Doctor] SET [BioDescription] = 'Experienced cardiologist with 15 years of practice. Specializes in heart disease prevention and treatment.' WHERE [UserID] = 2;
UPDATE [Doctor] SET [BioDescription] = 'Board-certified dermatologist focusing on skin cancer prevention and cosmetic dermatology.' WHERE [UserID] = 3;
UPDATE [Doctor] SET [BioDescription] = 'Pediatric specialist with expertise in child development and infectious diseases.' WHERE [UserID] = 4;

-- Update Patient table with additional info
UPDATE [Patient] SET [dateOfBirth] = '1985-03-15', [gender] = 'Female', [description] = 'Regular checkups, no major health issues' WHERE [UserID] = 5;
UPDATE [Patient] SET [dateOfBirth] = '1978-07-22', [gender] = 'Male', [description] = 'History of hypertension, regular monitoring needed' WHERE [UserID] = 6;
UPDATE [Patient] SET [dateOfBirth] = '1992-11-08', [gender] = 'Female', [description] = 'Allergic to penicillin' WHERE [UserID] = 7;
UPDATE [Patient] SET [dateOfBirth] = '1965-01-30', [gender] = 'Male', [description] = 'Diabetic, requires regular blood sugar monitoring' WHERE [UserID] = 8;

-- Insert Doctor Specializations
INSERT INTO [DoctorSpecialization] ([DoctorID], [SpecID]) VALUES
(1, 1), -- Dr. John Smith - Cardiology
(2, 2), -- Dr. Sarah Johnson - Dermatology
(3, 4); -- Dr. Michael Brown - Pediatrics

-- Insert Services
INSERT INTO [Service] ([SpecID], [ServiceName], [Price]) VALUES
(1, 'ECG Test', 75.00),
(1, 'Echocardiogram', 200.00),
(2, 'Skin Biopsy', 150.00),
(2, 'Mole Removal', 125.00),
(4, 'Child Wellness Exam', 80.00);

-- Insert Medicine Inventory
INSERT INTO [MedicineInventory] ([MedicineName], [GenericName], [Category], [UnitOfMeasure], [CurrentStock], [ReorderLevel], [ExpiryDate], [Cost], [Supplier]) VALUES
('Lisinopril 10mg', 'Lisinopril', 'ACE Inhibitor', 'Tablet', 500, 50, '2025-12-31', 0.25, 'PharmaCorp'),
('Metformin 500mg', 'Metformin HCl', 'Antidiabetic', 'Tablet', 300, 30, '2025-10-15', 0.15, 'MediSupply'),
('Amoxicillin 250mg', 'Amoxicillin', 'Antibiotic', 'Capsule', 200, 25, '2025-08-20', 0.50, 'PharmaCorp');

-- Insert Sample Appointments
INSERT INTO [Appointment] ([DoctorID], [PatientID], [RoomID], [Description], [DateTime], [Status], [Email], [PhoneNumber]) VALUES
(1, 1, 2, 'Routine cardiac checkup', '2024-12-20 10:00:00', 'Scheduled', 'alice.williams@email.com', '+1234567894'),
(2, 2, 1, 'Skin rash consultation', '2024-12-21 14:30:00', 'Scheduled', 'bob.davis@email.com', '+1234567895'),
(3, 3, 4, 'Child wellness exam', '2024-12-22 09:15:00', 'Scheduled', 'carol.miller@email.com', '+1234567896'),
(1, 4, 2, 'Diabetes follow-up', '2024-12-19 11:00:00', 'Completed', 'david.wilson@email.com', '+1234567897');

-- Insert Sample Schedules
INSERT INTO [Schedule] ([DoctorID], [RoomID], [ScheduleDate], [startTime], [endTime], [PatientID], [EventType], [Description]) VALUES
(1, 2, '2024-12-20', '10:00:00', '10:30:00', 1, 'appointment', 'Cardiac checkup'),
(2, 1, '2024-12-21', '14:30:00', '15:00:00', 2, 'appointment', 'Skin consultation'),
(3, 4, '2024-12-22', '09:15:00', '09:45:00', 3, 'appointment', 'Wellness exam');

-- Insert Sample Transactions
INSERT INTO [Transaction] ([AppointmentID], [UserID], [Method], [TimeOfPayment], [Status], [ProcessedByUserID]) VALUES
(4, 8, 'Banking', '2024-12-19 11:30:00', 'Paid', 9),
(1, 5, 'Cash', '2024-12-20 10:30:00', 'Pending', NULL);

-- Insert Sample Notifications
INSERT INTO [Notification] ([UserID], [Message], [Type]) VALUES
(5, 'Your appointment with Dr. John Smith is scheduled for tomorrow at 10:00 AM', 'Reminder'),
(6, 'Your consultation fee payment is pending. Please complete payment.', 'BillingUpdate'),
(7, 'Welcome to our hospital system! Your account has been created successfully.', 'AdminNotice');

PRINT 'Fresh demonstration data has been created successfully!';