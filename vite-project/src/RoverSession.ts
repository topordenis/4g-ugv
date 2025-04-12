import { SdpCandidateMessage } from "./interfaces/SdpCandidateMessage";
import { IceCandidateMessage } from "./interfaces/IceCandidateMessage";
import { RegisterAsRoverClientMessage } from "./interfaces/RegisterAsRoverClientMessage";
import { GetVideoSourcesMessage } from "./interfaces/GetVideoSourcesMessage";

const config = {
  iceServers: [
    {
      urls: "stun:stun.cloudflare.com:3478", // list of free STUN servers: https://gist.github.com/zziuni/3741933
    },
  ],
};

export class RoverSession {
  url: string;
  _connection: WebSocket | undefined;
  _peer: RTCPeerConnection | undefined;
  _commandsChannel: RTCDataChannel | undefined;
  _onUpdate: (data: string) => void | undefined;
  _heartbeatInterval: number;

  constructor(url: string) {
    this.url = url;
    this._heartbeatInterval = 1000;
  }

  init() {
    this._connection = new WebSocket(this.url);
    this._connection.onopen = (event) => this.on_websocket_connect(event);
    this._connection.onclose = (event) => this.on_websocket_close(event);
    this._connection.onmessage = (event) => this.on_websocket_message(event);
  }
  onUpdate(_fn: (data: string) => void) {
    this._onUpdate = _fn;
  }

  sendCommands(speed: number, steer: number) {
    console.info('send commands to channel ' + this._commandsChannel)
    if (this._commandsChannel) this._commandsChannel.send(JSON.stringify({
      steer: steer,
      speed: speed
    }));
  }
  async on_websocket_connect(event: Event) {
    await this.send(
      <RegisterAsRoverClientMessage>{
        uuid: "empty",
        maxSpeed: 200,
      },
      "REGISTER_AS_ROVER_CLIENT_MESSAGE"
    );
    await this.send(
      <GetVideoSourcesMessage>{
        uuid: "empty",
        sources: [],
      },
      "GET_VIDEO_SOURCES_MESSAGE"
    );
  }
  async create_offer(deviceName: string) {
    console.info("websocket connect");
    this._peer = new RTCPeerConnection(config);
   this._peer.ontrack = function (event) {
    console.log('Received track:', event.track.kind);
  
    console.info('streams count ' + event.streams.length)
    if (event.streams && event.streams.length > 0) {
      console.log('Received stream with id:', event.streams[0].id);
      
      console.log('track ' + JSON.stringify(event.streams[0].getTracks()))
      // Assign the stream to a video element
      const remoteVideo = document.getElementById('remoteVideo') as HTMLDivElement | null;
      if (remoteVideo) {
      //@ts-ignore
      console.info('set stream to element')
      
        remoteVideo.srcObject = event.streams[0];
        //@ts-ignore
        remoteVideo.play().catch((err) => {
          console.error('Error trying to play video:', err);
        });
        
      }
    }
      }
    this._peer.addTransceiver('video', {
        direction: 'sendrecv'
      })
    this._peer.onicecandidate = (event: RTCPeerConnectionIceEvent) =>
      this.on_local_ice(event);

    this._peer.oniceconnectionstatechange = (event: Event) => {
        if (this._peer)
        console.info("ice connection state change " + this._peer.iceConnectionState);
    }
   this._commandsChannel = this._peer.createDataChannel("commands");
    this._commandsChannel.onopen = (event: Event) =>
      this.on_data_channel_open(event);
    this._commandsChannel.onclose = (event: Event) =>
      this.on_data_channel_close(event);
    this._commandsChannel.onmessage = (event: MessageEvent) =>
      this.on_data_channel_message(event);
    
    await this._peer.setLocalDescription(await this._peer.createOffer({
        offerToReceiveAudio: false,
        offerToReceiveVideo: true,
    }));

    console.info(
      "this._peer.localDescription?.type " + this._peer.localDescription?.type
    );
    await this.send(
      <SdpCandidateMessage>{
        uuid: "empty",
        sdp: this._peer.localDescription?.sdp,
        type: this._peer.localDescription?.type,
        deviceName: deviceName,
      },
      "SDP_CANDIDATE_MESSAGE"
    );
  }
  async on_websocket_close(event: Event) {
    console.info("websocket close");
  }
  on_websocket_message(event: MessageEvent) {
    /* const blob = event.data


        const data = await blob.text();

        if (data instanceof ArrayBuffer) {
            // binary frame
            let result = unpack(data) as IBinaryPacket;

            console.info('result ' + JSON.stringify(result.type))

            let packet = unpack(result.buffer) as any;
            switch (result.type) {
                case EBinaryPacketType.ICE_CANDIDATE:
                    this.on_ice_candidate(packet as IceCandidatePacket)
                    break;
                case EBinaryPacketType.SDP_CANDIDATE:
                    this.on_sdp_candidate(packet as SdpCandidatePacket)
                    break;
            }

        }
        */
    try {
      const msg = JSON.parse(event.data);
      console.info('got msg ' + JSON.stringify(msg) )
      switch (msg.type) {
        case "ICE_CANDIDATE_MESSAGE":
          this.on_ice_candidate(JSON.parse(msg.data) as IceCandidateMessage);
          break;
        case "SDP_CANDIDATE_MESSAGE":
          this.on_sdp_candidate(JSON.parse(msg.data) as SdpCandidateMessage);
          break;
        case "GET_VIDEO_SOURCES_MESSAGE":
            this.on_get_video_sources(JSON.parse(msg.data) as GetVideoSourcesMessage);
            break;
      }
    } catch (e) {
      console.error("error parsing json " + e);
    }
  }
  async on_data_channel_open(event: Event) {
    console.info("data channel open");
    // this._heartbeatInterval = setInterval(() => {
    //   if (this._commandsChannel) this._commandsChannel.send("heartbeat");
    // }, 300);
  }
  async on_data_channel_close(event: Event) {
    console.info("data channel open");
    if (this._heartbeatInterval) clearInterval(this._heartbeatInterval);
  }
  async on_data_channel_message(event: MessageEvent) {
    if (this._onUpdate) this._onUpdate(await event.data);
  }
  async on_get_video_sources(video_sources: GetVideoSourcesMessage) {
    console.info('video_sources ' + JSON.stringify(video_sources));
    await this.create_offer(video_sources.sources[0]);
  }
  async on_sdp_candidate(sdp_candidate: SdpCandidateMessage) {
    let init = <RTCIceCandidateInit>{};

    const description = new RTCSessionDescription({
      type: "answer",
      sdp: sdp_candidate.sdp, // SDP string from the message
    });

    console.info("remote description " + JSON.stringify(description));
    if (this._peer) this._peer.setRemoteDescription(description);
  }
  async on_ice_candidate(ice_candidate: IceCandidateMessage) {
    let ice = {
      candidate: ice_candidate.candidate,
      sdpMLineIndex: ice_candidate.sdpMLineIndex,
      sdpMid: ice_candidate.sdpMid,
      usernameFragment: ice_candidate.usernameFragment,
    };
    console.info('on_ice_candidate ' + JSON.stringify(ice));
    if (this._peer) this._peer.addIceCandidate(ice);
  }
  on_local_ice(event: RTCPeerConnectionIceEvent) {
    if (event.candidate != null) {
      console.info("got ice " + JSON.stringify(event.candidate));
      /*
            std::string uuid{};
std::string candidate{};
int sdpMLineIndex{};
std::string sdpMid{};
std::string usernameFragment{};
            */
      this.send(
        <IceCandidateMessage>{
          candidate: event.candidate.candidate,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
          sdpMid: event.candidate.sdpMid,
          usernameFragment: event.candidate.usernameFragment,
        },
        "ICE_CANDIDATE_MESSAGE"
      );
    }
  }

  async send(packet: any, type: string) {
    let buffer = {
      type: type,
      data: JSON.stringify(packet),
    };

    if (this._connection) await this._connection.send(JSON.stringify(buffer));
  }
}
