-- Ghost data script for Schedule and Appointment tables
-- Date range: June 9, 2025 - June 23, 2025

-- Clear any existing test data in this date range
DELETE FROM Schedule
WHERE ScheduleDate BETWEEN '2025-06-09' AND '2025-06-23';

DELETE FROM Appointment
WHERE CAST(DateTime AS DATE) BETWEEN '2025-06-09' AND '2025-06-23';

-- Insert appointments first (we'll link them to schedules)
INSERT INTO Appointment (DoctorID, PatientID, RoomID, Description, DateTime, Status, Email, PhoneNumber)
VALUES
-- Monday, June 9, 2025
(1, 5, 201, 'Annual checkup', '2025-06-09 09:00:00', 'Scheduled', 'patient5@example.com', '555-0105'),
(2, 8, 202, 'Cardiac consultation', '2025-06-09 10:30:00', 'Scheduled', 'patient8@example.com', '555-0108'),
(3, 12, 203, 'Post-surgery follow-up', '2025-06-09 14:00:00', 'Scheduled', 'patient12@example.com', '555-0112'),

-- Tuesday, June 10, 2025
(1, 15, 201, 'Diabetes management', '2025-06-10 08:30:00', 'Scheduled', 'patient15@example.com', '555-0115'),
(4, 3, 204, 'Respiratory evaluation', '2025-06-10 11:00:00', 'Scheduled', 'patient3@example.com', '555-0103'),
(2, 9, 202, 'Blood pressure check', '2025-06-10 15:30:00', 'Scheduled', 'patient9@example.com', '555-0109'),

-- Wednesday, June 11, 2025
(3, 6, 203, 'Physiotherapy session', '2025-06-11 09:30:00', 'Scheduled', 'patient6@example.com', '555-0106'),
(5, 11, 205, 'Dermatology consultation', '2025-06-11 13:00:00', 'Scheduled', 'patient11@example.com', '555-0111'),
(4, 2, 204, 'Lab results review', '2025-06-11 16:00:00', 'Scheduled', 'patient2@example.com', '555-0102'),

-- Thursday, June 12, 2025
(1, 4, 201, 'Vaccination', '2025-06-12 08:00:00', 'Scheduled', 'patient4@example.com', '555-0104'),
(2, 13, 202, 'Heart monitoring', '2025-06-12 10:00:00', 'Scheduled', 'patient13@example.com', '555-0113'),
(5, 7, 205, 'Skin condition follow-up', '2025-06-12 14:30:00', 'Scheduled', 'patient7@example.com', '555-0107'),

-- Friday, June 13, 2025
(3, 10, 203, 'Physical rehabilitation', '2025-06-13 09:00:00', 'Scheduled', 'patient10@example.com', '555-0110'),
(4, 14, 204, 'Asthma management', '2025-06-13 11:30:00', 'Scheduled', 'patient14@example.com', '555-0114'),
(1, 1, 201, 'General consultation', '2025-06-13 15:00:00', 'Scheduled', 'patient1@example.com', '555-0101'),

-- Monday, June 16, 2025
(2, 5, 202, 'Heart checkup', '2025-06-16 08:30:00', 'Scheduled', 'patient5@example.com', '555-0105'),
(3, 8, 203, 'Rehabilitation assessment', '2025-06-16 10:00:00', 'Scheduled', 'patient8@example.com', '555-0108'),
(5, 12, 205, 'Skin biopsy results', '2025-06-16 14:30:00', 'Scheduled', 'patient12@example.com', '555-0112'),

-- Tuesday, June 17, 2025
(1, 3, 201, 'Follow-up consultation', '2025-06-17 09:30:00', 'Scheduled', 'patient3@example.com', '555-0103'),
(4, 9, 204, 'Respiratory therapy', '2025-06-17 11:00:00', 'Scheduled', 'patient9@example.com', '555-0109'),
(2, 15, 202, 'Cardiac stress test', '2025-06-17 15:00:00', 'Scheduled', 'patient15@example.com', '555-0115'),

-- Wednesday, June 18, 2025
(3, 2, 203, 'Post-surgery check', '2025-06-18 08:00:00', 'Scheduled', 'patient2@example.com', '555-0102'),
(5, 6, 205, 'Acne treatment follow-up', '2025-06-18 10:30:00', 'Scheduled', 'patient6@example.com', '555-0106'),
(1, 11, 201, 'Blood test review', '2025-06-18 14:00:00', 'Scheduled', 'patient11@example.com', '555-0111'),

-- Thursday, June 19, 2025
(4, 4, 204, 'Lung function test', '2025-06-19 09:00:00', 'Scheduled', 'patient4@example.com', '555-0104'),
(2, 7, 202, 'ECG review', '2025-06-19 11:30:00', 'Scheduled', 'patient7@example.com', '555-0107'),
(3, 13, 203, 'Joint pain assessment', '2025-06-19 15:30:00', 'Scheduled', 'patient13@example.com', '555-0113'),

-- Friday, June 20, 2025
(5, 1, 205, 'Mole check', '2025-06-20 08:30:00', 'Scheduled', 'patient1@example.com', '555-0101'),
(1, 10, 201, 'Prescription renewal', '2025-06-20 10:00:00', 'Scheduled', 'patient10@example.com', '555-0110'),
(4, 14, 204, 'Breathing treatment', '2025-06-20 14:00:00', 'Scheduled', 'patient14@example.com', '555-0114');

-- Get the IDs of the inserted appointments
DECLARE @AppointmentIDs TABLE (ID INT, RowNum INT IDENTITY(1,1));
INSERT INTO @AppointmentIDs (ID)
SELECT AppointmentID FROM Appointment
WHERE CAST(DateTime AS DATE) BETWEEN '2025-06-09' AND '2025-06-23'
ORDER BY DateTime;

-- Insert schedules linked to appointments
INSERT INTO Schedule (DoctorID, RoomID, PatientID, AppointmentID, ScheduleDate, startTime, endTime, EventType, Description)
SELECT
    a.DoctorID,
    a.RoomID,
    a.PatientID,
    a.AppointmentID,
    CAST(a.DateTime AS DATE),
    CAST(a.DateTime AS TIME),
    DATEADD(MINUTE, 30, CAST(a.DateTime AS TIME)), -- 30-minute appointments
    'appointment',
    a.Description
FROM Appointment a
WHERE CAST(a.DateTime AS DATE) BETWEEN '2025-06-09' AND '2025-06-23';

-- Insert on-call schedules (not linked to appointments)
INSERT INTO Schedule (DoctorID, RoomID, PatientID, AppointmentID, ScheduleDate, startTime, endTime, EventType, Description)
VALUES
-- Monday, June 9, 2025
(1, 101, NULL, NULL, '2025-06-09', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(3, 101, NULL, NULL, '2025-06-09', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Tuesday, June 10, 2025
(2, 101, NULL, NULL, '2025-06-10', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(4, 101, NULL, NULL, '2025-06-10', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Wednesday, June 11, 2025
(3, 101, NULL, NULL, '2025-06-11', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(5, 101, NULL, NULL, '2025-06-11', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Thursday, June 12, 2025
(4, 101, NULL, NULL, '2025-06-12', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(1, 101, NULL, NULL, '2025-06-12', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Friday, June 13, 2025
(5, 101, NULL, NULL, '2025-06-13', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(2, 101, NULL, NULL, '2025-06-13', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Saturday & Sunday, June 14-15, 2025 (weekend coverage)
(1, 102, NULL, NULL, '2025-06-14', '08:00:00', '20:00:00', 'oncall', 'Weekend on-call coverage'),
(2, 102, NULL, NULL, '2025-06-15', '08:00:00', '20:00:00', 'oncall', 'Weekend on-call coverage'),

-- Monday, June 16, 2025
(4, 101, NULL, NULL, '2025-06-16', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(1, 101, NULL, NULL, '2025-06-16', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Tuesday, June 17, 2025
(5, 101, NULL, NULL, '2025-06-17', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(3, 101, NULL, NULL, '2025-06-17', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Wednesday, June 18, 2025
(2, 101, NULL, NULL, '2025-06-18', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(4, 101, NULL, NULL, '2025-06-18', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Thursday, June 19, 2025
(1, 101, NULL, NULL, '2025-06-19', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(5, 101, NULL, NULL, '2025-06-19', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Friday, June 20, 2025
(3, 101, NULL, NULL, '2025-06-20', '07:00:00', '09:00:00', 'oncall', 'Morning emergency coverage'),
(2, 101, NULL, NULL, '2025-06-20', '17:00:00', '20:00:00', 'oncall', 'Evening emergency coverage'),

-- Saturday & Sunday, June 21-22, 2025 (weekend coverage)
(3, 102, NULL, NULL, '2025-06-21', '08:00:00', '20:00:00', 'oncall', 'Weekend on-call coverage'),
(4, 102, NULL, NULL, '2025-06-22', '08:00:00', '20:00:00', 'oncall', 'Weekend on-call coverage');

-- Insert break schedules
INSERT INTO Schedule (DoctorID, RoomID, PatientID, AppointmentID, ScheduleDate, startTime, endTime, EventType, Description)
VALUES
-- Lunch breaks for each doctor on weekdays
-- Week 1
(1, NULL, NULL, NULL, '2025-06-09', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-09', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-09', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-09', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-09', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-10', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-10', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-10', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-10', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-10', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-11', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-11', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-11', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-11', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-11', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-12', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-12', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-12', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-12', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-12', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-13', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-13', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-13', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-13', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-13', '12:00:00', '13:00:00', 'break', 'Lunch break'),

-- Week 2
(1, NULL, NULL, NULL, '2025-06-16', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-16', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-16', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-16', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-16', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-17', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-17', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-17', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-17', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-17', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-18', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-18', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-18', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-18', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-18', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-19', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-19', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-19', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-19', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-19', '12:00:00', '13:00:00', 'break', 'Lunch break'),

(1, NULL, NULL, NULL, '2025-06-20', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(2, NULL, NULL, NULL, '2025-06-20', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(3, NULL, NULL, NULL, '2025-06-20', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(4, NULL, NULL, NULL, '2025-06-20', '12:00:00', '13:00:00', 'break', 'Lunch break'),
(5, NULL, NULL, NULL, '2025-06-20', '12:00:00', '13:00:00', 'break', 'Lunch break'),

-- Additional short breaks
(1, NULL, NULL, NULL, '2025-06-09', '15:00:00', '15:15:00', 'break', 'Coffee break'),
(2, NULL, NULL, NULL, '2025-06-10', '09:30:00', '09:45:00', 'break', 'Coffee break'),
(3, NULL, NULL, NULL, '2025-06-11', '15:30:00', '15:45:00', 'break', 'Coffee break'),
(4, NULL, NULL, NULL, '2025-06-12', '10:00:00', '10:15:00', 'break', 'Coffee break'),
(5, NULL, NULL, NULL, '2025-06-13', '16:00:00', '16:15:00', 'break', 'Coffee break'),
(1, NULL, NULL, NULL, '2025-06-16', '09:30:00', '09:45:00', 'break', 'Coffee break'),
(2, NULL, NULL, NULL, '2025-06-17', '15:30:00', '15:45:00', 'break', 'Coffee break'),
(3, NULL, NULL, NULL, '2025-06-18', '10:00:00', '10:15:00', 'break', 'Coffee break'),
(4, NULL, NULL, NULL, '2025-06-19', '16:00:00', '16:15:00', 'break', 'Coffee break'),
(5, NULL, NULL, NULL, '2025-06-20', '09:30:00', '09:45:00', 'break', 'Coffee break'),

-- Special meetings/breaks
(1, 301, NULL, NULL, '2025-06-11', '08:00:00', '09:00:00', 'break', 'Department meeting'),
(2, 301, NULL, NULL, '2025-06-11', '08:00:00', '09:00:00', 'break', 'Department meeting'),
(3, 301, NULL, NULL, '2025-06-11', '08:00:00', '09:00:00', 'break', 'Department meeting'),
(4, 301, NULL, NULL, '2025-06-11', '08:00:00', '09:00:00', 'break', 'Department meeting'),
(5, 301, NULL, NULL, '2025-06-11', '08:00:00', '09:00:00', 'break', 'Department meeting'),

(1, 301, NULL, NULL, '2025-06-18', '08:00:00', '09:00:00', 'break', 'Staff training'),
(2, 301, NULL, NULL, '2025-06-18', '08:00:00', '09:00:00', 'break', 'Staff training'),
(3, 301, NULL, NULL, '2025-06-18', '08:00:00', '09:00:00', 'break', 'Staff training'),
(4, 301, NULL, NULL, '2025-06-18', '08:00:00', '09:00:00', 'break', 'Staff training'),
(5, 301, NULL, NULL, '2025-06-18', '08:00:00', '09:00:00', 'break', 'Staff training');

-- Add some 2-hour and longer appointments to test multi-hour display
INSERT INTO Appointment (DoctorID, PatientID, RoomID, Description, DateTime, Status, Email, PhoneNumber)
VALUES
(1, 16, 201, 'Comprehensive health assessment', '2025-06-09 13:30:00', 'Scheduled', 'patient16@example.com', '555-0116'),
(2, 17, 202, 'Advanced cardiac evaluation', '2025-06-11 13:30:00', 'Scheduled', 'patient17@example.com', '555-0117'),
(3, 18, 203, 'Extended physiotherapy session', '2025-06-13 13:30:00', 'Scheduled', 'patient18@example.com', '555-0118'),
(4, 19, 204, 'Detailed respiratory assessment', '2025-06-17 13:30:00', 'Scheduled', 'patient19@example.com', '555-0119'),
(5, 20, 205, 'Extensive dermatology procedure', '2025-06-19 13:30:00', 'Scheduled', 'patient20@example.com', '555-0120');

-- Get the IDs of the inserted long appointments
DECLARE @LongAppointmentIDs TABLE (ID INT, RowNum INT IDENTITY(1,1));
INSERT INTO @LongAppointmentIDs (ID)
SELECT AppointmentID FROM Appointment
WHERE Description LIKE 'Comprehensive%'
   OR Description LIKE 'Advanced%'
   OR Description LIKE 'Extended%'
   OR Description LIKE 'Detailed%'
   OR Description LIKE 'Extensive%'
ORDER BY DateTime;

-- Insert schedules for long appointments (2 hours)
INSERT INTO Schedule (DoctorID, RoomID, PatientID, AppointmentID, ScheduleDate, startTime, endTime, EventType, Description)
SELECT
    a.DoctorID,
    a.RoomID,
    a.PatientID,
    a.AppointmentID,
    CAST(a.DateTime AS DATE),
    CAST(a.DateTime AS TIME),
    DATEADD(HOUR, 2, CAST(a.DateTime AS TIME)), -- 2-hour appointments
    'appointment',
    a.Description
FROM Appointment a
WHERE Description LIKE 'Comprehensive%'
   OR Description LIKE 'Advanced%'
   OR Description LIKE 'Extended%'
   OR Description LIKE 'Detailed%'
   OR Description LIKE 'Extensive%';
