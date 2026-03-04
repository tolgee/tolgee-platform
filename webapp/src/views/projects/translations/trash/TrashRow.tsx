import React from 'react';
import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationCellReadOnly } from '../TranslationCellReadOnly';
import { TrashKeyCell } from './TrashKeyCell';
import { TrashTrashedCell } from './TrashTrashedCell';

type LanguageModel = components['schemas']['LanguageModel'];
type ScreenshotModel = components['schemas']['ScreenshotModel'];
type TagModel = components['schemas']['TagModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type SimpleUserAccountModel = components['schemas']['SimpleUserAccountModel'];

export type TrashedKeyModel = {
  id: number;
  name: string;
  namespace?: string;
  deletedAt: string;
  permanentDeleteAt: string;
  description?: string;
  tags: TagModel[];
  translations: Record<string, TranslationViewModel>;
  screenshots: ScreenshotModel[];
  isPlural: boolean;
  deletedBy?: SimpleUserAccountModel;
};

const StyledRow = styled('div')`
  display: grid;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  position: relative;
`;

type Props = {
  data: TrashedKeyModel;
  selected: boolean;
  onToggle: () => void;
  onRestore: () => void;
  onDelete: () => void;
  canRestore: boolean;
  canDelete: boolean;
  languages: LanguageModel[];
  columnSizes: string[];
  showNamespace: boolean;
  onFilterNamespace?: (namespace: string) => void;
};

export const TrashRow: React.FC<Props> = React.memo(function TrashRow({
  data,
  selected,
  onToggle,
  onRestore,
  onDelete,
  canRestore,
  canDelete,
  languages,
  columnSizes,
  showNamespace,
  onFilterNamespace,
}) {
  const translations = data.translations ?? {};

  return (
    <StyledRow
      style={{
        gridTemplateColumns: columnSizes.join(' '),
        width: `calc(${columnSizes.join(' + ')})`,
      }}
      data-cy="trash-row"
    >
      <TrashKeyCell
        data={data}
        selected={selected}
        onToggle={onToggle}
        showNamespace={showNamespace}
        onFilterNamespace={onFilterNamespace}
      />

      <TrashTrashedCell
        data={data}
        canRestore={canRestore}
        canDelete={canDelete}
        onRestore={onRestore}
        onDelete={onDelete}
      />

      {languages.map((language) => {
        const translation = translations[language.tag];
        return (
          <TranslationCellReadOnly
            key={language.tag}
            text={translation?.text}
            state={translation?.state}
            locale={language.tag}
            isPlural={data.isPlural ?? false}
          />
        );
      })}
    </StyledRow>
  );
});
