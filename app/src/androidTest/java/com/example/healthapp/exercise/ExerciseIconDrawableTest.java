package com.example.healthapp.exercise;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.healthapp.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExerciseIconDrawableTest {
    @Test
    public void allExerciseVectorsInflateAndDrawVisiblePixels() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int[] icons = {
                R.drawable.exercise_icon_walking,
                R.drawable.exercise_icon_running,
                R.drawable.exercise_icon_aerobics,
                R.drawable.exercise_icon_elliptical,
                R.drawable.exercise_icon_swimming,
                R.drawable.exercise_icon_stairs,
                R.drawable.exercise_icon_jump_rope,
                R.drawable.exercise_icon_cycling,
                R.drawable.exercise_icon_badminton,
                R.drawable.exercise_icon_basketball,
                R.drawable.exercise_icon_soccer,
                R.drawable.exercise_icon_ping_pong,
                R.drawable.exercise_icon_tennis,
                R.drawable.exercise_icon_volleyball,
                R.drawable.exercise_icon_yoga,
                R.drawable.exercise_icon_pilates,
                R.drawable.exercise_icon_hiit,
                R.drawable.exercise_icon_strength
        };
        for (int icon : icons) {
            Drawable drawable = ContextCompat.getDrawable(context, icon);
            assertNotNull(drawable);
            Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, 96, 96);
            drawable.draw(new Canvas(bitmap));
            int opaquePixels = 0;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    if ((bitmap.getPixel(x, y) >>> 24) != 0) {
                        opaquePixels++;
                    }
                }
            }
            assertTrue("Icon " + icon + " rendered empty", opaquePixels > 0);
            assertTrue("Icon " + icon + " filled the whole canvas",
                    opaquePixels < bitmap.getWidth() * bitmap.getHeight());
            bitmap.recycle();
        }
    }
}
