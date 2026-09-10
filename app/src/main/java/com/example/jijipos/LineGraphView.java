package com.example.jijipos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class LineGraphView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;

    // Default Mock Trend Data Points to perfectly replicate the curve wave layout matching your Selcom reference image
    private float[] dataPoints = new float[]{400000f, 650000f, 1300000f, 950000f, 450000f, 950000f, 850000f, 1400000f};
    private String[] axisXLabels = new String[]{"1", "2", "3", "4", "5", "6", "7"};

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 1. Neon Green Premium Spline Curve Vector Config
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#4CD964")); // Vibrant Neon Green matching Selcom Pesa
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // 2. Translucent Green Area Fill Gradient Config under the curve path
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // 3. Thin White/Gray Muted Axis Grid Lines Config
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#25FFFFFF")); // 15% opacity white for elegant dark mode contrast
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);

        // 4. Muted Translucent Label Typography Text Config
        labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#8E8E93")); // Clean system gray color label
        labelPaint.setTextSize(26f);
        labelPaint.setAntiAlias(true);
    }

    public void setData(float[] newDataPoints) {
        this.dataPoints = newDataPoints;
        invalidate(); // Force immediate canvas redrawing update loop
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints == null || dataPoints.length < 2) return;

        float width = getWidth();
        float height = getHeight();

        // Establish proper canvas grid margins room spacing definitions to fit side labels safely
        float paddingLeft = 100f;   // Room size footprint block for the Y-Axis Labels ("2M", "1M")
        float paddingRight = 40f;
        float paddingTop = 40f;
        float paddingBottom = 60f;  // Room size footprint block for the X-Axis Labels ("1", "2", "3")

        float graphWidth = width - paddingLeft - paddingRight;
        float graphHeight = height - paddingTop - paddingBottom;

        // Force maximum limit range boundary cap checks targeting 2M (2,000,000 TZS) matching reference metrics
        float maxVal = 2000000f;

        // --- DRAW Y-AXIS HORIZONTAL GRID LINES AND CONTEMPORARY LABELS ---
        String[] yLabels = new String[]{"2M", "1M", "750K", "500K", "TZS 0"};
        float[] yPositions = new float[]{0f, 0.5f, 0.625f, 0.75f, 1f}; // Percentages relative to the max scaling ceiling boundaries

        for (int i = 0; i < yLabels.length; i++) {
            float y = paddingTop + (graphHeight * yPositions[i]);

            // Draw horizontal cross grid guideline vector path
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);

            // Render text string descriptions alignment anchors tags
            canvas.drawText(yLabels[i], 15f, y + 8f, labelPaint);
        }

        // --- CALCULATION OF THE GLOWING GREEN TREND CURVE TRAJECTORY WAVE PATH ---
        float stepX = graphWidth / (dataPoints.length - 1);
        Path path = new Path();
        Path fillPath = new Path();

        for (int i = 0; i < dataPoints.length; i++) {
            float x = paddingLeft + (i * stepX);
            // Protect value overflows cap boundaries safely
            float boundedValue = Math.min(dataPoints[i], maxVal);
            float y = paddingTop + graphHeight - ((boundedValue / maxVal) * graphHeight);

            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, paddingTop + graphHeight);
                fillPath.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }

            if (i == dataPoints.length - 1) {
                fillPath.lineTo(x, paddingTop + graphHeight);
                fillPath.close();
            }

            // --- DRAW X-AXIS TIMELINE STEP MARKERS FOOTER LABELS ---
            if (i < axisXLabels.length) {
                canvas.drawText(axisXLabels[i], x - 10f, height - 15f, labelPaint);
            }
        }

        // Apply dark-mode fading green linear color background gradient under the curve path vector limits
        fillPaint.setShader(new LinearGradient(0, paddingTop, 0, paddingTop + graphHeight,
                Color.parseColor("#454CD964"), Color.TRANSPARENT, Shader.TileMode.CLAMP));

        // Commit drawing paths arrays layout directly to view canvas layer
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(path, linePaint);

        // Draw primary vertical base anchor frame layout boundary line
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, gridPaint);
    }
}
