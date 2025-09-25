package com.example.gwallet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GenPdf {

    /**
     * Generates a PDF file from the provided JSON string and saves it to the app's internal storage.
     * The PDF contains a title and the formatted JSON content.
     *
     * @param json The JSON string to convert to PDF.
     * @param transactionId The transaction ID to name the PDF file.
     * @param context The application context for file storage.
     * @return The absolute path to the generated PDF file, or null if generation fails.
     */
    public static String generatePdfFromJson(String json, long transactionId, Context context) {
        File pdfDir = new File(context.getFilesDir(), "pdfs");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }
        File pdfFile = new File(pdfDir, "transaction_" + transactionId + ".pdf");

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Add title
            document.add(new Paragraph("Transaction Details - ID: " + transactionId));
            document.add(new Paragraph(" ")); // Empty line

            // Add JSON content
            document.add(new Paragraph(json));

            document.close();
            return pdfFile.getAbsolutePath();
        } catch (Exception e) {
            // In a real app, log the error: Log.e("GenPdf", "Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes the PDF file associated with the given transaction ID.
     *
     * @param transactionId The transaction ID.
     * @param context The application context.
     * @return true if deletion was successful, false otherwise.
     */
    public static boolean deletePdfForTransaction(long transactionId, Context context) {
        File pdfDir = new File(context.getFilesDir(), "pdfs");
        File pdfFile = new File(pdfDir, "transaction_" + transactionId + ".pdf");
        if (pdfFile.exists()) {
            return pdfFile.delete();
        }
        return false;
    }
}