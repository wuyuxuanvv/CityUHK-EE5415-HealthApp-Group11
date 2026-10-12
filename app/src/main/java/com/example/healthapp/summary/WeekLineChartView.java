package com.example.healthapp.summary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.healthapp.R;
import java.util.Locale;

/** Seven-day chart. Future dates have labels but no plotted observations. */
public class WeekLineChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] first = new float[7];
    private final float[] second = new float[7];
    private final String[] ticks = new String[5];
    private final String[] days = {"一", "二", "三", "四", "五", "六", "日"};
    private final int black, blue, red, grey;
    private int elapsedDays;
    private boolean calories;
    private float maximum = 1;

    public WeekLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        black = ContextCompat.getColor(context, R.color.health_outline);
        blue = ContextCompat.getColor(context, R.color.health_accent_blue);
        red = ContextCompat.getColor(context, R.color.health_accent_red);
        grey = ContextCompat.getColor(context, R.color.health_grey_light);
        setData(new float[7], new float[7], 0, false);
    }

    void setData(float[] values, float[] other, int elapsed, boolean kcal) {
        System.arraycopy(values, 0, first, 0, 7);
        System.arraycopy(other, 0, second, 0, 7);
        elapsedDays = Math.max(0, Math.min(7, elapsed));
        calories = kcal;
        float max = 0;
        for (int i = 0; i < elapsedDays; i++) {
            max = Math.max(max, first[i]);
            if (calories) max = Math.max(max, second[i]);
        }
        maximum = Math.max(4, (float) Math.ceil(max / 4) * 4);
        for (int i = 0; i < 5; i++) {
            ticks[i] = String.format(Locale.getDefault(), "%.0f", maximum * i / 4);
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 58 * d, right = getWidth() - 12 * d;
        float top = 24 * d, bottom = getHeight() - 30 * d;
        if (right <= left || bottom <= top) return;
        paint.setTextSize(11 * d);
        paint.setStrokeWidth(d);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 5; i++) {
            float y = bottom - (bottom - top) * i / 4;
            paint.setColor(grey);
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(black);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(ticks[i], left - 6 * d, y + 4 * d, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(calories ? "千卡" : "分钟", 0, 14 * d, paint);
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            paint.setColor(i < elapsedDays ? black : grey);
            canvas.drawText(days[i], left + (right - left) * i / 6,
                    bottom + 20 * d, paint);
        }
        drawSeries(canvas, first, calories ? red : blue, left, right, top, bottom, d);
        if (calories) drawSeries(canvas, second, blue, left, right, top, bottom, d);
    }

    private void drawSeries(Canvas canvas, float[] values, int color, float left,
                            float right, float top, float bottom, float d) {
        paint.setColor(color);
        paint.setStrokeWidth(3 * d);
        for (int i = 0; i < elapsedDays; i++) {
            float x = left + (right - left) * i / 6;
            float y = bottom - (bottom - top) * values[i] / maximum;
            if (i > 0) {
                float previousX = left + (right - left) * (i - 1) / 6;
                float previousY = bottom - (bottom - top) * values[i - 1] / maximum;
                canvas.drawLine(previousX, previousY, x, y, paint);
            }
            canvas.drawCircle(x, y, 4 * d, paint);
        }
    }
}
