package com.topor.rover;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.FrameCryptor;
import org.webrtc.FrameCryptorAlgorithm;
import org.webrtc.FrameCryptorFactory;
import org.webrtc.FrameCryptorKeyProvider;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import android.util.Log;
public class Rover {
    private WebSocket webSocket;
    private OkHttpClient client;
    private final EglBase _rootEglBase;
    private String _wsUrl;
    private PeerConnectionFactory factory;

    private MainActivity _activity;

    private ArrayList<RoverClient> roverClients;

    private ArrayList<RoverCameraCapture> _capturers;

    private final MediaConstraints pcConstraints = new MediaConstraints();

    public Rover(String wsUrl, MainActivity activity,  EglBase rootEglBase) {
        _wsUrl = wsUrl;
        _rootEglBase = rootEglBase;
        _activity = activity;
        client = new OkHttpClient();
        roverClients = new ArrayList<RoverClient>();
        _capturers = new ArrayList<RoverCameraCapture>();

     /*   pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));


        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(1080)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(2400)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(30)));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(30)));
*/
    }


    public void OnWebSocketMessage(String data) {
        Log.d("WebSocketHandler", "Received message: " + data);


        Gson gson = new Gson();
        // Deserialize the incoming JSON message into a Message object
        WebSocketMessage message = gson.fromJson(data, WebSocketMessage.class);
//GetVideoSourcesMessage
        //RegisterAsRoverClientMessage
        if (message.getType().equals("REGISTER_AS_ROVER_CLIENT_MESSAGE")) {

            Log.i("Mata", "REGISTER_AS_ROVER_CLIENT_MESSAGE got, Create new rover client");

            RegisterAsRoverClientMessage msg = gson.fromJson(message.getData().toString(), RegisterAsRoverClientMessage.class);
            RoverClient client = new RoverClient(this, msg.uuid);

            roverClients.add(client);

        }
        else if(message.getType().equals("GET_VIDEO_SOURCES_MESSAGE")) {
            GetVideoSourcesMessage msg = gson.fromJson(message.getData().toString(), GetVideoSourcesMessage.class);

            msg.sources = GetVideoSources();
            SendWesocketMessage(new WebSocketMessage("GET_VIDEO_SOURCES_MESSAGE", msg));
        }
        else if (message.getType().equals("SDP_CANDIDATE_MESSAGE")) {

            SdpCandidateMessage sdp = gson.fromJson(message.getData().toString(), SdpCandidateMessage.class);

            Optional<RoverClient> client = roverClients.stream()
                    .filter(p -> p.GetUUID().equals(sdp.uuid))
                    .findFirst();

            if (client.isPresent()) {

             //  client.get().setCamera(sdp.deviceName);

                SessionDescription sdp_description = new SessionDescription(
                        SessionDescription.Type.OFFER,
                        sdp.sdp
                );
                client.get().GetPeer().setRemoteDescription(client.get(), sdp_description);
                Log.i("Rover","createAnswer for sdp description");
                client.get().GetPeer().createAnswer(client.get(), pcConstraints);

            }
            else {
                Log.e("OnWebSocketMessage", "Couldnt find a client for SDP Message");
            }


            // Here you can update your UI or process the user data further
            // For example, you could update a TextView or make changes to your app
        }
        else if (message.getType().equals("ICE_CANDIDATE_MESSAGE")) {
            // Deserialize the 'data' field into a User object
            IceCandidateMessage ice = gson.fromJson(message.getData().toString(), IceCandidateMessage.class);

            Optional<RoverClient> client = roverClients.stream()
                    .filter(p -> p.GetUUID().equals(ice.uuid))
                    .findFirst();

            if (client.isPresent()){
                IceCandidate candidate = new IceCandidate(
                      ice.sdpMid,
                       ice.sdpMLineIndex,
                       ice.candidate
                );
                Log.i("OnWebSocketMessage", "ADD ICE FOR CANDIDATE");
                client.get().GetPeer().addIceCandidate(candidate);
            }else {
                Log.e("OnWebSocketMessage", "Couldnt find a client for that ICE Message");
            }
            // Here you can update your UI or process the user data further
            // For example, you could update a TextView or make changes to your app
        }
        else {
            Log.d("WebSocketHandler", "Unknown message type: " + message.getType());
        }
    }

    public  PeerConnectionFactory GetPeerFactory(){
        return factory;
    }
    public  void InitializePeerFactory(){
        // Initialize WebRTC
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder((Context) _activity)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        // This is very important: https://stackoverflow.com/a/69983765/7569705
        /*
            Without encoder/decoder we won't be able to send/receive remote stream

            The error message was triggered due to the offer containing H264 codecs whilst the Android Client was not anticipating H264 and was not setup to encode and/or decode this particular hardware encoded stream.
         */
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(_rootEglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(_rootEglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(decoderFactory)
                .setVideoEncoderFactory(encoderFactory)
                .createPeerConnectionFactory();
    }
    public void Initialize(){
        Request request = new Request.Builder().url(_wsUrl).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {

                InitializePeerFactory();

                System.out.println("Connected to WebSocket");
                WebSocketMessage message = new WebSocketMessage("ROVER_SIGNAL", "imtherover");

                Gson gson = new Gson();
                String json = gson.toJson(message);

                webSocket.send(json);


            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Received: " + text);
                OnWebSocketMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                System.out.println("Closing: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("Error: " + t.getMessage());
            }
        });

    }


    public CameraEnumerator GetCameraEnumerator(){
        CameraEnumerator enumerator;

        if (Camera2Enumerator.isSupported((Context) _activity)) {
            enumerator = new Camera2Enumerator((Context) _activity);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        return enumerator;
    }
    public String[] GetVideoSources(){
        CameraEnumerator enumerator = GetCameraEnumerator();
        return enumerator.getDeviceNames();
    }

    Optional<RoverCameraCapture> GetCameraCapturer(String deviceName) {
        CameraEnumerator enumerator = GetCameraEnumerator();
        final String[] deviceNames = enumerator.getDeviceNames();

        if (!Arrays.asList(deviceNames).contains(deviceName))
            return Optional.empty();


        for (RoverCameraCapture capturer : _capturers) {
            if (capturer._deviceName.compareTo(deviceName) == 0) {
                capturer._references++;
                Log.i("Rover", "Already existing camera capture. Increasing reference count.");
                return Optional.of(capturer);
            }
        }

        Log.i("Rover", "Camera capture doesnt exist .Creating it");


        RoverCameraCapture roverCameraCapture = new RoverCameraCapture(deviceName, enumerator);
        roverCameraCapture.Initialize(factory, _rootEglBase, _activity);
        _capturers.add(roverCameraCapture);
        return Optional.of(roverCameraCapture);
    }

    public void SendWesocketMessage(WebSocketMessage message){
        Gson gson = new Gson();
        String json = gson.toJson(message);

        webSocket.send(json);

        Log.i("Rover", "Sending websocket message!");
    }

}
