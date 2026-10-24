package com.pdfmaster.app.pdf

import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.FileNotFoundException
import java.io.IOException

/** An error with a message that is safe to show to the user. */
class PdfToolException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

object PdfErrors {
    fun friendly(t: Throwable): String = when (t) {
        is PdfToolException -> t.userMessage
        is InvalidPasswordException -> "This PDF is password protected. Unlock it first, then try again."
        is OutOfMemoryError -> "This file is too large to process on this device."
        is FileNotFoundException -> "The file is no longer available. It may have been moved or deleted."
        is SecurityException -> "Permission to read this file was denied."
        is IOException -> "This file couldn't be read. It may be damaged or not a valid PDF."
        else -> "Something went wrong. Please try again."
    }
}
