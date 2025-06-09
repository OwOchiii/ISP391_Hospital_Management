-- Add appointmentId column to Schedule table
ALTER TABLE Schedule ADD AppointmentID INT;

-- Add foreign key constraint to link Schedule to Appointment
ALTER TABLE Schedule
ADD CONSTRAINT FK_Schedule_Appointment
FOREIGN KEY (AppointmentID)
REFERENCES Appointment(AppointmentID);

-- Update existing Schedule records where type is 'appointment' and patientId exists
-- This assumes there's a matching Appointment record with the same patientId and doctorId
-- This is a sample approach - you might need to adjust based on your actual data
UPDATE s
SET s.AppointmentID = a.AppointmentID
FROM Schedule s
INNER JOIN Appointment a ON s.PatientID = a.PatientID AND s.DoctorID = a.DoctorID
WHERE s.EventType = 'appointment' AND s.PatientID IS NOT NULL AND s.AppointmentID IS NULL;

-- Create an index on AppointmentID for better performance
CREATE INDEX IDX_Schedule_AppointmentID ON Schedule(AppointmentID);
