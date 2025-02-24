export interface IceCandidateMessage {
    uuid: string
    candidate: string
    sdpMLineIndex: number
    sdpMid: string
    usernameFragment: string
}

/*
String uuid;
    String candidate;
    int sdpMLineIndex;
    String sdpMid;
    String usernameFragment;
*/