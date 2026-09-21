package com.example.tca_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {
    private Paint paint;
    private Paint textPaint;
    private Paint borderPaint;
    private RectF rectF;
    private float[] data = new float[0];
    private int[] colors = new int[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN};

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(2f);
        
        rectF = new RectF();
    }

    public void setData(float[] data) {
        this.data = data;
        invalidate(); // Redraw the view
    }
    
    public void setColors(int[] colors) {
        this.colors = colors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (data == null || data.length == 0) return;

        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2f;
        
        // Define bounds for pie chart
        rectF.set(width/2 - radius, height/2 - radius, width/2 + radius, height/2 + radius);
        
        float total = 0;
        for (float val : data) {
            total += val;
        }
        
        float[] drawData = data;
        int[] drawColors = colors;
        
        // If no data, draw a single gray circle indicating 0 posts
        if (total == 0) {
            drawData = new float[]{100f};
            drawColors = new int[]{android.graphics.Color.parseColor("#E0E0E0")}; // Light Gray
            total = 100f;
        }
        
        float startAngle = 90f; // Adjusted to match the image which starts around bottom-right for blue
        
        // Draw slices and texts
        for (int i = 0; i < drawData.length; i++) {
            if (drawData[i] <= 0) continue;

            float sweepAngle = (drawData[i] / total) * 360f;
            
            // Draw slice with exact category color
            paint.setColor(drawColors[i % drawColors.length]);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            
            // Draw outline
            canvas.drawArc(rectF, startAngle, sweepAngle, true, borderPaint);
            
            // Draw percentage text
            float angle = startAngle + (sweepAngle / 2);
            float textRadius = radius * 0.65f;
            float x = (float) (width/2 + textRadius * Math.cos(Math.toRadians(angle)));
            float y = (float) (height/2 + textRadius * Math.sin(Math.toRadians(angle)));
            
            textPaint.setTextSize(radius * 0.25f);
            int percentage = Math.round((drawData[i] / total) * 100);
            if (percentage > 0) {
                canvas.drawText(percentage + "%", x, y + (textPaint.getTextSize() / 3), textPaint);
            }
            
            startAngle += sweepAngle;
        }
    }
}
