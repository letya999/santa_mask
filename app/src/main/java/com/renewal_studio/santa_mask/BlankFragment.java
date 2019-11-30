package com.renewal_studio.santa_mask;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.renderscript.RenderScript;
import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.CameraViewImpl;
import io.github.silvaren.easyrs.tools.Nv21Image;

public class BlankFragment extends Fragment {
    RenderScript rs;
    CameraView cameraView;

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraView.start();
        rs = RenderScript.create(getActivity().getApplicationContext());
        cameraView.setOnFrameListener(new CameraViewImpl.OnFrameListener() {
            @Override
            public void onFrame(final byte[] data, final int width, final int height, int rotationDegrees) {
                Bitmap bitmap = Nv21Image.nv21ToBitmap(rs, data, width, height);
            }
        });
    }

    @Override
    public void onPause() {
        cameraView.stop();
        super.onPause();
    }

}
