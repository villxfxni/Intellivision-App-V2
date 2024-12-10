package com.example.directionstest.UI.Views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.directionstest.R;
import com.google.mlkit.vision.objects.DetectedObject;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal"
        , "FieldMayBeLocal"})
public class BoundingBox extends View {
    private Paint boxPaint;
    private Paint rectPaint;
    private Paint textPaint;
    private RectF boxRect;
    private RectF textRect;
    private Typeface poppins;
    private final Canvas canvas = new Canvas();

    private Size previewRes,inputRes;

    private String mLabel = "";
    private float labelSize = 20f;
    private int labelColor = ContextCompat.getColor(getContext(), R.color.bondi_blue);

    private List<DetectedObject> detectedObjects;

    public BoundingBox(Context context) {
        this(context, null);
        init();
    }

    public BoundingBox(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.boundingBoxStyle);
    }

    public BoundingBox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BoundingBox,
                defStyleAttr, 0
        );

        try{
            labelColor = a.getColor(R.styleable.BoundingBox_android_textColor, labelColor);
            labelSize = a.getDimension(R.styleable.BoundingBox_android_textSize, labelSize);
        }
        finally {
            a.recycle();
            init();
        }

    }

    private void init(){
        boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxRect = new RectF();
        textRect = new RectF();
//        screenHeight = getResources().getDisplayMetrics().heightPixels;

        boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.bondi_blue));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);

        rectPaint.setColor(0x80D2D2EB);
        rectPaint.setStyle(Paint.Style.FILL);

        poppins = Typeface.create("alexandria_regular", Typeface.NORMAL);

        textPaint.setColor(labelColor);
        textPaint.setTextSize(labelSize);
        textPaint.setTypeface(poppins);
    }

    public void setPreviewResolution(Size res){
        this.previewRes = res;
    }
    public Size getPreviewResolution() {
        return previewRes;
    }

    public void setInputResolution(Size res){
        this.inputRes = res;
    }

    public void setDetectedObjects(List<DetectedObject> detectedObjects) {
        this.detectedObjects = detectedObjects;
        invalidate();
    }

    private void spawnBoxes(Canvas canvas,RectF rect){
        canvas.drawRoundRect(rect, 24f, 24f, boxPaint);
    }

    private void updateLabel(Canvas canvas) {
        textRect.set(boxRect.left+6
                ,boxRect.top+6
                ,boxRect.right-6
                ,boxRect.top+labelSize+labelSize);
        canvas.drawRoundRect(textRect,16f,16f,rectPaint);
        canvas.drawText(mLabel, boxRect.left+4,boxRect.top+labelSize+8,textPaint);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if(visibility == View.GONE){
            Log.e("TAG", "onVisibilityChanged: ");
            this.draw(canvas);
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(detectedObjects != null) {
            for (DetectedObject objects : detectedObjects) {
//                Log.e("TAG", "onSuccess: tracking ID "+object.getTrackingId());
                spawnBoxes(canvas, mapBoxRect(objects.getBoundingBox()));

                mLabel = objects.getLabels()
                        .stream()
                        .map(DetectedObject.Label::getText)
                        .collect(Collectors.joining(", "));

//                Log.e("TAG", "onDraw: LABELS "+mLabel);
                updateLabel(canvas);
            }

        }

    }

    /**
     * Converts output rect from (360*640) to native resolution e.g(1080*2400)
     * @param boundingBox
     * @return
     */
    public RectF mapBoxRect(Rect boundingBox){
        float w;
        float h;
        if(inputRes !=null){
            h = previewRes.getHeight()/(float) inputRes.getWidth();
            w = previewRes.getWidth()/(float) inputRes.getHeight();
            boxRect.set(boundingBox.left * w,
                    boundingBox.top * h,
                    boundingBox.right * w,
                    boundingBox.bottom * h);
        }
        return boxRect;
    }

}