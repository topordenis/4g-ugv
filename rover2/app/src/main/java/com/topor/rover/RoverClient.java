package com.topor.rover;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

import org.webrtc.AudioSource;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import okhttp3.WebSocket;

public class RoverClient implements SdpObserver, PeerConnection.Observer, DataChannel.Observer  {
    private final PeerConnection pc;
    private final Rover _rover;

    private String _uuid;

    private final MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMS;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    public String GetUUID() {
        return _uuid;
    }
    public PeerConnection GetPeer() {
        return pc;
    }
    RoverClient(Rover rover, String uuid) {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        rtcConfig.iceServers.add(PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer());

        pc = rover.GetPeerFactory().createPeerConnection(rtcConfig, this);
        _rover = rover;
        _uuid = uuid;


        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));


        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1080)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(2400)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(30)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));

        setCamera("0");

    }
    private final static String TAG = PeerConnectionClient.class.getCanonicalName();
    private VideoCapturer createCapturer(CameraEnumerator enumerator, boolean frontFacing) {
        final String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName) == frontFacing) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    private VideoCapturer getVideoCapturer() {
        VideoCapturer videoCapturer;
        CameraEnumerator enumerator;

        if (Camera2Enumerator.isSupported((Context) _rover.GetActivity())) {
            enumerator = new Camera2Enumerator((Context)  _rover.GetActivity());
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        // Switch the camera based on the current state
        videoCapturer = createCapturer(enumerator, false);

        // Toggle the camera for the next switch
//        useFrontCamera = !useFrontCamera;

        return videoCapturer;
    }
    public void setCamera(String deviceName) {
        localMS = _rover.GetPeerFactory().createLocalMediaStream("LOCAL_MS");
        videoCapturer = getVideoCapturer();
        videoSource = _rover.GetPeerFactory().createVideoSource(false);

     //   SurfaceTexture surfaceTexture = new SurfaceTexture("someid");
   //     Surface surface = new Surface(surfaceTexture);


        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread",  null);
        videoCapturer.initialize(surfaceTextureHelper,
                (Context) _rover.GetActivity(), videoSource.getCapturerObserver());


        videoCapturer.startCapture(800, 600, 30);




        Utils.ScreenDimensions dimensions = Utils.getScreenDimentions((Context) _rover.GetActivity());
        int fps = Utils.getFps((Context) _rover.GetActivity());

        Log.d(TAG, "FPSSSS: " + fps + " " + dimensions.screenWidth + " " + dimensions.screenHeight);

        var track = _rover.GetPeerFactory().createVideoTrack("LOCAL_MS_VS", videoSource);

        var sink = new VideoSink(){
            @Override
            public void onFrame(VideoFrame videoFrame) {

            }
        };

        track.addSink(sink);
        localMS.addTrack(track);

        pc.addStream(localMS);
    //    localMS = _rover.GetPeerFactory().createLocalMediaStream("LOCAL_MS");
     //   RoverCameraCapture roverCameraCapture = _rover.GetCameraCapturer(deviceName).get();

        //val localVideoSource = peerConnectionFactory.createVideoSource(false)
     //   (roverCameraCapture.c as VideoCapturer).initialize(surfaceTextureHelper, localVideoOutput.context, localVideoSource.capturerObserver)

    //   localMS.addTrack(_rover.GetPeerFactory().createVideoTrack("LOCAL_MS_VS", roverCameraCapture.));

//        audioSource = _rover.GetPeerFactory().createAudioSource(new MediaConstraints());
  //      localMS.addTrack(_rover.GetPeerFactory().createAudioTrack("LOCAL_MS_AT", audioSource));

    }

    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
        Log.i("onCreateSuccess", "Called onCreateSuccess!");

        try {
            pc.setLocalDescription(RoverClient.this, sdp);


            SdpCandidateMessage candidateMessage = new SdpCandidateMessage();
            candidateMessage.uuid = _uuid;
            candidateMessage.type = sdp.type.canonicalForm();
            candidateMessage.sdp = sdp.description;

            Log.i("onCreateSuccess", "sending our sdp!");

            _rover.SendWesocketMessage(new WebSocketMessage("SDP_CANDIDATE_MESSAGE", candidateMessage));
        }
        catch (Exception ex){
            Log.e("onCreateSuccess", Objects.requireNonNull(ex.getMessage()));
        }


    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, s);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange change to " + signalingState.name());
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.i(TAG, "onIceConnectionChange change to " + iceConnectionState.name());
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.i(TAG, "iceGatheringState change to " + iceGatheringState.name());
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        Log.i(TAG, "onIceCandidate called " + candidate.sdp);
        try {
            IceCandidateMessage candidateMessage = new IceCandidateMessage();
            candidateMessage.uuid = _uuid;
            candidateMessage.candidate = candidate.sdp;
            candidateMessage.sdpMLineIndex = candidate.sdpMLineIndex;
            candidateMessage.sdpMid = candidate.sdpMid;

            _rover.SendWesocketMessage(new WebSocketMessage("ICE_CANDIDATE_MESSAGE", candidateMessage));
        }
        catch (Exception ex){
            Log.e("ICE", ex.getMessage());
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        pc.removeIceCandidates(iceCandidates);
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
    //    Log.d(TAG, "onAddStream " + mediaStream.getId());
        // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
     //   mListener.onAddRemoteStream(mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
      //  Log.d(TAG, "onRemoveStream " + mediaStream.getId());
    //    mListener.onRemoveRemoteStream();
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        //Log.d(TAG, "onDataChannel " + dataChannel.state());

       // mDataChannel = dataChannel;
       // mDataChannel.registerObserver(this);
    }

    @Override
    public void onRenegotiationNeeded() {
    }



    @Override
    public void onBufferedAmountChange(long l) {

    }

    @Override
    public void onStateChange() {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {

    }
}
