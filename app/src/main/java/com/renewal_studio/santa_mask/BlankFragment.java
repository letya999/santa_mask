package com.renewal_studio.santa_mask;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.renderscript.RenderScript;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;
import java.io.File;
import java.util.List;
import io.github.silvaren.easyrs.tools.Nv21Image;

public class BlankFragment extends Fragment {
    RenderScript rs;
    CameraView cameraView;
    public static ImageView imageView;
    Bitmap bitmap;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;
    private final DetectedFace detectedFace = new DetectedFace();
    CameraOverlay cameraOverlay;

    public BlankFragment() {}

    public static BlankFragment newInstance() {
        return new BlankFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank, container, false);
        cameraView = view.findViewById(R.id.camera_view);
        imageView = view.findViewById(R.id.imageView);
        cameraOverlay = view.findViewById(R.id.cameraOverlay);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        detectedFace.initialize(getActivity().getApplicationContext(), inferenceHandler, cameraOverlay);
        cameraView.start();
        cameraOverlay.setPreview(cameraView);
        rs = RenderScript.create(getActivity().getApplicationContext());
        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] data, final int width, final int height, int rotationDegrees) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bitmap = Nv21Image.nv21ToBitmap(rs, data, width, height);
                        detectedFace.onImageAvailable(bitmap);
                    }
                });
            }
        });
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());
    }

    @Override
    public void onPause() {
        cameraView.stop();
        detectedFace.deInitialize();
        super.onPause();
    }

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
        private Paint mFaceLandmardkPaint;
        private int mframeNum = 0;
        private CameraOverlay overlay;

        public void initialize(
                final Context context,
                final Handler handler, final CameraOverlay overlay) {
            this.mContext = context;
            this.mInferenceHandler = handler;
            this.overlay = overlay;
            mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
            mFaceLandmardkPaint = new Paint();
            mFaceLandmardkPaint.setColor(Color.GREEN);
            mFaceLandmardkPaint.setStrokeWidth(2);
            mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
        }

        public void deInitialize() {
            synchronized (BlankFragment.DetectedFace.this) {
                if (mFaceDet != null)
                    mFaceDet.release();
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
            mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
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
                                synchronized (BlankFragment.DetectedFace.this) {
                                    results = mFaceDet.detect(mResizedBitmap);
                                }
                            }
                            if (results.size() != 0) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        overlay.setFaceResults(results);
                                        overlay.invalidate();
                                    }
                                });
                            }
                            mframeNum++;
                            mIsComputing = false;
                        }
                    });
            Trace.endSection();
        }
    }

}
