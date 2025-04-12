import React, { useState, useRef, useEffect } from "react";

interface JoystickProps {
  onChange: (position: { x: number, y: number }) => void;
  position: { x: number, y: number };
}

const Joystick: React.FC<JoystickProps> = ({ onChange, position, setPosition } : JoystickProps) => {
  
  const [dragging, setDragging] = useState(false);
  const joystickRef = useRef<HTMLDivElement>(null);

  const handleMove = (clientX: number, clientY: number) => {
    if (!joystickRef.current) return;
    const rect = joystickRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const dx = clientX - centerX;
    const dy = clientY - centerY;
    const distance = Math.min(Math.sqrt(dx * dx + dy * dy), 40);
    const angle = Math.atan2(dy, dx);

    let position = {
      x: 50 + Math.cos(angle) * distance,
      y: 50 + Math.sin(angle) * distance,
    };


    onChange(position)

  };

  const handleMouseDown = (e: React.MouseEvent | React.TouchEvent) => {
    setDragging(true);
  };

  const handleMouseMove = (e: MouseEvent | TouchEvent) => {
    if (!dragging) return;
    const clientX = "touches" in e ? e.touches[0].clientX : e.clientX;
    const clientY = "touches" in e ? e.touches[0].clientY : e.clientY;
    handleMove(clientX, clientY);
  };

  const handleMouseUp = () => {
    setDragging(false);
    onChange({ x: 50, y: 50 });
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    let pos = position;

    if (e.key === "ArrowDown") onChange({ x: 50, y: 100 });
    if (e.key === "ArrowUp") onChange({ x: 50, y: 0 });
    if (e.key === "ArrowLeft") onChange({ x: 0, y: 50 });
    if (e.key === "ArrowRight") onChange({ x: 100, y: 50 });
  };
  const handleKeyUp = (e: KeyboardEvent) => {
    if (e.key === "ArrowUp" || e.key === 'ArrowDown' || e.key === 'ArrowLeft' || e.key === 'ArrowRight') onChange({ x: 50, y: 50 });
  };

  React.useEffect(() => {
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    window.addEventListener("touchmove", handleMouseMove);
    window.addEventListener("touchend", handleMouseUp);
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("touchmove", handleMouseMove);
      window.removeEventListener("touchend", handleMouseUp);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp); 
    };
  }, [dragging]);

  
  return (
    <div
      ref={joystickRef}
      style={{ bottom: "2rem" }}
      className="absolute w-24 h-24 bg-gray-300 rounded-full flex items-center justify-center "
    >
      <div
        className="absolute w-12 h-12 bg-blue-500 rounded-full cursor-pointer"
        style={{
          left: `${position.x}%`,
          top: `${position.y}%`,
          transform: "translate(-50%, -50%)",
        }}
        onMouseDown={handleMouseDown}
        onTouchStart={handleMouseDown}
      />
    </div>
  );
};

export default Joystick;
