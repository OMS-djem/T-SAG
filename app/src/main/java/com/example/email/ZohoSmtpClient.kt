package com.example.email

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object ZohoSmtpClient {

    const val DEFAULT_ZOHO_HOST = "smtp.zoho.com"
    const val DEFAULT_ZOHO_PORT = 465
    const val SENDER_EMAIL = "mdjemil@mobiserveholding.com"

    sealed class SmtpResult {
        data class Success(val message: String) : SmtpResult()
        data class Error(val message: String, val details: String? = null) : SmtpResult()
    }

    suspend fun testConnection(
        host: String = DEFAULT_ZOHO_HOST,
        port: Int = DEFAULT_ZOHO_PORT,
        senderEmail: String = SENDER_EMAIL,
        password: String
    ): SmtpResult = withContext(Dispatchers.IO) {
        if (password.isBlank()) {
            return@withContext SmtpResult.Error(
                "Zoho password / App Password is required.",
                "Go to Settings > Zoho Mail Configuration to enter your Zoho password or generate an App Password at accounts.zoho.com"
            )
        }

        var socket: SSLSocket? = null
        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = factory.createSocket(host, port) as SSLSocket
            socket.soTimeout = 15000

            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))

            val banner = readResponse(reader)
            if (!banner.startsWith("220")) {
                return@withContext SmtpResult.Error("Unexpected server greeting: $banner")
            }

            sendCommand(writer, "EHLO localhost")
            readMultiLineResponse(reader)

            sendCommand(writer, "AUTH LOGIN")
            val authResp = readResponse(reader)
            if (!authResp.startsWith("334")) {
                return@withContext SmtpResult.Error("AUTH LOGIN rejected: $authResp")
            }

            val b64User = Base64.encodeToString(senderEmail.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, b64User)
            val userResp = readResponse(reader)
            if (!userResp.startsWith("334")) {
                return@withContext SmtpResult.Error("Username rejected: $userResp")
            }

            val b64Pass = Base64.encodeToString(password.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, b64Pass)
            val passResp = readResponse(reader)
            if (!passResp.startsWith("235")) {
                return@withContext SmtpResult.Error(
                    "Authentication Failed (Zoho code: $passResp)",
                    "Zoho rejected credentials. If your Zoho account has Two-Factor Authentication (2FA), please generate an App Password at accounts.zoho.com > Security > App Passwords."
                )
            }

            sendCommand(writer, "QUIT")
            SmtpResult.Success("Connection and Authentication to Zoho SMTP successful as $senderEmail!")
        } catch (e: Exception) {
            SmtpResult.Error(
                "Connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                e.message
            )
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun sendEmail(
        host: String = DEFAULT_ZOHO_HOST,
        port: Int = DEFAULT_ZOHO_PORT,
        senderEmail: String = SENDER_EMAIL,
        sessionSenderName: String = "Mobiserve Field Audit NOC",
        password: String,
        recipients: List<String>,
        ccRecipients: List<String> = emptyList(),
        subject: String,
        bodyText: String,
        attachments: List<File> = emptyList()
    ): SmtpResult = withContext(Dispatchers.IO) {
        if (password.isBlank()) {
            return@withContext SmtpResult.Error(
                "Zoho password / App Password is required to send directly from $senderEmail.",
                "Please enter your Zoho Mail password or App Password in the Zoho Mail Settings card."
            )
        }

        val validRecipients = recipients.filter { it.isNotBlank() }
        val validCcRecipients = ccRecipients.filter { it.isNotBlank() }
        if (validRecipients.isEmpty() && validCcRecipients.isEmpty()) {
            return@withContext SmtpResult.Error("No recipient email specified.")
        }

        var socket: SSLSocket? = null
        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = factory.createSocket(host, port) as SSLSocket
            socket.soTimeout = 30000

            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))

            // 1. Initial Greeting
            val banner = readResponse(reader)
            if (!banner.startsWith("220")) {
                return@withContext SmtpResult.Error("Server greeting error: $banner")
            }

            // 2. EHLO
            sendCommand(writer, "EHLO localhost")
            readMultiLineResponse(reader)

            // 3. AUTH LOGIN
            sendCommand(writer, "AUTH LOGIN")
            val authResp = readResponse(reader)
            if (!authResp.startsWith("334")) {
                return@withContext SmtpResult.Error("AUTH LOGIN rejected: $authResp")
            }

            // Username
            val b64User = Base64.encodeToString(senderEmail.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, b64User)
            val userResp = readResponse(reader)
            if (!userResp.startsWith("334")) {
                return@withContext SmtpResult.Error("Username rejected by Zoho: $userResp")
            }

            // Password
            val b64Pass = Base64.encodeToString(password.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, b64Pass)
            val passResp = readResponse(reader)
            if (!passResp.startsWith("235")) {
                return@withContext SmtpResult.Error(
                    "Authentication Failed (Zoho code: $passResp)",
                    "Zoho rejected credentials. If your account has 2FA enabled, please generate an App-Specific Password at accounts.zoho.com > Security > App Passwords."
                )
            }

            // 4. MAIL FROM
            sendCommand(writer, "MAIL FROM:<${senderEmail.trim()}>")
            val mailFromResp = readResponse(reader)
            if (!mailFromResp.startsWith("250")) {
                return@withContext SmtpResult.Error("MAIL FROM rejected: $mailFromResp")
            }

            // 5. RCPT TO for primary recipients
            for (rcpt in validRecipients) {
                sendCommand(writer, "RCPT TO:<${rcpt.trim()}>")
                val rcptResp = readResponse(reader)
                if (!rcptResp.startsWith("250") && !rcptResp.startsWith("251")) {
                    return@withContext SmtpResult.Error("Recipient <$rcpt> rejected: $rcptResp")
                }
            }

            // RCPT TO for CC group recipients
            for (cc in validCcRecipients) {
                sendCommand(writer, "RCPT TO:<${cc.trim()}>")
                val rcptResp = readResponse(reader)
                if (!rcptResp.startsWith("250") && !rcptResp.startsWith("251")) {
                    return@withContext SmtpResult.Error("CC Recipient <$cc> rejected: $rcptResp")
                }
            }

            // 6. DATA
            sendCommand(writer, "DATA")
            val dataResp = readResponse(reader)
            if (!dataResp.startsWith("354")) {
                return@withContext SmtpResult.Error("DATA command rejected: $dataResp")
            }

            // 7. Write MIME Message
            val boundary = "==_MOBI_SITE_AUDIT_" + UUID.randomUUID().toString().replace("-", "")
            val rfcDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())

            val cleanSenderName = sessionSenderName.ifBlank { "Mobiserve Field Operations" }.replace("\"", "")
            writer.write("From: \"$cleanSenderName\" <$senderEmail>\r\n")
            writer.write("To: ${validRecipients.joinToString(", ")}\r\n")
            if (validCcRecipients.isNotEmpty()) {
                writer.write("Cc: ${validCcRecipients.joinToString(", ")}\r\n")
            }
            writer.write("Subject: $subject\r\n")
            writer.write("Date: $rfcDate\r\n")
            writer.write("MIME-Version: 1.0\r\n")
            writer.write("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n")
            writer.write("X-Mailer: Mobiserve Field Operations Android Client\r\n")
            writer.write("\r\n")

            // Part 1: Body Text
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n")
            writer.write("Content-Transfer-Encoding: 8bit\r\n")
            writer.write("\r\n")
            writer.write(bodyText)
            writer.write("\r\n\r\n")

            // Part 2..N: Attachments
            for (file in attachments) {
                if (file.exists() && file.length() > 0) {
                    writer.write("--$boundary\r\n")
                    writer.write("Content-Type: image/jpeg; name=\"${file.name}\"\r\n")
                    writer.write("Content-Transfer-Encoding: base64\r\n")
                    writer.write("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
                    writer.write("\r\n")

                    // Read and Base64 encode file chunks
                    val bytes = file.readBytes()
                    val base64Content = Base64.encodeToString(bytes, Base64.DEFAULT)
                    writer.write(base64Content)
                    writer.write("\r\n")
                }
            }

            // End of MIME multipart
            writer.write("--$boundary--\r\n")
            writer.write(".\r\n")
            writer.flush()

            val endDataResp = readResponse(reader)
            if (!endDataResp.startsWith("250")) {
                return@withContext SmtpResult.Error("Error sending message data: $endDataResp")
            }

            // 8. QUIT
            sendCommand(writer, "QUIT")
            readResponse(reader)

            SmtpResult.Success("Email sent directly from $senderEmail via Zoho Mail SMTP!")
        } catch (e: Exception) {
            SmtpResult.Error(
                "Failed to send email: ${e.localizedMessage ?: e.javaClass.simpleName}",
                e.message
            )
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun sendCommand(writer: BufferedWriter, command: String) {
        writer.write(command)
        writer.write("\r\n")
        writer.flush()
    }

    private fun readResponse(reader: BufferedReader): String {
        return reader.readLine() ?: throw java.io.IOException("Connection closed unexpectedly by server.")
    }

    private fun readMultiLineResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            sb.append(line).append("\n")
            // In SMTP multiline response, code followed by '-' indicates more lines (e.g. 250-SIZE)
            // Code followed by space indicates last line (e.g. 250 OK)
            if (line.length >= 4 && line[3] == ' ') {
                break
            }
        }
        return sb.toString()
    }
}
