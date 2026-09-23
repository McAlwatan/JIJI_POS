package com.example.jijipos.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.jijipos.R;
import com.example.jijipos.SessionManager;
import com.example.jijipos.database.AppDatabase;
import com.example.jijipos.database.dao.TransactionDao;
import com.example.jijipos.database.entity.Business;
import com.example.jijipos.database.entity.User;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import com.google.android.material.button.MaterialButton;

/**
 * Business reports screen for the manager. Replaces the old stub with a real
 * PDF generator (Android's built-in PdfDocument, no extra dependency) that
 * summarises the current month: grand totals plus a per-cashier breakdown,
 * then hands the file to the system share sheet via FileProvider.
 */
public class ManagerReportsFragment extends Fragment {

    private MaterialButton btnExportPdf;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_reports, container, false);

        btnExportPdf = view.findViewById(R.id.btnExportPdf);
        btnExportPdf.setOnClickListener(v -> exportMonthlyReport());

        return view;
    }

    private void exportMonthlyReport() {
        if (btnExportPdf != null) {
            btnExportPdf.setEnabled(false);
            btnExportPdf.setText("Generating...");
        }

        final long businessId = SessionManager.getBusinessId(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            String failure = null;
            File pdfFile = null;
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());

                // Current month window: first day 00:00 -> now
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                long end = System.currentTimeMillis();

                String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.getTime());

                String businessName = "JIJI POS Business";
                Business business = businessId > 0 ? db.businessDao().getBusinessById(businessId) : null;
                if (business != null && business.getBusinessName() != null) {
                    businessName = business.getBusinessName();
                }

                List<User> cashiers = businessId > 0 ? db.userDao().getCashierForBusiness(businessId) : null;

                double grandTotal = 0.0;
                int grandCount = 0;
                java.util.List<String[]> rows = new java.util.ArrayList<>();
                if (cashiers != null) {
                    for (User c : cashiers) {
                        TransactionDao.SalesStats stats =
                                db.transactionDao().getCashierSalesStats(c.getId(), start, end);
                        double total = (stats != null && stats.totalSales != null) ? stats.totalSales : 0.0;
                        int count = (stats != null) ? stats.saleCount : 0;
                        grandTotal += total;
                        grandCount += count;
                        rows.add(new String[]{c.getFullName(), String.valueOf(count),
                                String.format(Locale.US, "%,.0f TZS", total)});
                    }
                }

                pdfFile = buildPdf(businessName, monthLabel, grandTotal, grandCount, rows);
            } catch (Exception e) {
                e.printStackTrace();
                failure = "Could not build the report";
            }

            final File finalPdf = pdfFile;
            final String finalFailure = failure;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (btnExportPdf != null) {
                        btnExportPdf.setEnabled(true);
                        btnExportPdf.setText("Export Monthly PDF Report");
                    }
                    if (finalFailure != null || finalPdf == null) {
                        Toast.makeText(getContext(),
                                finalFailure != null ? finalFailure : "No report generated",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        sharePdf(finalPdf);
                    }
                });
            }
        });
    }

    private File buildPdf(String businessName, String monthLabel, double grandTotal,
                          int grandCount, List<String[]> rows) throws Exception {
        int pageWidth = 595;   // A4 @ 72dpi
        int pageHeight = 842;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint title = new Paint();
        title.setColor(Color.parseColor("#0D0D0D"));
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(24);

        Paint subtitle = new Paint();
        subtitle.setColor(Color.parseColor("#555555"));
        subtitle.setTextSize(13);

        Paint accent = new Paint();
        accent.setColor(Color.parseColor("#1F8A6E"));
        accent.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        accent.setTextSize(13);

        Paint body = new Paint();
        body.setColor(Color.parseColor("#222222"));
        body.setTextSize(12);

        Paint header = new Paint();
        header.setColor(Color.parseColor("#1F8A6E"));
        header.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        header.setTextSize(12);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#DDDDDD"));
        line.setStrokeWidth(1);

        int margin = 48;
        float y = 70;

        canvas.drawText("JIJI POS", margin, y, title);
        y += 22;
        canvas.drawText(businessName, margin, y, subtitle);
        y += 18;
        canvas.drawText("Monthly sales report - " + monthLabel, margin, y, subtitle);
        y += 30;

        canvas.drawText("SUMMARY", margin, y, accent);
        y += 22;
        canvas.drawText("Total sales:  " + String.format(Locale.US, "%,.0f TZS", grandTotal), margin, y, body);
        y += 20;
        canvas.drawText("Transactions:  " + grandCount, margin, y, body);
        y += 20;
        canvas.drawText("Cashiers on record:  " + rows.size(), margin, y, body);
        y += 34;

        // Table header
        canvas.drawText("CASHIER", margin, y, header);
        canvas.drawText("SALES", margin + 300, y, header);
        canvas.drawText("TOTAL", margin + 400, y, header);
        y += 10;
        canvas.drawLine(margin, y, pageWidth - margin, y, line);
        y += 24;

        if (rows.isEmpty()) {
            canvas.drawText("No cashier activity recorded this month.", margin, y, body);
        } else {
            for (String[] row : rows) {
                if (y > pageHeight - 80) break; // simple single-page guard
                canvas.drawText(truncate(row[0], 34), margin, y, body);
                canvas.drawText(row[1], margin + 300, y, body);
                canvas.drawText(row[2], margin + 400, y, body);
                y += 12;
                canvas.drawLine(margin, y, pageWidth - margin, y, line);
                y += 24;
            }
        }

        // Footer
        String generated = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US)
                .format(Calendar.getInstance().getTime());
        canvas.drawText("Generated " + generated, margin, pageHeight - 48, subtitle);

        document.finishPage(page);

        File dir = new File(requireContext().getCacheDir(), "reports");
        if (!dir.exists()) dir.mkdirs();
        String fileName = "JIJI_Report_" + monthLabel.replace(" ", "_") + ".pdf";
        File file = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        }
        document.close();
        return file;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "\u2026";
    }

    private void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "JIJI POS monthly report");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share report"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(),
                    "Report saved but could not be shared", Toast.LENGTH_SHORT).show();
        }
    }
}
