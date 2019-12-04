package com.renewal_studio.santa_mask;

import android.graphics.Bitmap;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Trace;
import android.view.Display;
import android.view.WindowManager;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DetectedFace {
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    //324, 648, 972, 1296, 224, 448, 672, 976, 1344
    private static final int INPUT_SIZE = 976;
    private static final String TAG = "OnGetImageListener";
    private int mScreenRotation = 90;
    private List<VisionDetRet> results;
    private Bitmap mCroppedBitmap = null;
    private Bitmap mResizedBitmap = null;
    private Bitmap mInversedBipmap = null;
    private boolean mIsComputing = false;
    private Handler mInferenceHandler;
    private Context mContext;
    private FaceDet mFaceDet;
    private FloatingCameraWindow mWindow;
    private Paint mFaceLandmardkPaint;
    private int mframeNum = 0;

    public void initialize(
            final Context context,
            final Handler handler) {
        this.mContext = context;
        this.mInferenceHandler = handler;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        mWindow = new FloatingCameraWindow(mContext);
        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }

    public void deInitialize() {
        synchronized (DetectedFace.this) {
            if (mFaceDet != null)
                mFaceDet.release();
            if (mWindow != null)
                mWindow.release();
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = -90;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }
        final float minDim = Math.min(src.getWidth(), src.getHeight());
        final Matrix matrix = new Matrix();
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);
        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }
        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    public Bitmap imageSideInversion(Bitmap src){
        Matrix sideInversion = new Matrix();
        sideInversion.setScale(-1, 1);
        Bitmap inversedImage = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), sideInversion, false);
        return inversedImage;
    }

    public void onImageAvailable(Bitmap mRGBframeBitmap) {
        mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);
        drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);
        mInversedBipmap = imageSideInversion(mCroppedBitmap);
        mResizedBitmap = Bitmap.createScaledBitmap(mInversedBipmap, (int)(INPUT_SIZE/4.5), (int)(INPUT_SIZE/4.5), true);
        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
                        }
                        if(mframeNum % 3 == 0) {
                            synchronized (DetectedFace.this) {
                                results = mFaceDet.detect(mResizedBitmap);
                            }
                        }
                        if (results.size() != 0) {
                            for (final VisionDetRet ret : results) {
                                float resizeRatio = 4.5f;
                                Canvas canvas = new Canvas(mInversedBipmap);
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                for (Point point : landmarks) {
                                    int pointX = (int) (point.x * resizeRatio);
                                    int pointY = (int) (point.y * resizeRatio);
                                    canvas.drawCircle(pointX, pointY, 4, mFaceLandmardkPaint);
                                }
                            }
                        }
                        mframeNum++;
                        mWindow.setRGBBitmap(mInversedBipmap);
                        mIsComputing = false;
                    }
                });
        Trace.endSection();
    }
}
