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

import java.util.Locale;

/**
 * Real-data trend graph — no baked-in mock arrays. Call setData(labels,
 * values) with whatever CustomerHomeFragment pulls from the database;
 * an empty call renders an empty-state message instead of a fake curve.
 *
 * Two things changed from the original version:
 *  1. The Y-axis ceiling used to be hardcoded to 2,000,000 TZS, so real
 *     spending (a few thousand TZS) rendered as a flat line pinned to the
 *     bottom. It's now computed from the actual data via niceCeiling().
 *  2. The grid/label colors were tuned for a dark card (#25FFFFFF white
 *     grid lines, light gray text) but this view now sits inside a WHITE
 *     card (fv_light_surface) — those were nearly invisible there, so
 *     they're now dark-on-light. The line/fill accent is also switched
 *     from neon green to the app's blue accent family for consistency
 *     with the rest of the fairvest-styled screens.
 */
public class LineGraphView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private Paint emptyStatePaint;

    private float[] dataPoints = new float[0];
    private String[] axisXLabels = new String[0];

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2F6FED"));
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#25FFFFFF")); // 15% white — reads on any dark card
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);

        labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#8E8E93")); // system gray, legible on dark
        labelPaint.setTextSize(26f);
        labelPaint.setAntiAlias(true);

        emptyStatePaint = new Paint();
        emptyStatePaint.setColor(Color.parseColor("#9B9B9B"));
        emptyStatePaint.setTextSize(28f);
        emptyStatePaint.setAntiAlias(true);
        emptyStatePaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * @param labels one entry per data point — pass "" for points you
     *               don't want printed (e.g. label every 3rd hour on a
     *               24-point day so the axis doesn't clash)
     * @param values real amounts, same length as labels
     */
    public void setData(String[] labels, float[] values) {
        this.axisXLabels = (labels != null) ? labels : new String[0];
        this.dataPoints = (values != null) ? values : new float[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (dataPoints.length < 2) {
            canvas.drawText("No spending yet this period", width / 2f, height / 2f, emptyStatePaint);
            return;
        }

        float paddingLeft = 100f;
        float paddingRight = 40f;
        float paddingTop = 40f;
        float paddingBottom = 60f;

        float graphWidth = width - paddingLeft - paddingRight;
        float graphHeight = height - paddingTop - paddingBottom;

        float rawMax = 0f;
        for (float v : dataPoints) rawMax = Math.max(rawMax, v);
        float maxVal = niceCeiling(rawMax);

        String[] yLabels = new String[]{
                formatAxisValue(maxVal),
                formatAxisValue(maxVal * 0.75f),
                formatAxisValue(maxVal * 0.5f),
                formatAxisValue(maxVal * 0.25f),
                "TZS 0"
        };
        float[] yPositions = new float[]{0f, 0.25f, 0.5f, 0.75f, 1f};

        for (int i = 0; i < yLabels.length; i++) {
            float y = paddingTop + (graphHeight * yPositions[i]);
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
            canvas.drawText(yLabels[i], 15f, y + 8f, labelPaint);
        }

        float stepX = graphWidth / (dataPoints.length - 1);
        Path path = new Path();
        Path fillPath = new Path();

        for (int i = 0; i < dataPoints.length; i++) {
            float x = paddingLeft + (i * stepX);
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

            if (i < axisXLabels.length && axisXLabels[i] != null && !axisXLabels[i].isEmpty()) {
                canvas.drawText(axisXLabels[i], x - 10f, height - 15f, labelPaint);
            }
        }

        fillPaint.setShader(new LinearGradient(0, paddingTop, 0, paddingTop + graphHeight,
                Color.parseColor("#402F6FED"), Color.TRANSPARENT, Shader.TileMode.CLAMP));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(path, linePaint);
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, gridPaint);
    }

    /** Rounds up to a "nice" axis ceiling (1 / 2 / 5 x 10^n) instead of the raw max. */
    private float niceCeiling(float value) {
        if (value <= 0f) return 1000f; // sensible floor so a near-empty graph still has a readable axis
        double exponent = Math.floor(Math.log10(value));
        double magnitude = Math.pow(10, exponent);
        double normalized = value / magnitude;

        double niceNormalized;
        if (normalized <= 1) niceNormalized = 1;
        else if (normalized <= 2) niceNormalized = 2;
        else if (normalized <= 5) niceNormalized = 5;
        else niceNormalized = 10;

        return (float) (niceNormalized * magnitude);
    }

    private String formatAxisValue(float value) {
        if (value >= 1_000_000f) return String.format(Locale.US, "%.1fM", value / 1_000_000f);
        if (value >= 1_000f) return String.format(Locale.US, "%.0fK", value / 1_000f);
        return String.format(Locale.US, "%.0f", value);
    }
}