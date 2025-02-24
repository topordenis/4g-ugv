import { useEffect, useState } from "react";
import reactLogo from "./assets/react.svg";
import viteLogo from "/vite.svg";
import "./App.css";
import { RoverSession } from "./RoverSession";
import Joystick from "./Joystick";
import { RoverState } from "./interfaces/Roverstate";

function App() {
  const [count, setCount] = useState(0);

  // New state to hold the current date and time
  const [currentTime, setCurrentTime] = useState(new Date().toLocaleString());

  const [roverState, setRoverState] = useState<RoverState>({Voltage: 36.2, Speed: 5});
  useEffect(() => {
    // Set up the interval to update time every second
    const interval = setInterval(() => {
      setCurrentTime(new Date().toLocaleString());
    }, 1000);

    // Initialize the rover session
    let session = new RoverSession("ws://cf.topordenis164.workers.dev/");
    session.init();

    // Clean up the interval when the component is unmounted
    return () => clearInterval(interval);
  }, []);

  const [position, setPosition] = useState({ x: 50, y: 50 });

  let onJoystickChange = (position: any) => {
    console.info("position", position);
    setPosition(position);
  };

  return (
    <>
      <div className="flex flex-col items-center justify-center h-screen">
        <video
          id="video"
          autoPlay
          style={{ width: "100%", height: "100%" }}
        ></video>
        <Joystick onChange={onJoystickChange} position={position} />
        {/* Display current time */}
        <div style={{ display: 'flex', flexDirection: 'column',  position: "absolute", top: "10px", right: "10px", color: "white" }}>
         <p> {currentTime}</p>
          <p> Voltage: {roverState.Voltage}V</p>
          <p> Speed: {roverState.Voltage}km/h</p>
        </div>
       
      </div>
    </>
  );
}

export default App;
