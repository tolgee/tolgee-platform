import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';
import { EmptyKeyPlaceholder } from '../cell/EmptyKeyPlaceholder';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];
  return {
    container: {
      display: 'flex',
      border: `1px solid ${borderColor}`,
      borderWidth: '1px 0px 0px 0px',
    },
    languages: {
      display: 'flex',
      flexDirection: 'column',
      position: 'relative',
      alignItems: 'stretch',
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  languages: LanguageModel[];
  columnSizes: number[];
  editEnabled: boolean;
  onResize: (colIndex: number) => void;
};

export const RowTable: React.FC<Props> = React.memo(function RowTable({
  data,
  columnSizes,
  editEnabled,
  languages,
  onResize,
}) {
  const classes = useStyles();
  const [hover, setHover] = useState(false);
  const [focus, setFocus] = useState(false);
  const active = hover || focus;

  const [activeDebounced] = useDebounce(active, 100);

  const relaxedActive = active || activeDebounced;

  return (
    <div
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onFocus={() => setFocus(true)}
      onBlur={() => setFocus(false)}
      className={classes.container}
    >
      <CellKey
        editEnabled={editEnabled}
        data={data}
        width={columnSizes[0]}
        active={relaxedActive}
      />
      {data.keyId < 0 ? (
        <EmptyKeyPlaceholder colIndex={0} onResize={onResize} />
      ) : (
        languages.map((language, index) => (
          <CellTranslation
            key={language.tag}
            data={data}
            language={language}
            colIndex={index}
            onResize={onResize}
            editEnabled={editEnabled}
            width={columnSizes[index + 1]}
            active={relaxedActive}
            // render edit button on last item, so it's focusable
            renderEdit={index === languages.length - 1}
          />
        ))
      )}
    </div>
  );
});
