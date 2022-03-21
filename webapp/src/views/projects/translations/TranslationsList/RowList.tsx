import React, { useState } from 'react';
import { makeStyles } from '@material-ui/core';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';

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
  columnSizes: string[];
  onResize: (colIndex: number) => void;
};

export const RowList: React.FC<Props> = React.memo(function RowList({
  data,
  columnSizes,
  languages,
  onResize,
}) {
  const permissions = useProjectPermissions();
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
      data-cy="translations-row"
    >
      <CellKey
        editEnabled={permissions.satisfiesPermission(
          ProjectPermissionType.EDIT
        )}
        data={data}
        width={columnSizes[0]}
        active={relaxedActive}
        position="left"
      />
      <div className={classes.languages} style={{ width: columnSizes[1] }}>
        {languages.map((language, index) => (
          <CellTranslation
            key={language.tag}
            data={data}
            language={language}
            colIndex={0}
            onResize={onResize}
            editEnabled={permissions.canEditLanguage(language.id)}
            width={columnSizes[1]}
            active={relaxedActive}
            // render last focusable button on last item, so it's focusable
            lastFocusable={index === languages.length - 1}
          />
        ))}
      </div>
    </div>
  );
});
