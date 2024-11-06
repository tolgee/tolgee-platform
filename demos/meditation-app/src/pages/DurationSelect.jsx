import { useState } from "react";
import { useSearchParams, Navigate, useNavigate } from "react-router-dom";
import {
  meditationTypes,
  MIN_MEDITATION_SECONDS,
} from "../constants/constants";
import PrevNextButtons from "../components/PrevNextButtons";
import { useTranslate } from "@tolgee/react";

function DurationSelect() {
  const { t } = useTranslate();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // if no meditation sound selected, redirect to sound select page
  const type = searchParams.get("type");
  if (!meditationTypes.includes(type.toLowerCase())) {
    return <Navigate to="/meditation/sound-select" />;
  }

  const [time, setTime] = useState({ minutes: 0, seconds: 0 });
  const [error, setError] = useState(null);
  const totalSeconds = time.minutes * 60 + time.seconds;

  function handleChange(e) {
    const { name, value } = e.target;
    let actualValue = Number(value);
    if (isNaN(actualValue) || actualValue < 0) {
      actualValue = 0;
    } else if (actualValue > 59) {
      actualValue = 59;
    }
    setTime((prevTime) => ({ ...prevTime, [name]: actualValue }));
  }

  function handlePrev() {
    navigate(`/meditation/sound-select?type=${type}`);
  }
  function handleNext() {
    if (totalSeconds > MIN_MEDITATION_SECONDS) {
      navigate(`/meditation/meditate?type=${type}&time=${totalSeconds}`);
    } else {
      setError(
        t(
          "durationSelectError",
          "Please select duration greater than {min} seconds.",
          {
            min: MIN_MEDITATION_SECONDS,
          }
        )
      );
    }
  }

  return (
    <div className="flex flex-col gap-4 flex-grow px-8 pb-6 h-full">
      <div className="flex-grow justify-center items-center flex flex-col gap-6">
        <h2 className="text-center text-xl md:text-3xl font-heading font-bold text-primary">
          {t("durationSelectTitle", "Select the duration:")}
        </h2>
        <div className="flex gap-3 text-[3rem] md:text-[3.5rem] bg-themeLightColor p-5 rounded text-primary border-4 border-themeColor shadow-lg font-number">
          <input
            type="number"
            name="minutes"
            className="w-[6rem] md:w-[7rem] tracking-wider p-2"
            onChange={handleChange}
            value={String(time.minutes).padStart(2, "0")}
          />
          <span>:</span>
          <input
            type="number"
            name="seconds"
            className="w-[6rem] md:w-[7rem] tracking-wider p-2"
            onChange={handleChange}
            value={String(time.seconds).padStart(2, "0")}
          />
        </div>
        {error && (
          <div className="text-red-400 text-lg text-center font-semibold font-primary">
            {error}
          </div>
        )}
      </div>
      <PrevNextButtons
        prevButtonText={t("soundSelection", "Sound Selection")}
        prevButtonOnClick={handlePrev}
        nextButtonText={t("lets_meditate", "Lets Meditate!")}
        nextButtonOnClick={handleNext}
      />
    </div>
  );
}

export default DurationSelect;
