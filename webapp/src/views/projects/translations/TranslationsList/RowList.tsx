import React, { useState } from 'react';
import { useDebounce } from 'use-debounce';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { CellKey } from '../CellKey';
import { CellTranslation } from './CellTranslation';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  border: 1px solid ${({ theme }) => theme.palette.grey[200]};
  border-width: 1px 0px 0px 0px;
`;

const StyledLanguages = styled('div')`
  display: flex;
  flex-direction: column;
  position: relative;
  align-items: stretch;
`;

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
  const [hover, setHover] = useState(false);
  const [focus, setFocus] = useState(false);
  const active = hover || focus;

  const [activeDebounced] = useDebounce(active, 100);

  const relaxedActive = active || activeDebounced;

  return (
    <StyledContainer
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onFocus={() => setFocus(true)}
      onBlur={() => setFocus(false)}
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
      <StyledLanguages style={{ width: columnSizes[1] }}>
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
      </StyledLanguages>
    </StyledContainer>
  );
});
