import React, { useState } from 'react';
import { useDebounce } from 'use-debounce';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';
import clsx from 'clsx';
import { DeletableKeyWithTranslationsModelType } from '../context/services/useTranslationsService';
import { CELL_SPACE_BOTTOM, CELL_SPACE_TOP } from '../cell/styles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-width: 1px 0px 0px 0px;
  &.deleted {
    text-decoration: line-through;
    pointer-events: none;
  }
`;

const StyledLanguages = styled('div')`
  display: grid;
  position: relative;
`;

type Props = {
  data: DeletableKeyWithTranslationsModelType;
  languages: LanguageModel[];
  columnSizesPercent: string[];
  columnSizes: number[];
  onResize: (colIndex: number) => void;
  bannerBefore: boolean;
  bannerAfter: boolean;
};

export const RowList: React.FC<Props> = React.memo(function RowList({
  data,
  columnSizesPercent,
  columnSizes,
  languages,
  onResize,
  bannerBefore,
  bannerAfter,
}) {
  const { satisfiesPermission } = useProjectPermissions();
  const [hover, setHover] = useState(false);
  const [focus, setFocus] = useState(false);
  const active = hover || focus;

  const [activeDebounced] = useDebounce(active, 100);

  const relaxedActive = active || activeDebounced;

  const keyClassName = clsx({
    [CELL_SPACE_TOP]: bannerBefore,
    [CELL_SPACE_BOTTOM]: bannerAfter,
  });

  const firstTranslationClassName = clsx({
    [CELL_SPACE_TOP]: bannerBefore,
  });

  const lastTranslationClassName = clsx({
    [CELL_SPACE_BOTTOM]: bannerAfter,
  });

  return (
    <StyledContainer
      onMouseOver={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onFocus={() => setFocus(true)}
      onBlur={() => setFocus(false)}
      data-cy="translations-row"
      className={clsx(data.deleted && 'deleted')}
    >
      <CellKey
        editInDialog
        editEnabled={satisfiesPermission('keys.edit')}
        data={data}
        widthPercent={columnSizesPercent[0]}
        width={columnSizes[0]}
        active={relaxedActive}
        className={keyClassName}
        oneScreenshotBig
      />
      <StyledLanguages style={{ width: columnSizesPercent[1] }}>
        {languages.map((language, index) => {
          return (
            <CellTranslation
              key={language.tag}
              data={data}
              language={language}
              colIndex={0}
              onResize={onResize}
              width={columnSizesPercent[1]}
              active={relaxedActive}
              className={clsx({
                [firstTranslationClassName]: index === 0,
                [lastTranslationClassName]: index === languages.length - 1,
              })}
              // render last focusable button on last item, so it's focusable
              lastFocusable={index === languages.length - 1}
            />
          );
        })}
      </StyledLanguages>
    </StyledContainer>
  );
});
