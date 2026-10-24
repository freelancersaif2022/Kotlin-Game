package com.pdfmaster.app.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pdfmaster.app.ui.theme.ToolColors

enum class ToolCategory(val title: String) {
    CONVERT("Create & Convert"),
    ORGANIZE("Organize"),
    EDIT("Edit & Sign"),
    OPTIMIZE("Optimize"),
    SECURITY("Security"),
}

enum class PdfTool(
    val title: String,
    val shortTitle: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val category: ToolCategory,
    val isPro: Boolean = false,
    /** Counts towards the free daily limit. */
    val metered: Boolean = true,
) {
    IMAGE_TO_PDF("Image to PDF", "Image to PDF", "Turn photos and screenshots into a clean PDF", Icons.Rounded.Image, ToolColors.Red, ToolCategory.CONVERT),
    SCAN("Scan Document", "Scan", "Scan paper documents with auto edge detection", Icons.Rounded.DocumentScanner, ToolColors.Teal, ToolCategory.CONVERT),
    TEXT_TO_PDF("Text to PDF", "Text to PDF", "Type or paste text and save it as a PDF", Icons.Rounded.TextSnippet, ToolColors.Slate, ToolCategory.CONVERT),
    PDF_TO_IMAGES("PDF to Images", "PDF to JPG", "Export every page as a high-quality image", Icons.Rounded.PhotoLibrary, ToolColors.Orange, ToolCategory.CONVERT),
    EXTRACT_TEXT("Extract Text", "Extract Text", "Copy all the text out of a PDF", Icons.Rounded.Article, ToolColors.Blue, ToolCategory.CONVERT),
    OCR("OCR Text Recognition", "OCR", "Recognise text in scanned PDFs and images", Icons.Rounded.TextFields, ToolColors.Violet, ToolCategory.CONVERT, isPro = true),

    MERGE("Merge PDF", "Merge", "Combine several PDFs into one document", Icons.Rounded.CallMerge, ToolColors.Blue, ToolCategory.ORGANIZE),
    SPLIT("Split PDF", "Split", "Split a PDF or extract the pages you need", Icons.Rounded.CallSplit, ToolColors.Green, ToolCategory.ORGANIZE),
    ORGANIZE("Organize Pages", "Organize", "Reorder, rotate and delete pages", Icons.Rounded.DashboardCustomize, ToolColors.Indigo, ToolCategory.ORGANIZE),

    SIGN("Sign PDF", "Sign", "Draw your signature and place it anywhere", Icons.Rounded.Draw, ToolColors.Crimson, ToolCategory.EDIT),
    ANNOTATE("Edit & Annotate", "Annotate", "Highlight, draw and add text or notes", Icons.Rounded.EditNote, ToolColors.Pink, ToolCategory.EDIT),
    PAGE_NUMBERS("Page Numbers", "Page No.", "Add page numbers in the style you like", Icons.Rounded.FormatListNumbered, ToolColors.Teal, ToolCategory.EDIT),
    WATERMARK("Add Watermark", "Watermark", "Stamp a text watermark on every page", Icons.Rounded.BrandingWatermark, ToolColors.Amber, ToolCategory.EDIT, isPro = true),

    COMPRESS("Compress PDF", "Compress", "Reduce file size while keeping quality", Icons.Rounded.Compress, ToolColors.Orange, ToolCategory.OPTIMIZE),

    LOCK("Protect PDF", "Lock", "Secure a PDF with AES-256 password encryption", Icons.Rounded.Lock, ToolColors.Indigo, ToolCategory.SECURITY, isPro = true),
    UNLOCK("Unlock PDF", "Unlock", "Remove the password from a PDF you own", Icons.Rounded.LockOpen, ToolColors.Green, ToolCategory.SECURITY, isPro = true),
    ;

    companion object {
        val featured = listOf(IMAGE_TO_PDF, MERGE, SPLIT, COMPRESS, SIGN, SCAN, LOCK, OCR)
    }
}
