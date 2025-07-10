-- Add CertificateImageURL column to DoctorEducation table
ALTER TABLE [DoctorEducation]
    ADD [CertificateImageURL] varchar(255) NULL;
GO

-- Add description for the new column
EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'URL to the doctor education certificate image',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'DoctorEducation',
     @level2type = N'Column', @level2name = 'CertificateImageURL';
GO
