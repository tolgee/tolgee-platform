import React, { useState } from "react";
import Guess from "@/components/Guess";
import { OptionsInterface } from "@/app/[locale]/App";
import { useTranslate } from "@tolgee/react";

interface CountryInterface {
  country: string;
  flag: string;
}

interface QuizInterface {
  options: CountryInterface[];
  handleOptions: React.Dispatch<React.SetStateAction<OptionsInterface[]>>;
  currentCountry: CountryInterface;
  handleCountry: React.Dispatch<React.SetStateAction<void>>;
  score: number;
  handleCorrectCount: React.Dispatch<React.SetStateAction<number>>;
}

const Quiz: React.FC<QuizInterface> = ({
  options,
  handleOptions,
  currentCountry,
  handleCountry,
  score,
  handleCorrectCount,
}) => {
  const [correctText, setCorrectText] = useState(false);
  const [incorrectText, setIncorrectText] = useState(false);
  const [questionNumber, setQuestionNumber] = useState(1);
  const [disableGuess, setDisableGuess] = useState(false);

  const checkAnswer = (flag: string) => {
    if (flag === currentCountry.country) {
      setCorrectText(true);
      handleCorrectCount((count) => count + 1);
      setDisableGuess(true);
    } else {
      setIncorrectText(true);
      setDisableGuess(true);
    }
    setTimeout(() => {
      setCorrectText(false);
      setIncorrectText(false);
      setDisableGuess(false);
      setQuestionNumber((count) => count + 1);
      handleCountry();
      handleOptions([]);
    }, 2000);
  };

  const { t } = useTranslate();
  console.log("flag", currentCountry.flag);

  return (
    <div className="w-full flex gap-24 max-sm:gap-6 max-sm:flex-col max-lg:gap-6 h-full justify-center items-center ">
      <div className="w-full max-w-2xl bg-gradient-to-br from-gray-800 to-gray-900 py-8 px-10 max-md:px-6 rounded-2xl shadow-xl z-10 mx-auto max-sm:mt-[320px] max-sm:px-4 max-sm:py-6  ">
        <h2
  className="text-3xl font-bold text-center text-gray-200 mb-6 bg-gradient-to-r from-gray-800 to-gray-900 p-4 rounded-lg shadow-lg max-md:text-md max-md:p-3 max-sm:text-sm max-sm:p-2 max-sm:mb-3"
>

          {t("guess")}
          <br />
          <img
            src={`${currentCountry.flag}`}
            alt={`${currentCountry.country} flag`}
            className="inline-block w-36 h-24 mt-4 rounded-lg shadow-md transition-transform duration-500 ease-in-out hover:rotate-6 max-sm:w-20 max-sm:h-12 max-sm:mt-2"
          />
        </h2>

        <p className="text-lg text-center text-gray-300 mb-5 max-sm:text-base max-sm:mb-2 font-medium">
          {t("question")}{" "}
          <span className="text-yellow-400 font-bold">
            {questionNumber} / 10
          </span>
        </p>

        <div className="grid grid-cols-2 gap-6 mb-4 max-sm:mb-2 max-sm:gap-3">
          {options.map(
            (option: { country: string; flag: string }, index: number) => (
              <button
                key={index}
                onClick={() => checkAnswer(option.country)}
                disabled={disableGuess}
                className="py-3 px-5  rounded-lg bg-gradient-to-r from-gray-700 to-gray-800 hover:from-gray-600 hover:to-gray-700 text-gray-200 font-semibold shadow-lg hover:shadow-xl transition-all duration-300 ease-in-out transform hover:-translate-y-1 focus:outline-none focus:ring-2 focus:ring-blue-400"
              >
                {option.country}
              </button>
            )
          )}
        </div>
      </div>

      <div className="">
        <div className="pb-10 space-y-4 max-w-md mx-auto bg-gradient-to-br from-gray-800 to-gray-900 p-6 rounded-lg shadow-lg mb-10 max-sm:p-3 flex flex-col justify-center items-center">
        <p className="text-lg h-full flex flex-col justify-center items-center  text-center text-gray-200 font-medium">
            {t("your-score")}{" "}
            <span
              className={`text-3xl font-extrabold ${
                score >= 7 ? "text-green-300" : "text-red-400"
              }`}
            >
              {score}
            </span>
          </p>
          <p
            className={`text-2xl font-semibold text-green-400 text-center mb-2 transition-opacity duration-300 ${
              correctText ? "opacity-100" : "opacity-0"
            }`}
          >
            {t("correct")}
          </p>

          <p
            className={`text-2xl font-semibold text-red-400 text-center mb-2 transition-opacity duration-300 ${
              incorrectText ? "opacity-100" : "opacity-0"
            }`}
          >
            {t("incorrect")}
          </p>

          <p
            className={`text-lg text-yellow-400 text-center italic mb-4 transition-opacity duration-300 ${
              incorrectText ? "opacity-100" : "opacity-0"
            }`}
          >
            {t("correct-ans")}{" "}
            <span className="font-bold">{currentCountry.country}</span>
          </p>

          
        </div>
        <img
          src="/flag/countryFlag.png"
          alt="Country Flag"
          className="w-64 animate-spinSlow "
        />
      </div>
    </div>
  );
};

export default Quiz;
