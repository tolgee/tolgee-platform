import React from "react";
import { IconButton } from '@mui/material';
import Dice from './svgs/Dice'
import Man from './svgs/Man'
import { useTranslate } from '@tolgee/react';

const Header = ({setGameScreen}) => {
  const { t } = useTranslate();

  return (
    <header className="flex flex-col items-center gap-1">
      <h1 className="text-4xl sm:text-5xl font-bold tracking-wide primary-color uppercase">
        {t('lets',"Let's")}
      </h1>
      <p className="flex items-end justify-center tracking-wide text-xl sm:text-4xl font-semibold primary-color mb-8 ">
        {t('play','Play')} <Dice className="-mb-1 animate-bounce" /> {t('and-exercise',"and Exercise")}{" "}
        <Man className="ml-1" />
      </p>
      <IconButton onClick={setGameScreen} sx={{ padding: 0 }}>
        <div className="px-8 py-3 secondary-back white-text font-semibold uppercase text-sm sm:text-base">
          {t('start-game',"Start Game")}
        </div>
      </IconButton>


    </header>
  );
};

export default Header;
