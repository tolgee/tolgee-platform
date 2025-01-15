import React, { useRef, useState } from 'react';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';
import { styled } from '@mui/material';
import clsx from 'clsx';
import { DeletableKeyWithTranslationsModelType } from '../context/services/useTranslationsService';
import { CELL_SPACE_BOTTOM, CELL_SPACE_TOP } from '../cell/styles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
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
  const { satisfiesPermission } = useProjectPermissions();
  const [hover, setHover] = useState(false);
  const [focus, setFocus] = useState(false);
  const active = hover || focus;

  const [activeDebounced] = useDebounce(active, 100);

  const relaxedActive = active || activeDebounced;

  const containerRef = useRef<HTMLDivElement>(null);

  const allClassName = clsx({
    [CELL_SPACE_TOP]: bannerBefore,
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
      style={{
        gridTemplateColumns: columnSizes.join(' '),
        width: `calc(${columnSizes.join(' + ')})`,
      }}
    >
      <CellKey
        editInDialog
        editEnabled={satisfiesPermission('keys.edit')}
        data={data}
        active={relaxedActive}
        className={allClassName}
      />
      {languages.map((language, index) => {
        return (
          <CellTranslation
            key={language.tag}
            data={data}
            language={language}
            colIndex={index}
            onResize={onResize}
            active={relaxedActive}
            // render last focusable button on last item, so it's focusable
            lastFocusable={index === languages.length - 1}
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
