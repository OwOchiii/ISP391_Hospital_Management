-- SQL Script to update column types for Vietnamese character support
-- This script changes varchar columns to nvarchar and sets the appropriate collation

-- Set database collation (if permissions allow)
-- ALTER DATABASE Medicare COLLATE Vietnamese_CI_AS;

-- Users table
ALTER TABLE [Users] ALTER COLUMN [FullName] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [Users] ALTER COLUMN [Email] nvarchar(255) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Users] ALTER COLUMN [PasswordHash] nvarchar(255) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Users] ALTER COLUMN [PhoneNumber] nvarchar(20) COLLATE Vietnamese_CI_AS NULL;

-- Role table
ALTER TABLE [Role] ALTER COLUMN [RoleName] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [Role] ALTER COLUMN [Description] nvarchar(max) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Role] ALTER COLUMN [Permission] nvarchar(max) COLLATE Vietnamese_CI_AS NULL;

-- Patient table
ALTER TABLE [Patient] ALTER COLUMN [gender] nvarchar(10) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Patient] ALTER COLUMN [description] nvarchar(max) COLLATE Vietnamese_CI_AS NULL;

-- PatientContact table
ALTER TABLE [PatientContact] ALTER COLUMN [AddressType] nvarchar(50) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [PatientContact] ALTER COLUMN [StreetAddress] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [PatientContact] ALTER COLUMN [City] nvarchar(100) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [PatientContact] ALTER COLUMN [State] nvarchar(100) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [PatientContact] ALTER COLUMN [PostalCode] nvarchar(20) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [PatientContact] ALTER COLUMN [Country] nvarchar(100) COLLATE Vietnamese_CI_AS NOT NULL;

-- Doctor table
ALTER TABLE [Doctor] ALTER COLUMN [BioDescription] nvarchar(max) COLLATE Vietnamese_CI_AS NULL;

-- DoctorEducation table
ALTER TABLE [DoctorEducation] ALTER COLUMN [Degree] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [DoctorEducation] ALTER COLUMN [Institution] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [DoctorEducation] ALTER COLUMN [Description] nvarchar(max) COLLATE Vietnamese_CI_AS NULL;

-- Specialization table
ALTER TABLE [Specialization] ALTER COLUMN [SpecName] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [Specialization] ALTER COLUMN [Symptom] nvarchar(255) COLLATE Vietnamese_CI_AS NULL;

-- Service table
ALTER TABLE [Service] ALTER COLUMN [ServiceName] nvarchar(255) COLLATE Vietnamese_CI_AS NOT NULL;

-- Appointment table
ALTER TABLE [Appointment] ALTER COLUMN [Description] nvarchar(255) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Appointment] ALTER COLUMN [Status] nvarchar(20) COLLATE Vietnamese_CI_AS NOT NULL;
ALTER TABLE [Appointment] ALTER COLUMN [Email] nvarchar(255) COLLATE Vietnamese_CI_AS NULL;
ALTER TABLE [Appointment] ALTER COLUMN [PhoneNumber] nvarchar(20) COLLATE Vietnamese_CI_AS NULL;

-- Transaction table
ALTER TABLE [Transaction] ALTER COLUMN [Method] nvarchar(20) COLLATE Vietnamese_CI_AS NOT NULL;

-- Add other tables as needed from your schema
