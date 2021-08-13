import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { KeyCell } from './KeyCell';
import { useDebounce } from 'use-debounce/lib';

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
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  languages: LanguageModel[];
  columnSizes: number[];
  editEnabled: boolean;
};

export const ListRow: React.FC<Props> = React.memo(function ListRow({
  data,
  columnSizes,
  editEnabled,
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
      <KeyCell
        editEnabled={editEnabled}
        data={data}
        width={columnSizes[0]}
        active={relaxedActive}
      />
    </div>
  );
});
