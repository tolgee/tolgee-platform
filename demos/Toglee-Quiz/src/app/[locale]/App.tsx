"use client";

import { useTranslate } from '@tolgee/react';
import {usePathname} from 'next/navigation'

/* eslint-disable react-hooks/exhaustive-deps */
import React, { useState, useEffect } from 'react';

import Quiz from '@/containers/Quiz';
import Result from '@/components/Result';
import enCountries from '@/data/en-capitals.json';
import frCountries from '@/data/fr-capitals.json';
import hiCountries from '@/data/hi-capitals.json';
import jaCountries from '@/data/ja-capitals.json';
export interface OptionsInterface {
  name: string;
  capital: string;
}

const App = () => {
  const path = usePathname()
  console.log(path)
  let Countries: OptionsInterface[] = enCountries; // Default value for Countries

  // Path-based switch to select the correct countries
  switch (path) {
    case '/':
      Countries = enCountries;
      break;
    case '/hi':
      Countries = hiCountries;
      break;
    case '/ja-JP':
      Countries = jaCountries;
      break;
    case '/fr-FR':
      Countries = frCountries;
      break;
    default:
      Countries = enCountries;
      break;
  }
  const [options, setOptions] = useState<OptionsInterface[]>([]);
  const [currentCountry, setCurrentCountry] = useState<OptionsInterface>(
    Countries[Math.floor(Math.random() * Countries.length)]
  );
  const [countryIds, setCountryIds] = useState<number[]>([]);
  const [correctCount, setCorrectCount] = useState(0);

  const setupCountries = () => {
    const arr = [];
    for (let i = 0; i < 10; i++) {
      arr.push(Math.floor(Math.random() * Countries.length));
    }
    setCountryIds([...countryIds, ...arr]);
    setCorrectCount(0);
  };

  useEffect(() => {
    setupCountries();
  }, []);

  const newCountry = () => {
    setCurrentCountry(Countries[countryIds[0]]);
    const copyOfCountryIds = [...countryIds];
    copyOfCountryIds.shift();
    setCountryIds(copyOfCountryIds);
  };

  useEffect(() => {
    const arr = [];
    for (let i = 0; i < 5; i++) {
      arr.push(Countries[Math.floor(Math.random() * Countries.length)]);
    }
    const duplicatesRemoved = arr.filter(
      (country) => country.capital !== currentCountry.capital
    );
    if (duplicatesRemoved.length < 5) {
      duplicatesRemoved.push(
        Countries[Math.floor(Math.random() * Countries.length)]
      );
    }
    duplicatesRemoved.splice(Math.floor(Math.random() * 4), 0, currentCountry);
    setOptions([...options, ...duplicatesRemoved]);
  }, [currentCountry]);

  const { t } = useTranslate();

  return (
    <div className="App  flex flex-col items-center justify-center p-10 h-[700px] text-white">
      <h1 className="text-4xl font-bold mb-6">{t('quiz-name')}</h1>
      {countryIds.length > 0 ? (
        <Quiz
          options={options}
          handleOptions={setOptions}
          currentCountry={currentCountry}
          handleCountry={newCountry}
          score={correctCount}
          handleCorrectCount={setCorrectCount}
        />
      ) : (
        <Result score={correctCount} restart={setupCountries} />
      )}
    </div>
  );
};

export default App;

