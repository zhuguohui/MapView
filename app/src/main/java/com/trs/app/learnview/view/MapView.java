package com.trs.app.learnview.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import com.trs.app.learnview.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by zhuguohui
 * Date: 2021/12/28
 * Time: 10:56
 * Desc:
 */
public class MapView extends View {
    private List<ProvinceItem> list = new ArrayList<>();
    private Paint paint;
    private int vectorWidth = -1;
    private Matrix matrix = new Matrix();
    private Matrix invertMatrix = new Matrix();
    private float viewScale = -1f;
    private float userScale = 1.0f;
    private boolean initFinish = false;
    private int bgColor;
    private GestureDetector gestureDetector;
    private int offsetX, offsetY;
    private Scroller scroller;
    private float[] points;
    private float[] pointsFocusBefore;
    private float focusX,focusY;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean showDebugInfo=false;
    private static final int MAX_SCROLL=10000;
    private static final int MIN_SCROLL=-10000;

    public MapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgColor = Color.parseColor("#f5f5f5");
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GRAY);
        decodeThread.start();
        scroller = new Scroller(getContext());
        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        scaleGestureDetector = new ScaleGestureDetector(getContext(),scaleGestureListener);

    }

    private ScaleGestureDetector.OnScaleGestureListener  scaleGestureListener=new ScaleGestureDetector.OnScaleGestureListener() {

        float lastScaleFactor;
        boolean mapPoint=false;
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float[] points=new float[]{detector.getFocusX(),detector.getFocusY()};
            pointsFocusBefore=new float[]{detector.getFocusX(),detector.getFocusY()};
            if(mapPoint) {
                mapPoint=false;
                invertMatrix.mapPoints(points);
                focusX = points[0];
                focusY = points[1];
            }
            float change = scaleFactor - lastScaleFactor;
            lastScaleFactor=scaleFactor;
            userScale+=change;
            postInvalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            lastScaleFactor=1.0f;
            mapPoint=true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    private GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            boolean result = false;
            float x = event.getX();
            float y = event.getY();
            points = new float[]{x, y};
            invertMatrix.mapPoints(points);
            for (ProvinceItem item : list) {
                if (item.onTouch(points[0], points[1])) {
                    result = true;
                }
            }
            postInvalidate();
            return result;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            offsetX += -distanceX/userScale;
            offsetY += -distanceY/userScale;
            postInvalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            scroller.fling(offsetX, offsetY, (int) ((int) velocityX/userScale), (int) ((int) velocityY/userScale),MIN_SCROLL,
                 MAX_SCROLL,MIN_SCROLL,  MAX_SCROLL);
            postInvalidate();
            return true;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private Thread decodeThread = new Thread() {
        @Override
        public void run() {
            //Dom 解析 SVG文件

            InputStream inputStream = getContext().getResources().openRawResource(R.raw.ic_map);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            try {
                DocumentBuilder builder = factory.newDocumentBuilder();

                Document doc = builder.parse(inputStream);

                Element rootElement = doc.getDocumentElement();
                String strWidth = rootElement.getAttribute("android:width");
                vectorWidth = Integer.parseInt(strWidth.replace("dp", ""));
                NodeList items = rootElement.getElementsByTagName("path");

                for (int i = 1; i < items.getLength(); i++) {
                    Element element = (Element) items.item(i);
                    String pathData = element.getAttribute("android:pathData");
                    @SuppressLint("RestrictedApi")
                    Path path = PathParser.createPathFromPathData(pathData);
                    ProvinceItem item = new ProvinceItem(path, i);
                    list.add(item);
                }
                initFinish = true;
                postInvalidate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };



    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.getCurrX();
            offsetY = scroller.getCurrY();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (vectorWidth != -1 && viewScale == -1) {
            int width = getWidth();
            viewScale = width * 1.0f / vectorWidth;
        }
        if (viewScale != -1) {
            float scale=viewScale*userScale;
            matrix.reset();
            matrix.postTranslate(offsetX,offsetY);
            matrix.postScale(scale, scale,focusX,focusY);

            invertMatrix.reset();
            matrix.invert(invertMatrix);
        }
        canvas.setMatrix(matrix);
        canvas.drawColor(bgColor);
        if (initFinish) {
            for (ProvinceItem item : list) {
                item.onDraw(canvas, paint);
            }
        }

        showDebugInfo(canvas);
    }

    private void showDebugInfo(Canvas canvas){
        if(!showDebugInfo){
            return;
        }
        if (points != null) {
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(points[0], points[1], 20, paint);
        }
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(focusX,focusY,20,paint);


        if(pointsFocusBefore!=null) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pointsFocusBefore[0], pointsFocusBefore[1], 20, paint);
        }


    }
}



class ProvinceItem {
    Path path;
    private final Region region;
    private boolean isSelected = false;
    private final RectF rectF;
    private final int index;

    public boolean onTouch(float x, float y) {
        if (region.contains((int) x, (int) y)) {
            isSelected = true;
            return true;
        }
        isSelected = false;
        return false;
    }

    public ProvinceItem(Path path, int index) {
        this.path = path;
        rectF = new RectF();
        path.computeBounds(rectF, true);
        region = new Region();
        region.setPath(path, new Region(new Rect((int) rectF.left
                , (int) rectF.top, (int) rectF.right, (int) rectF.bottom)));
        this.index = index;
    }


    protected void onDraw(Canvas canvas, Paint paint) {
        paint.reset();
        paint.setColor(isSelected ? Color.YELLOW : Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawPath(path, paint);
        paint.setColor(Color.GRAY);
        paint.setColor(Color.BLUE);
        //  canvas.drawText(index+"",rectF.centerX(),rectF.centerY(),paint);

    }
}
