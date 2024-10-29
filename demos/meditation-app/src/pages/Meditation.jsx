import { useState } from "react";
import { useSearchParams, Navigate, Link } from "react-router-dom";
import { useTranslate } from "@tolgee/react";
import { IoPause, IoPlay, IoClose } from "react-icons/io5";
import { meditationTypes } from "../constants/constants";
import useMeditationTimer from "../hooks/useMeditationTimer";
import ExitMeditationModal from "../components/ExitMeditationModal";
import lotus from "../assets/lotus.png";

function Meditation() {
  const [searchParams] = useSearchParams();
  const type = searchParams.get("type");
  const time = Number(searchParams.get("time"));
  const [showModal, setShowModal] = useState(false);

  // if type of meditation is unknown
  if (!meditationTypes.includes(type.toLowerCase())) {
    return <Navigate to="/meditation/sound-select" />;
  }

  // if time is invalid or greater than 1 hour
  if (isNaN(time) || time > 3599 || time <= 10) {
    return <Navigate to="/meditation/duration-select" />;
  }

  const {
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
  } = useMeditationTimer(time);

  const minutes = String(Math.trunc(count / 60)).padStart(2, "0");
  const seconds = String(count % 60).padStart(2, "0");

  function handleClose() {
    handlePause();
    setShowModal(true);
  }

  return (
    <main className="relative h-screen w-screen overflow-hidden">
      <video
        muted
        loop
        ref={videoRef}
        className="fixed top-0 left-0 h-screen w-screen object-cover"
      >
        <source src={`/videos/${type}.mp4`} type="video/mp4" />
      </video>
      <audio loop ref={audioRef}>
        <source src={`/sounds/${type}.mp3`} type="audio/mpeg" />
      </audio>
      <audio ref={bellAudioRef}>
        <source src={`/sounds/bell.mp3`} type="audio/mpeg" />
      </audio>
      <div className="fixed h-screen w-screen z-10 flex flex-col items-center justify-center gap-10 text-primary font-primary">
        <div className="flex gap-3 p-4 text-[2.75rem] sm:text-[3.5rem] bg-primaryBackground/30 rounded backdrop-blur font-number">
          <div className="p-1 flex gap-1">
            <span className="bg-primaryBackground w-12 sm:w-16 text-center px-2 py-1 rounded">
              {minutes[0]}
            </span>
            <span className="bg-primaryBackground w-12 sm:w-16 text-center px-2 py-1 rounded">
              {minutes[1]}
            </span>
          </div>
          <span>:</span>
          <div className="p-1 flex gap-1">
            <span className="bg-primaryBackground w-12 sm:w-16 text-center px-2 py-1 rounded">
              {seconds[0]}
            </span>
            <span className="bg-primaryBackground w-12 sm:w-16 text-center px-2 py-1 rounded">
              {seconds[1]}
            </span>
          </div>
        </div>
        <div className={`flex gap-8 ${count === 0 ? "hidden" : ""}`}>
          <button className="btn circle-btn" onClick={handleClose}>
            <IoClose />
          </button>
          <button
            className="btn circle-btn"
            onClick={isPlaying ? handlePause : handlePlay}
          >
            {isPlaying ? <IoPause /> : <IoPlay />}
          </button>
        </div>
      </div>
      <StartEndOverlay
        count={count}
        handleRepeat={handleRepeat}
        handleStart={handleStart}
        showOverlay={showOverlay}
      />
      {showModal && <ExitMeditationModal setShowModal={setShowModal} />}
    </main>
  );
}

function StartEndOverlay({ handleRepeat, handleStart, showOverlay, count }) {
  const { t } = useTranslate();

  return (
    <div
      className={`absolute flex items-center justify-center top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 aspect-square rounded-full bg-primaryBackground z-20 transition-[width] duration-1000 ease-linear overflow-hidden ${
        showOverlay ? "w-[calc(1.5_*_max(100vw,100vh))]" : "w-0"
      }`}
    >
      {count > 5 ? (
        <button
          className="w-28 aspect-square relative bg-secondaryBackground text-secondaryText rounded-full font-bold text-xl font-primary before:content-[''] before:absolute before:-z-10 before:w-full before:aspect-square hover:before:w-[200%] before:top-1/2 before:left-1/2 before:-translate-x-1/2 before:-translate-y-1/2 before:rounded-full before:bg-secondaryBackground/50 before:transition-[width] before:duration-300 before:ease-linear"
          onClick={handleStart}
        >
          {t("start", "Start")}
        </button>
      ) : (
        <div className="flex flex-col gap-8 text-primaryText items-center">
          <h3 className="text-2xl font-bold">{t("wellDone", "Well Done!")}</h3>
          <img src={lotus} alt="Lotus" className="w-20" />
          <div className="flex max-[420px]:flex-col flex-row gap-6">
            <Link to="/" className="btn secondary-btn">
              {t("goToHome", "Go to Home")}
            </Link>
            <button className="btn primary-btn" onClick={handleRepeat}>
              {t("repeat", "Repeat!")}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Meditation;
