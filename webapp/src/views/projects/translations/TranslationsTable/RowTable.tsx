import React, { useRef, useState } from 'react';
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
      position: 'relative',
    },
    fakeContainer: {
      display: 'block',
      position: 'absolute',
      bottom: 0,
      right: 0,
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  languages: LanguageModel[];
  columnSizes: string[];
  onResize: (colIndex: number) => void;
};

export const RowTable: React.FC<Props> = React.memo(function RowTable({
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

  const containerRef = useRef<HTMLDivElement>(null);

  const colSizesNum = columnSizes.map((val) => Number(val.replace('%', '')));

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
      {languages.map((language, index) => {
        const allWidth = 100 - colSizesNum[0];

        const prevWidth = colSizesNum
          .slice(1, index + 1)
          .reduce((prev, cur) => prev + cur, 0);

        const cellWidth = Number(colSizesNum[index + 1]);

        // calculate arrow position for popup
        const cellPosition = `${
          ((prevWidth + cellWidth / 2) / allWidth) * 100
        }%`;

        return (
          <CellTranslation
            key={language.tag}
            data={data}
            language={language}
            colIndex={index}
            onResize={onResize}
            editEnabled={permissions.satisfiesPermission(
              ProjectPermissionType.TRANSLATE
            )}
            width={columnSizes[index + 1]}
            cellPosition={cellPosition}
            active={relaxedActive}
            // render last focusable button on last item, so it's focusable
            lastFocusable={index === languages.length - 1}
            containerRef={containerRef}
          />
        );
      })}
      <div
        className={classes.fakeContainer}
        ref={containerRef}
        style={{ left: columnSizes[0] }}
      />
    </div>
  );
});
