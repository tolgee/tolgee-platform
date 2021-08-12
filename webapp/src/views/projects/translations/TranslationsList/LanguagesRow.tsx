import React from 'react';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { EmptyKeyPlaceholder } from '../cell/EmptyKeyPlaceholder';
import { LanguageCell } from './LanguageCell';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    content: {
      display: 'flex',
      flexDirection: 'column',
      width: '100%',
    },
  };
});

type Props = {
  languages: LanguageModel[];
  data: KeyWithTranslationsModel;
  editEnabled: boolean;
  width: number;
  colIndex: number;
  onResize: (colIndex: number) => void;
};

export const LanguagesRow: React.FC<Props> = React.memo(function Cell({
  languages,
  data,
  editEnabled,
  onResize,
  colIndex,
}) {
  const classes = useStyles();

  if (data.keyId < 0) {
    return <EmptyKeyPlaceholder colIndex={0} onResize={onResize} />;
  }

  return (
    <div className={classes.content}>
      {languages.map((l) => (
        <LanguageCell
          key={l.tag}
          data={data}
          language={l}
          colIndex={colIndex}
          onResize={onResize}
          editEnabled={editEnabled}
        />
      ))}
    </div>
  );
});
