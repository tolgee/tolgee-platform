import React, { useState } from 'react';
import Guess from '@/components/Guess';
import { OptionsInterface } from '@/app/[locale]/App';
import { useTranslate } from '@tolgee/react';

interface CountryInterface {
  name: string;
  capital: string;
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

  const checkAnswer = (capital: string) => {
    if (capital === currentCountry.capital) {
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

  return (
    <div className="w-full max-w-2xl bg-gray-800 py-6 px-10 rounded-xl shadow-lg z-1">
      <h2 className="text-2xl font-semibold text-center mb-3 text-white bg-gradient-to-r from-blue-600 to-purple-600 p-4 rounded-lg">
        {t('guess')} {currentCountry.name} ?
      </h2>

      <p className="text-lg text-center text-gray-300 mb-3">
        {t('question')} {questionNumber} / 10
      </p>

      <div className="grid grid-cols-2 gap-4 mb-4">
        {options.map((option: { name: string; capital: string }, index: number) => (
          <Guess
            key={index}
            capital={option.capital}
            handleAnswer={checkAnswer}
            disableGuess={disableGuess}
          />
        ))}
      </div>

      <p
        className={`text-lg text-green-500 text-center mb-2 transition-all ${
          correctText ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {t('correct')}
      </p>

      <p
        className={`text-lg text-red-500 text-center mb-2 transition-all ${
          incorrectText ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {t('incorrect')}
      </p>

      <p
        className={`text-lg text-center text-yellow-500 mb-4 transition-all ${
          incorrectText ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {t('correct-ans')} {currentCountry.capital}
      </p>

      <p className="text-lg text-center text-white">
        {t('your-score')} {' '}
        <span className={`font-bold ${score >= 7 ? 'text-green-400' : 'text-red-400'}`}>
          {score}
        </span>
      </p>
    </div>
  );
};

export default Quiz;
