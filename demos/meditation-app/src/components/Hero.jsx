import { Link } from "react-router-dom";
import meditatingGirl from "../assets/meditating-girl.svg";
import { useTranslate } from "@tolgee/react";

function Hero() {
  const { t } = useTranslate(true);
  return (
    <section className="bg-secondaryBackground h-[calc(100vh_-_theme(height.header))]">
      <div className="container mx-auto flex-grow flex flex-col items-center pb-6 h-full">
        <div className="flex-grow">
          <img
            src={meditatingGirl}
            alt="illustration of meditating girl"
            className="h-full object-contain"
          />
        </div>

        <h1 className="text-3xl md:text-5xl font-bold font-heading text-center text-secondaryText">
          {t("heroTitle", "ZENITH")}
        </h1>
        <p className="text-lg md:text-xl text-center text-secondaryText mt-4">
          {t("tagLine", "Elevate Your Mind, Find Your Zen.")}
        </p>
        <Link to="/meditation/sound-select" className="secondary-btn btn mt-4">
          {t("heroButtonText", "Meditate!")}
        </Link>
      </div>
    </section>
  );
}

export default Hero;
