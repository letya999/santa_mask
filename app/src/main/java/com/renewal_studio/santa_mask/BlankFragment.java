package com.renewal_studio.santa_mask;

import android.graphics.Bitmap;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.renderscript.RenderScript;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;

import java.util.concurrent.Semaphore;

import hugo.weaving.DebugLog;
import io.github.silvaren.easyrs.tools.Nv21Image;

public class BlankFragment extends Fragment {
    RenderScript rs;
    CameraView cameraView;
    ImageView imageView;
    Bitmap bitmap;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;
    private final DetectedFace detectedFace = new DetectedFace();

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        detectedFace.initialize(getActivity().getApplicationContext(), inferenceHandler);
        cameraView.start();
        rs = RenderScript.create(getActivity().getApplicationContext());
        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] data, final int width, final int height, int rotationDegrees) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bitmap = Nv21Image.nv21ToBitmap(rs, data, width, height);
                        detectedFace.onImageAvailable(bitmap);
                        //imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    @DebugLog
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
        super.onPause();
    }

}
