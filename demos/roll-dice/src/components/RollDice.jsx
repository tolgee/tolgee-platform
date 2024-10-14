import React, { useRef, useState, useEffect, forwardRef, useImperativeHandle } from "react";
import { Canvas, useFrame, useLoader } from "@react-three/fiber";
import * as THREE from "three";

// Use forwardRef to expose the startRoll method
const RollDice = forwardRef((props, ref) => {
  // Load all textures
  const texture1 = useLoader(THREE.TextureLoader, './textures/face-1.png');
  const texture2 = useLoader(THREE.TextureLoader, './textures/face-2.png');
  const texture3 = useLoader(THREE.TextureLoader, './textures/face-3.png');
  const texture4 = useLoader(THREE.TextureLoader, './textures/face-4.png');
  const texture5 = useLoader(THREE.TextureLoader, './textures/face-5.png');
  const texture6 = useLoader(THREE.TextureLoader, './textures/face-6.png');

  const textures = [texture1, texture2, texture3, texture4, texture5, texture6];

  const cubeRef = useRef();
  const [rolling, setRolling] = useState(false);
  const [rotationSpeed, setRotationSpeed] = useState(0.1);
  const [finalRotation, setFinalRotation] = useState([0, 0, 0]);
  const [currentTexture, setCurrentTexture] = useState(textures[0]); // Initial texture

  // Expose the startRoll method to the parent
  useImperativeHandle(ref, () => ({
    startRoll: () => {
      startRolling(props.result);
    }
  }));

  const startRolling = (result) => {
    setRolling(true);
    setRotationSpeed(0.3);
    const targetRotation = getRotationForResult(result);
    setFinalRotation(targetRotation);
  };

  const getRotationForResult = (result) => {
    switch (result) {
      case 1:
        return [0, 0, 0];
      case 2:
        return [0, Math.PI / 2, 0];
      case 3:
        return [Math.PI / 2, 0, 0];
      case 4:
        return [-Math.PI / 2, 0, 0];
      case 5:
        return [0, -Math.PI / 2, 0];
      case 6:
        return [Math.PI, 0, 0];
      default:
        return [0, 0, 0];
    }
  };

  useEffect(() => {
    // Update the current texture when props.result changes
    if (props.result >= 1 && props.result <= 6) {
      setCurrentTexture(textures[props.result - 1]);
    }
  }, [props.result, textures]); // Re-run when props.result or textures change

  useFrame(() => {
    if (rolling) {
      // Spin the cube continuously
      cubeRef.current.rotation.x += rotationSpeed;
      cubeRef.current.rotation.y += rotationSpeed;

      // Gradually slow down the rotation
      if (rotationSpeed > 0.05) {
        setRotationSpeed(rotationSpeed * 0.95); // Slow down
      } else {
        // Stop the rolling and set the final rotation
        setRolling(false);
        cubeRef.current.rotation.set(...finalRotation);
      }
    }
  });

  return (
    <>
      <ambientLight intensity={2} color={"white"} />
      <mesh ref={cubeRef} position={[0, 0, 0]}>
        <boxGeometry args={[3, 3, 3]} />
        {/* Apply the dynamically updated texture */}
        <meshStandardMaterial map={currentTexture} />
      </mesh>
    </>
  );
});

export default RollDice;
