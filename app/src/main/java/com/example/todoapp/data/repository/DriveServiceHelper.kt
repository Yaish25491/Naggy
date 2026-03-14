package com.example.todoapp.data.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DriveServiceHelper(private val driveService: Drive) {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    fun uploadFile(localFilePath: String, driveFileName: String): Task<String> {
        return Tasks.call(executor) {
            val metadata = File()
                .setName(driveFileName)
                .setParents(listOf("appDataFolder"))

            val localFile = java.io.File(localFilePath)
            val mediaContent = FileContent("application/octet-stream", localFile)

            // Check if file already exists
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .execute()
            
            val existingFile = result.files.find { it.name == driveFileName }

            val googleFile = if (existingFile != null) {
                driveService.files().update(existingFile.id, null, mediaContent).execute()
            } else {
                driveService.files().create(metadata, mediaContent).execute()
            }
            
            googleFile.id
        }
    }

    fun downloadFile(driveFileName: String, localFilePath: String): Task<Void?> {
        return Tasks.call(executor) {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .execute()

            val driveFile = result.files.find { it.name == driveFileName }
                ?: throw Exception("File not found on Drive")

            val localFile = java.io.File(localFilePath)
            driveService.files().get(driveFile.id).executeMediaAndDownloadTo(FileOutputStream(localFile))
            null
        }
    }
}
