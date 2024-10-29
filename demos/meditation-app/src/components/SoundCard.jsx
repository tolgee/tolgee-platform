import { useSearchParams } from "react-router-dom";
import { useRef } from "react";
import capitalizeWord from "../utils/capitalizeWord";

function SoundCard({ type, displayName, isActive }) {
  const [_, setSearchParams] = useSearchParams();
  const videoRef = useRef(null);
  const audioRef = useRef(null);

  function handleMouseEnter() {
    videoRef.current.play();
  }

  function handleMouseLeave() {
    videoRef.current.pause();
  }

  return (
    <div className="flex flex-col items-center gap-4">
      <button
        className={`btn h-32 md:h-44 aspect-square rounded-full overflow-hidden shadow-md border-8 ${
          isActive ? "border-themeColor" : ""
        }`}
        onClick={() => setSearchParams({ type })}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
      >
        <video muted loop ref={videoRef} className="object-cover h-full">
          <source src={`/videos/${type}.mp4`} type="video/mp4" />
        </video>
      </button>
      <span
        className={`text-lg md:text-xl font-semibold font-primary ${
          isActive ? "text-themeColor" : "text-primaryText"
        }`}
      >
        {displayName}
      </span>
    </div>
  );
}

export default SoundCard;
