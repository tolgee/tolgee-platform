import React from "react";
import { useTranslate } from "@tolgee/react";

interface ResultInterface {
  score: number;
  restart: () => void;
}

const Result: React.FC<ResultInterface> = ({ score, restart }) => {
  const { t } = useTranslate();

  return (
    <div className="flex gap-10 justify-center items-center max-sm:flex-col max-sm:mt-[120px]">
      <div>
        <img
          src="/flag/countryFlag.png"
          alt="Country Flag"
          className="w-80 animate-spinSlow max-sm:hidden "
        />
      </div>
      <div className="flex flex-col items-center justify-center w-full max-w-lg bg-gradient-to-b from-gray-800 to-gray-900 p-10 rounded-2xl shadow-2xl transition-all duration-300 ease-in-out mb-5">
        <p className="text-3xl font-semibold text-center mb-6 text-white bg-gradient-to-r from-gray-800 to-gray-900 p-4 rounded-lg shadow-lg max-md:text-xl">
          {t("game-over")}
        </p>

        <p className="text-2xl text-white mb-4 font-medium">
          {t("your-score")}{" "}
          <span
            className={`font-bold ${
              score >= 7 ? "text-green-400" : "text-red-400"
            }`}
          >
            {score}
          </span>
        </p>

        <p
          data-testid="outcome"
          className={`text-2xl font-bold mb-6 ${
            score >= 7 ? "text-green-500" : "text-red-500"
          } transition-colors duration-300`}
        >
          {score >= 7 ? t("you-win") : t("you-lose")}
        </p>

        <button
          className="bg-indigo-800 text-white py-3 px-8 rounded-full hover:bg-indigo-600 transition duration-200 transform hover:scale-110 shadow-md focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-gray-800"
          onClick={restart}
        >
          {t("restart-quiz")}
        </button>
      </div>

      <div>
        <img
          src="/flag/countryFlag.png"
          alt="Country Flag"
          className="w-80 animate-spinSlow max-sm:w-64 "
        />
      </div>
    </div>
  );
};

export default Result;
