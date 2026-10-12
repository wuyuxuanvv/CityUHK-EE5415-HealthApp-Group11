package com.example.healthapp.home.pet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.healthapp.R;

/** Preloaded sprites and nearest-neighbour drawing; no allocations in onDraw. */
public final class PetView extends View {
    private final Paint paint = new Paint();
    private final RectF catRect = new RectF();
    private final RectF bowlRect = new RectF();
    private final RectF fishRect = new RectF();
    private final Bitmap[][] frames;
    private final Bitmap bowl;
    private final Bitmap fish;
    private int pose;
    private int frame;
    private float position;
    private float fishProgress = -1;

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        frames = new Bitmap[][] {
                load(R.drawable.cat_home_idle_1, R.drawable.cat_home_idle_2,
                        R.drawable.cat_home_idle_3),
                load(R.drawable.cat_home_walk_1, R.drawable.cat_home_walk_2,
                        R.drawable.cat_home_walk_3, R.drawable.cat_home_walk_4),
                load(R.drawable.cat_home_walk_left_1, R.drawable.cat_home_walk_left_2,
                        R.drawable.cat_home_walk_left_3, R.drawable.cat_home_walk_left_4),
                load(R.drawable.cat_home_eat_1, R.drawable.cat_home_eat_2,
                        R.drawable.cat_home_eat_3),
                load(R.drawable.cat_home_happy_1, R.drawable.cat_home_happy_2,
                        R.drawable.cat_home_happy_3)
        };
        bowl = load(R.drawable.cat_home_bowl)[0];
        fish = load(R.drawable.cat_home_fish)[0];
    }

    private Bitmap[] load(int... ids) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap[] result = new Bitmap[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = BitmapFactory.decodeResource(getResources(), ids[i], options);
        }
        return result;
    }

    void show(int pose, int frame, float position, float fishProgress) {
        this.pose = pose;
        this.frame = frame;
        this.position = position;
        this.fishProgress = fishProgress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Reserve separate horizontal areas for the bowl and the entire walking sprite.
        // Clamp the sprite rectangle, rather than its centre, so even the tail stays clear.
        // Larger cat, while retaining roughly 32% width for visible left/right travel.
        float size = Math.min(getHeight() * 0.9f, getWidth() * 0.4f);
        float gap = getWidth() * 0.04f;
        float top = (getHeight() - size) * 0.5f;
        float prop = size * 0.4f;
        float bowlX = gap + prop / 2;
        bowlRect.set(bowlX - prop / 2, top + size * 0.7f,
                bowlX + prop / 2, top + size);
        float minCenter = bowlRect.right + gap + size / 2;
        float maxCenter = getWidth() - gap - size / 2;
        float fraction = (Math.max(-1f, Math.min(1f, position)) + 1f) / 2f;
        float center = minCenter + (maxCenter - minCenter) * fraction;
        if (pose == 3 || fishProgress >= 0) {
            // The eating sprite includes a bowl on its left; align it with the fixed bowl.
            center = bowlX + size * 0.23f;
        }
        catRect.set(center - size / 2, top, center + size / 2, top + size);
        if (pose != 3) { // Eating frames already contain the brown bowl.
            canvas.drawBitmap(bowl, null, bowlRect, paint);
        }
        canvas.drawBitmap(frames[pose][frame], null, catRect, paint);
        if (fishProgress >= 0) {
            float fishSize = size * 0.2f;
            float x = getWidth() * 0.9f * (1 - fishProgress) + center * fishProgress;
            float y = top + size * (0.15f + 0.4f * fishProgress);
            fishRect.set(x - fishSize / 2, y, x + fishSize / 2, y + fishSize);
            canvas.drawBitmap(fish, null, fishRect, paint);
        }
    }
}
