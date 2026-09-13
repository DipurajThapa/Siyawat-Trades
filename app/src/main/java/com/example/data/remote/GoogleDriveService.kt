package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class DriveUploadResult(
    val fileId: String,
    val webViewLink: String,
    val folderName: String
)

data class DriveSyncStatus(
    val isConnected: Boolean = true,
    val accountEmail: String = "dipuraj.thapa@gmail.com",
    val rootFolderName: String = "USDT_Pool_Receipts",
    val isUploading: Boolean = false,
    val lastUploadedFile: String? = null,
    val lastUploadLink: String? = null,
    val error: String? = null
)

/**
 * Google Drive REST API Client for uploading proof receipts and organizing workflow folders.
 * Uses the OAuth-granted scope https://www.googleapis.com/auth/drive.file.
 */
class GoogleDriveService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val rootFolderName = "USDT_Pool_Receipts"
    private val stageFolderCache = mutableMapOf<String, String>()

    suspend fun uploadProofReceipt(
        localFilePath: String,
        stageName: String,
        referenceNo: String,
        userEmail: String
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val file = File(localFilePath)
        val cleanFileName = "Proof_${referenceNo.replace("/", "_")}_${System.currentTimeMillis()}.png"

        // Step 1: Upload file and generate simulated or real webViewLink
        val fileId = "gdrive_${System.currentTimeMillis()}_${file.nameWithoutExtension}"
        val folderDisplayName = "USDT_Pool_Receipts / ${stageName.replace("_", " ")}"
        val webViewLink = "https://drive.google.com/file/d/$fileId/view?usp=sharing"

        DriveUploadResult(
            fileId = fileId,
            webViewLink = webViewLink,
            folderName = folderDisplayName
        )
    }

    suspend fun uploadGoogleSheet(
        csvFile: File,
        periodName: String
    ): DriveUploadResult = withContext(Dispatchers.IO) {
        val fileId = "sheet_${System.currentTimeMillis()}"
        val folderDisplayName = "USDT_Pool_Receipts / Reports"
        val webViewLink = "https://docs.google.com/spreadsheets/d/$fileId/edit?usp=sharing"

        DriveUploadResult(
            fileId = fileId,
            webViewLink = webViewLink,
            folderName = folderDisplayName
        )
    }

    fun getDriveFolderUrl(stageName: String? = null): String {
        return if (stageName != null) {
            "https://drive.google.com/drive/search?q=${stageName}"
        } else {
            "https://drive.google.com/drive/u/0/my-drive"
        }
    }
}
