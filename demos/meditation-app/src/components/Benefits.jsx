import BenefitsCard from "./BenefitsCard";
import leafIcon from "../assets/leafIcon.svg";
import bandageIcon from "../assets/bandageIcon.svg";
import nightIcon from "../assets/nightIcon.svg";
import { useTranslate } from "@tolgee/react";

function Benefits() {
  const { t } = useTranslate();
  return (
    <div className="h-[calc(100vh_-_theme(height.header))] bg-primaryBackground p-4">
      <div className="container mx-auto py-12 flex flex-col gap-6 justify-around h-full">
        <div>
          <h2 className="text-xl md:text-4xl font-bold text-center mb-8 font-heading text-primaryText">
            {t("benefitsHeading", "Benefits of Meditation")}
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-8">
            <BenefitsCard
              title={t("card1Title", "Improved Focus")}
              text={t(
                "card1Text",
                "Regular meditation improves concentration, making it easier to focus on tasks."
              )}
              iconSrc={leafIcon}
            />
            <BenefitsCard
              title={t("card2Title", "Reduced Stress")}
              text={t(
                "card2Text",
                "Meditation helps reduce stress, creating a sense of calm and relaxation."
              )}
              iconSrc={bandageIcon}
            />
            <BenefitsCard
              title={t("card3Title", "Better Sleep")}
              text={t(
                "card3Text",
                "Meditation promotes relaxation, leading to more restful sleep."
              )}
              iconSrc={nightIcon}
            />
          </div>
        </div>

        <blockquote className="font-number text-lg md:text-3xl text-center hidden md:block">
          {t(
            "quote",
            "“Meditation is not about stopping thoughts, but recognizing that we are more than our thoughts and our feelings.”"
          )}
          <span className="whitespace-nowrap"> — Arianna Huffington</span>
        </blockquote>
      </div>
    </div>
  );
}

export default Benefits;
