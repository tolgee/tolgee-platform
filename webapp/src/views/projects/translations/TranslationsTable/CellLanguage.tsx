import React from 'react';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { CellStateBar } from '../cell/CellStateBar';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles({
  container: {
    flexGrow: 1,
    position: 'relative',
  },
  content: {
    display: 'flex',
    alignItems: 'center',
    padding: '8px 12px',
    flexShrink: 0,
    '& > * + *': {
      marginLeft: '5px',
    },
  },
});

type Props = {
  language: LanguageModel;
  colIndex: number;
  onResize: (colIndex: number) => void;
};

export const CellLanguage: React.FC<Props> = ({
  language,
  onResize,
  colIndex,
}) => {
  const classes = useStyles();
  const handleResize = () => onResize(colIndex);
  return (
    <>
      <div className={classes.content}>
        <CircledLanguageIcon flag={language.flagEmoji} />
        <div>{language.name}</div>
      </div>
      <CellStateBar onResize={handleResize} />
    </>
  );
};
