package com.example.healthapp.summary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 本周运动柱状图（新粗野主义风格）：7 根直角柱 + 周一到周日标签。
 * 蓝色 = 达到每日运动目标；黄色 = 有运动未达标；浅灰 = 无记录。onDraw 不分配新对象。
 */
public class WeekBarChartView extends View {

    private static final int DAY_COUNT = 7;
    private static final String[] DAY_LABELS = {"一", "二", "三", "四", "五", "六", "日"};

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float[] minutes = new float[DAY_COUNT];
    private float dailyGoalMinutes = 30;
    private float maxMinutes = 60;

    public WeekBarChartView(Context context) {
        super(context);
        init();
    }

    public WeekBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(3f);
        goalPaint.setColor(Color.BLACK);
    }

    /** values 必须为 7 项（周一到周日分钟数）；goal 为每日运动目标分钟。 */
    public void setData(float[] values, float goal) {
        if (values == null || values.length != DAY_COUNT) {
            return;
        }
        System.arraycopy(values, 0, minutes, 0, DAY_COUNT);
        dailyGoalMinutes = Math.max(1, goal);
        float max = dailyGoalMinutes;
        for (float value : minutes) {
            max = Math.max(max, value);
        }
        maxMinutes = max * 1.15f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float labelSize = 12 * density;
        labelPaint.setTextSize(labelSize);

        float labelSpace = labelSize + 10 * density;
        float chartHeight = height - labelSpace;
        float slot = width / (float) DAY_COUNT;
        float barWidth = slot * 0.5f;

        // 目标参考线（黑色虚线感：画细线 + 短刻度即可，保持简单粗线）
        float goalY = height - labelSpace - chartHeight * (dailyGoalMinutes / maxMinutes);
        canvas.drawLine(0, goalY, width, goalY, goalPaint);

        for (int i = 0; i < DAY_COUNT; i++) {
            float centerX = slot * i + slot / 2f;
            float value = minutes[i];
            float barHeight = chartHeight * (value / maxMinutes);
            float left = centerX - barWidth / 2f;
            float top = height - labelSpace - barHeight;
            float bottom = height - labelSpace;

            if (value <= 0) {
                barPaint.setColor(0xFFE8E8E8);
            } else if (value >= dailyGoalMinutes) {
                barPaint.setColor(0xFF0000CC);
            } else {
                barPaint.setColor(0xFFFFCC00);
            }
            canvas.drawRect(left, top, left + barWidth, bottom, barPaint);
            canvas.drawRect(left, top, left + barWidth, bottom, goalPaint);

            canvas.drawText(DAY_LABELS[i], centerX,
                    height - 4 * density, labelPaint);
        }
    }
}
