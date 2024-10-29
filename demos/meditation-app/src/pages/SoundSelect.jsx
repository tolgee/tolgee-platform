import { useState } from "react";
import { meditationTypes } from "../constants/constants";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useTranslate } from "@tolgee/react";
import PrevNextButtons from "../components/PrevNextButtons";
import SoundCard from "../components/SoundCard";
import ErrorMessage from "../components/ErrorMessage";
import capitalizeWord from "../utils/capitalizeWord";

function SoundSelect() {
  const { t } = useTranslate();
  const [searchParams] = useSearchParams();
  const activeType = searchParams.get("type");
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  function handleNextClick() {
    if (activeType && meditationTypes.includes(activeType)) {
      navigate(`/meditation/duration-select?type=${activeType}`);
    } else {
      setError("Please select a meditation type.");
    }
  }
  return (
    <div className="flex flex-col flex-grow gap-8 px-8 pb-6 h-full">
      <div className="flex-grow flex flex-col justify-center gap-8 w-full">
        <h2 className="text-center text-xl md:ext-3xl font-heading font-bold text-primary p-3">
          {t("soundSelectTitle", "Select a sound:")}
        </h2>
        <div className="flex flex-col gap-10 md:flex-row justify-around">
          {meditationTypes.map((type) => (
            <SoundCard
              key={type}
              type={type}
              displayName={t(`meditationType_${type}`, capitalizeWord(type))}
              isActive={activeType === type}
            />
          ))}
        </div>
        {error && <ErrorMessage error={t("soundSelectError", error)} />}
      </div>
      <PrevNextButtons
        nextButtonText={t("durationSelection", "Duration Selection")}
        nextButtonOnClick={handleNextClick}
      />
    </div>
  );
}

export default SoundSelect;
