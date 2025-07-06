-- Add Receptionist table with ImageURL field
CREATE TABLE [Receptionist] (
    [ReceptionistID] int PRIMARY KEY IDENTITY(1,1),
    [UserID] int NOT NULL UNIQUE,
    [ImageURL] varchar(255) NULL,
    CONSTRAINT FK_Receptionist_User FOREIGN KEY ([UserID]) REFERENCES [Users] ([UserID])
);
GO

-- Add index for faster queries
CREATE INDEX [IX_Receptionist_UserID] ON [Receptionist] ([UserID]);
GO

-- Add description
EXEC sp_addextendedproperty
     @name = N'Table_Description',
     @value = 'Stores information about receptionists in the hospital system',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Receptionist'
GO

-- Add ImageURL column to Doctor table
ALTER TABLE [Doctor]
    ADD [ImageURL] varchar(255) NULL;
GO

-- Add description for the new column
EXEC sp_addextendedproperty
     @name = N'Column_Description',
     @value = 'URL to the doctor profile image',
     @level0type = N'Schema', @level0name = 'dbo',
     @level1type = N'Table',  @level1name = 'Doctor',
     @level2type = N'Column', @level2name = 'ImageURL';
GO

-- Create trigger for adding receptionist record when user with receptionist role is created
CREATE OR ALTER TRIGGER trg_AddReceptionistOnUserCreation
    ON [Users]
    AFTER INSERT
    AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @RoleIDForReceptionist INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'RECEPTIONIST');

    -- Insert into Receptionist table for new users with RECEPTIONIST role
    INSERT INTO [Receptionist] ([UserID], [ImageURL])
    SELECT i.[UserID], NULL
    FROM inserted i
    WHERE i.[RoleID] = @RoleIDForReceptionist;
END;
GO

-- Update existing trigger to handle receptionist role changes
CREATE OR ALTER TRIGGER trg_ManageUserRoleChanges
    ON [Users]
    AFTER UPDATE
    AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(RoleID) -- Only execute if RoleID was updated
        BEGIN
            DECLARE @RoleIDForPatient INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'PATIENT');
            DECLARE @RoleIDForDoctor INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'DOCTOR');
            DECLARE @RoleIDForReceptionist INT = (SELECT [RoleID] FROM [Role] WHERE [RoleName] = 'RECEPTIONIST');

            -- Process each updated row individually
            DECLARE @UserID INT, @NewRoleID INT, @OldRoleID INT
            DECLARE user_cursor CURSOR FOR
                SELECT i.[UserID], i.[RoleID], d.[RoleID]
                FROM inserted i
                         INNER JOIN deleted d ON i.[UserID] = d.[UserID]
                WHERE i.[RoleID] <> d.[RoleID]; -- Only process rows where role actually changed

            OPEN user_cursor
            FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID

            WHILE @@FETCH_STATUS = 0
                BEGIN
                    -- Handle Receptionist role assignment
                    IF @NewRoleID = @RoleIDForReceptionist AND NOT EXISTS (SELECT 1 FROM [Receptionist] WHERE [UserID] = @UserID)
                        BEGIN
                            INSERT INTO [Receptionist] ([UserID], [ImageURL])
                            VALUES (@UserID, NULL);
                        END

                    -- The rest of the existing logic for Patient and Doctor roles
                    IF @NewRoleID = @RoleIDForDoctor AND @OldRoleID = @RoleIDForPatient
                        BEGIN
                            -- Keep the Patient record but also add Doctor record
                            IF NOT EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Doctor] ([UserID], [BioDescription], [ImageURL])
                                    VALUES (@UserID, NULL, NULL);
                                END
                        END
                    ELSE IF @NewRoleID = @RoleIDForPatient AND @OldRoleID = @RoleIDForDoctor
                        BEGIN
                            -- Keep the Doctor record but also add Patient record
                            IF NOT EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [description])
                                    VALUES (@UserID, NULL, NULL, NULL);
                                END
                        END
                    ELSE
                        BEGIN
                            -- Make sure appropriate role-specific record exists
                            IF @NewRoleID = @RoleIDForPatient AND NOT EXISTS (SELECT 1 FROM [Patient] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Patient] ([UserID], [dateOfBirth], [gender], [description])
                                    VALUES (@UserID, NULL, NULL, NULL);
                                END
                            ELSE IF @NewRoleID = @RoleIDForDoctor AND NOT EXISTS (SELECT 1 FROM [Doctor] WHERE [UserID] = @UserID)
                                BEGIN
                                    INSERT INTO [Doctor] ([UserID], [BioDescription], [ImageURL])
                                    VALUES (@UserID, NULL, NULL);
                                END
                        END

                    FETCH NEXT FROM user_cursor INTO @UserID, @NewRoleID, @OldRoleID
                END

            CLOSE user_cursor
            DEALLOCATE user_cursor
        END
END;
GO
