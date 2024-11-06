import { useEffect, useState, useRef } from "react";

export default function useMeditationTimer(time) {
  const [showOverlay, setShowOverlay] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [count, setCount] = useState(time);
  const videoRef = useRef(null);
  const audioRef = useRef(null);
  const bellAudioRef = useRef(null);

  useEffect(() => {
    const timer = setInterval(() => {
      if (count > 0 && isPlaying) {
        setCount((prevCount) => prevCount - 1);
        if (count === 0 || !isPlaying) clearInterval(timer);
      }
    }, 1000);

    if (count === 0) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      videoRef.current.pause();
      videoRef.current.currentTime = 0;
      bellAudioRef.current.play();
      setShowOverlay(true);
    }

    return () => clearInterval(timer);
  }, [count, isPlaying]);

  function handleStart() {
    setShowOverlay(false);
    setTimeout(handlePlay, 1500);
  }

  function handlePlay() {
    setIsPlaying(true);
    audioRef.current.play();
    videoRef.current.play();
  }

  function handlePause() {
    setIsPlaying(false);
    audioRef.current.pause();
    videoRef.current.pause();
  }

  function handleRepeat() {
    setCount(time);
    audioRef.current.currentTime = 0;
    videoRef.current.currentTime = 0;
    bellAudioRef.current.pause();
    bellAudioRef.current.currentTime = 0;
    handleStart();
  }

  return {
    count,
    showOverlay,
    isPlaying,
    handleStart,
    handlePlay,
    handlePause,
    handleRepeat,
    videoRef,
    audioRef,
    bellAudioRef,
  };
}
