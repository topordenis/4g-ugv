import { useEffect, useReducer, useState, useRef } from "react";
import reactLogo from "./assets/react.svg";
import viteLogo from "/vite.svg";
import "./App.css";
import { RoverSession } from "./RoverSession";

import { RoverState } from "./interfaces/Roverstate";
import { Joystick } from 'react-joystick-component';
import { IJoystickUpdateEvent } from "react-joystick-component/build/lib/Joystick";
function App() {
  const [count, setCount] = useState(0);

  // New state to hold the current date and time
  const [currentTime, setCurrentTime] = useState(new Date().toLocaleString());

  const [roverState, setRoverState] = useState<RoverState>({ Voltage: 36.2, Speed: 5 });

  const startStream = () => {
    //@ts-ignore
    window.roverSession = new RoverSession("ws://cf.topordenis164.workers.dev/");
    //@ts-ignore
    window.roverSession.init();

  }
  // useEffect(() => {
  //   // Set up the interval to update time every second
  //   const interval = setInterval(() => {
  //     setCurrentTime(new Date().toLocaleString());
  //   }, 1000);

  //   // Initialize the rover session

  //   // Clean up the interval when the component is unmounted
  //   return () => clearInterval(interval);
  // }, []);

  const [position, setPosition] = useState({ x: 0, y: 0 });

  let onStop = (e) => {
    console.info('on stop');
    setPosition({ x: 0, y: 0 })

    //@ts-ignore
    window.roverSession.sendCommands(0, 0)
  }
  let ref = useRef(new Date());

  let sendCommand = (speed: number, steer: number) => {
    if (isNaN(speed) || isNaN(steer))
      return;

    //@ts-ignore
    if (window.roverSession)
      //@ts-ignore
      window.roverSession.sendCommands(speed, steer)
  }

  let handleMove = (ev: any) => {
    console.info("position ", JSON.stringify(ev));

    setPosition({ x: ev.x, y: ev.y });

    let difference_ms = new Date().getTime() - ref.current.getTime();

    if (difference_ms > 25) {
      //@ts-ignore
      console.info('difference_ms ' + difference_ms)
      //@ts-ignore
      sendCommand(ev.y * 200, ev.x * 100)
      ref.current = new Date()
    }

  };


  return (
    <>
      <div className="flex flex-col items-center justify-center h-screen overflow-hidden">
        <video
          id="remoteVideo"
          autoPlay
          style={{ width: "100%", height: "100%" }}
        ></video>
        <div style={{ width: '4rem', height: '1rem', cursor: 'pointer', backgroundColor: 'red' }} onClick={startStream}>Start stream</div>
        <div className="absolute bottom-0">
        <Joystick  size={130} sticky={false} baseColor="red" stickColor="blue" pos={position} move={handleMove} stop={onStop}></Joystick>
        </div>

        {/* Display current time */}
        <div style={{ display: 'flex', flexDirection: 'column', position: "absolute", top: "10px", right: "10px", color: "white" }}>
          <p> {currentTime}</p>
          <p> Voltage: {roverState.Voltage}V</p>
          <p> Speed: {roverState.Voltage}km/h</p>
        </div>

      </div>
    </>
  );
}

export default App;
