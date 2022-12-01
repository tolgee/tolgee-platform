import React, { useRef, useState } from 'react';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';
import { styled } from '@mui/material';
import clsx from 'clsx';
import { DeletableKeyWithTranslationsModelType } from '../context/services/useTranslationsService';
import { CELL_SPACE_BOTTOM, CELL_SPACE_TOP } from '../cell/styles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px 0px 0px 0px;
  position: relative;
  &.deleted {
    text-decoration: line-through;
    pointer-events: none;
  }
`;

const StyledFakeContainer = styled('div')`
  display: block;
  position: absolute;
  bottom: 0px;
  right: 0px;
`;

type Props = {
  data: DeletableKeyWithTranslationsModelType;
  languages: LanguageModel[];
  columnSizes: string[];
  onResize: (colIndex: number) => void;
  bannerBefore: boolean;
  bannerAfter: boolean;
};

export const RowTable: React.FC<Props> = React.memo(function RowTable({
  data,
  columnSizes,
  languages,
  onResize,
  bannerBefore,
  bannerAfter,
}) {
  const permissions = useProjectPermissions();
  const [hover, setHover] = useState(false);
  const [focus, setFocus] = useState(false);
  const active = hover || focus;

  const [activeDebounced] = useDebounce(active, 100);

  const relaxedActive = active || activeDebounced;

  const containerRef = useRef<HTMLDivElement>(null);

  const colSizesNum = columnSizes.map((val) => Number(val.replace('%', '')));

  const allClassName = clsx({
    [CELL_SPACE_TOP]: bannerBefore,
    [CELL_SPACE_BOTTOM]: bannerAfter,
  });

  return (
    <StyledContainer
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onFocus={() => setFocus(true)}
      onBlur={() => setFocus(false)}
      data-cy="translations-row"
      className={clsx(data.deleted && 'deleted')}
    >
      <CellKey
        editInDialog
        editEnabled={permissions.satisfiesPermission(
          ProjectPermissionType.EDIT
        )}
        data={data}
        width={columnSizes[0]}
        active={relaxedActive}
        position="left"
        className={allClassName}
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
            editEnabled={permissions.canEditLanguage(language.id)}
            width={columnSizes[index + 1]}
            cellPosition={cellPosition}
            active={relaxedActive}
            // render last focusable button on last item, so it's focusable
            lastFocusable={index === languages.length - 1}
            containerRef={containerRef}
            className={allClassName}
          />
        );
      })}
      <StyledFakeContainer
        ref={containerRef}
        style={{ left: columnSizes[0] }}
      />
    </StyledContainer>
  );
});
