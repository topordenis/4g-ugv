package com.topor.rover;

import android.content.Context;
import android.util.Log;

import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;

public class RoverCameraCapture {
    String _deviceName;
    CameraVideoCapturer _capturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    VideoSource videoSource;

    boolean _init = false;
    int _references;

    public SurfaceTextureHelper GetSurfaceTextureHelper(){
        return surfaceTextureHelper;
    }
    RoverCameraCapture(String deviceName, CameraEnumerator enumerator) {
        _deviceName = deviceName;
        _capturer = enumerator.createCapturer(deviceName, null);
        _references = 0;
    }
    void Initialize(PeerConnectionFactory peerConnectionFactory ,EglBase rootEglBase, Context ctx){
        videoSource = peerConnectionFactory.createVideoSource(_capturer.isScreencast());

        surfaceTextureHelper = SurfaceTextureHelper.create("VideoCapturerThread", rootEglBase.getEglBaseContext());
        _capturer.initialize(surfaceTextureHelper,
                (Context)ctx, videoSource.getCapturerObserver());


        _capturer.startCapture(800, 600, 30);

    }
}
