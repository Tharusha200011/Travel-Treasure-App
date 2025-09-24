package com.example.traveltreasure;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;

import android.graphics.Paint;


public class SummaryActivity extends AppCompatActivity {

    private TableLayout tableLayoutSummary;
    private DatabaseReference expensesRef, membersRef;
    private String itemName;
    private Map<String, Double> memberContributions = new HashMap<>();
    private List<String> memberNames = new ArrayList<>();
    private List<String> memberEmails = new ArrayList<>();
    private double totalExpense = 0;
    private double perPersonCost = 0;
    private PieChart pieChart;
    private LinearLayout btnDownloadPDF;
    private LinearLayout btnSharePdf;
    FirebaseUser user;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        tableLayoutSummary = findViewById(R.id.tableLayoutSummary);
        pieChart = findViewById(R.id.pieChart);
        btnDownloadPDF = findViewById(R.id.btnDownloadPDF);
        btnSharePdf = findViewById(R.id.btnSharePdf);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        itemName = getIntent().getStringExtra("itemName");

        // Initialize Firebase references
        expensesRef =  FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("expenses");
        membersRef =  FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("members");
        loadMembersAndExpenses();


        btnDownloadPDF.setOnClickListener(v -> generatePdf());
        btnSharePdf.setOnClickListener(v -> sharePdf());

    }

    private void sharePdf() {
        File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TripSummaries/TripSummary_" + itemName + ".pdf");

        if (pdfFile.exists()) {
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Trip Summary");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the summary of our trip.");

            // Add recipients
            String[] emails = memberEmails.toArray(new String[0]);
            shareIntent.putExtra(Intent.EXTRA_EMAIL, emails);

            // Grant read permission to the recipient app
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF"));
        } else {
            Toast.makeText(this, "Firstly download PDF file", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePdf() {
        // Create a new document
        PdfDocument document = new PdfDocument();

        // Create a page description
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 1000, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);

        // Get the canvas to draw
        Canvas canvas = page.getCanvas();

        // Setup paint for the title
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(30); // Increased font size for title
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); // Make the text bold

        // Draw title in the center
        String title = "Trip Summary of "+itemName;
        float titleWidth = titlePaint.measureText(title); // Measure the width of the title text
        float xPos = (canvas.getWidth() - titleWidth) / 2; // Calculate X position to center the text
        canvas.drawText(title, xPos, 40, titlePaint); // Draw text centered

        // Setup paint for regular text
        Paint textPaint = new Paint();
        textPaint.setTextSize(16); // Regular font size for table or smaller text
        textPaint.setColor(Color.BLACK);

        // Draw the table layout from UI
        drawTable(canvas, textPaint);

        // Draw the pie chart (if needed, could convert to bitmap and draw)
        drawPieChart(canvas);

        // Finish the page
        document.finishPage(page);

        // Write the document content to a file
        savePdf(document);

        // Close the document
        document.close();
    }

    private void drawTable(Canvas canvas, Paint paint) {
        int x = 50;
        int y = 100;

        for (int i = 0; i < tableLayoutSummary.getChildCount(); i++) {
            View row = tableLayoutSummary.getChildAt(i);
            if (row instanceof TableRow) {
                TableRow tableRow = (TableRow) row;

                // Get each cell in the row and draw it in the canvas
                for (int j = 0; j < tableRow.getChildCount(); j++) {
                    TextView textView = (TextView) tableRow.getChildAt(j);
                    canvas.drawText(textView.getText().toString(), x + j * 100, y, paint);
                }
                y += 30; // Move down for the next row
            }
        }
    }

    private void savePdf(PdfDocument document) {
        // Check if external storage is available
        if (isExternalStorageWritable()) {
            // Create or reference a common folder for trip summaries
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TripSummaries");
            if (!directory.exists()) {
                directory.mkdirs(); // Create the directory if it doesn't exist
            }

            // Name the file after the trip (e.g., "TripSummary_TripName.pdf")
            String fileName = "TripSummary_" + itemName + ".pdf";
            File file = new File(directory, fileName);

            try {
                // Write the PDF document to the specified file
                document.writeTo(new FileOutputStream(file));
                Toast.makeText(this, "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state); // Check if external storage is writable
    }


    private void drawPieChart(Canvas canvas) {
        // Enable drawing cache and capture the pie chart as a bitmap
        pieChart.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(pieChart.getDrawingCache());
        pieChart.setDrawingCacheEnabled(false);

        // Calculate the size of the PDF page and determine the max dimensions for the chart
        int pageWidth = canvas.getWidth();
        int pageHeight = canvas.getHeight();

        // Define the area on the PDF for the pie chart (leaving margins)
        int desiredWidth = pageWidth - 80; // Leave some margin on the sides
        int desiredHeight = 500; // Adjust height as needed

        // Calculate scale based on desired width and height while maintaining aspect ratio
        float scaleFactor = Math.min(
                (float) desiredWidth / bitmap.getWidth(),
                (float) desiredHeight / bitmap.getHeight()
        );

        // Calculate the scaled width and height
        int scaledWidth = (int) (bitmap.getWidth() * scaleFactor);
        int scaledHeight = (int) (bitmap.getHeight() * scaleFactor);

        // Scale the bitmap to fit the desired size
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);

        // Draw the scaled bitmap on the canvas at a specified position
        int xPos = (pageWidth - scaledWidth) / 2; // Center horizontally
        int yPos = 300; // Adjust the Y position to fit under the title and table

        canvas.drawBitmap(scaledBitmap, xPos, yPos, null);
    }

    private void loadMembersAndExpenses() {
        // Load members first
        membersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String memberName = snapshot.child("name").getValue(String.class);
                    String memberEmail = snapshot.child("email").getValue(String.class);
                    if (memberName != null) {
                        memberNames.add(memberName);
                        memberContributions.put(memberName, 0.0);
                        if (memberEmail != null) {
                            memberEmails.add(memberEmail);
                        }
                    }
                }

                // Now load expenses
                loadExpenses();
            }
        });
    }

    private void loadExpenses() {
        expensesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String payer = snapshot.child("payer").getValue(String.class);

                    // Handle the amount properly
                    Object amountObj = snapshot.child("amount").getValue();
                    double amount = 0;

                    // Check if the amount is a number or string and convert it properly
                    if (amountObj instanceof Double) {
                        amount = (double) amountObj;
                    } else if (amountObj instanceof Long) {
                        amount = ((Long) amountObj).doubleValue();
                    } else if (amountObj instanceof String) {
                        try {
                            amount = Double.parseDouble((String) amountObj);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            Toast.makeText(SummaryActivity.this, "Invalid amount format in Firebase", Toast.LENGTH_SHORT).show();
                        }
                    }

                    totalExpense += amount;
                    if (payer != null && memberContributions.containsKey(payer)) {
                        memberContributions.put(payer, memberContributions.get(payer) + amount);
                    }
                }

                // Calculate per-person cost
                perPersonCost = totalExpense / memberNames.size();

                // Display summary and pie chart
                displaySummary();
                displayPieChart();
            } else {
                Toast.makeText(SummaryActivity.this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displaySummary() {
        // Display table rows
        for (String memberName : memberNames) {
            double contributed = memberContributions.get(memberName);
            double toBeReceived = contributed - perPersonCost;
            double toBeContributed = perPersonCost - contributed;

            TableRow row = new TableRow(this);

            TextView nameView = new TextView(this);
            nameView.setText(memberName);

            TextView contributedView = new TextView(this);
            contributedView.setText(String.format("Rs%.2f", perPersonCost));

            TextView spentView = new TextView(this);
            spentView.setText(String.format("Rs%.2f", contributed));

            TextView receivedView = new TextView(this);
            receivedView.setText(String.format("Rs%.2f", Math.max(0, toBeReceived)));

            TextView contributedMoreView = new TextView(this);
            contributedMoreView.setText(String.format("Rs%.2f", Math.max(0, toBeContributed)));

            row.addView(nameView);
            row.addView(contributedView);
            row.addView(spentView);
            row.addView(receivedView);
            row.addView(contributedMoreView);

            tableLayoutSummary.addView(row);
        }
    }

    private void displayPieChart() {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();

        // Add each member's contribution to the pie chart
        for (Map.Entry<String, Double> entry : memberContributions.entrySet()) {
            String memberName = entry.getKey();
            double contribution = entry.getValue();
            pieEntries.add(new PieEntry((float) contribution, memberName));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "Member Contributions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS); // Use a predefined color template
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(16f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false); // Disable description label
        pieChart.setHoleRadius(40f); // Set hole radius for the doughnut style
        pieChart.setTransparentCircleRadius(45f);

        pieChart.animateY(1000); // Animation for the chart

        pieChart.invalidate(); // Refresh the chart
    }
}
