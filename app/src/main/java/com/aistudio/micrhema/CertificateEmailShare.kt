package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CertificateEmailShare {
    fun createLinkIntent(recipientEmail: String, certificateUrl: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(recipientEmail)}")
            putExtra(Intent.EXTRA_SUBJECT, "Certificado IBR — MIC Rhema")
            putExtra(
                Intent.EXTRA_TEXT,
                "Olá!\n\nSegue o acesso ao seu certificado de conclusão do IBR da MIC Rhema:\n$certificateUrl"
            )
        }
    }

    fun createIntent(context: Context, recipientEmail: String, certificateFile: File): Intent {
        val attachmentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            certificateFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Certificado IBR — MIC Rhema")
            putExtra(
                Intent.EXTRA_TEXT,
                "Olá!\n\nSegue em anexo o seu certificado de conclusão do IBR da MIC Rhema.\n\nEste e-mail foi preparado pelo aplicativo MIC Rhema."
            )
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
