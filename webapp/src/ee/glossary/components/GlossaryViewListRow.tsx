import { styled } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { GlossaryListTranslationCell } from 'tg.ee.module/glossary/components/GlossaryListTranslationCell';
import { GlossaryListTermCell } from 'tg.ee.module/glossary/components/GlossaryListTermCell';

type GlossaryTermWithTranslationsModel =
  components['schemas']['GlossaryTermWithTranslationsModel'];

const StyledRow = styled('div')`
  display: grid;
  position: relative;
  grid-auto-columns: minmax(350px, 1fr);
  grid-auto-flow: column;

  &.deleted {
    text-decoration: line-through;
    pointer-events: none;
  }
`;

type Props = {
  organizationId: number;
  glossaryId: number;
  item: GlossaryTermWithTranslationsModel;
  baseLanguage: string | undefined;
  editingTranslation: [number | undefined, string | undefined];
  onEditTranslation: (termId?: number, languageCode?: string) => void;
  selectedLanguages: string[] | undefined;
  checked: boolean;
  onCheckedToggle: () => void;
};

export const GlossaryViewListRow: React.VFC<Props> = ({
  organizationId,
  glossaryId,
  item,
  baseLanguage,
  editingTranslation,
  onEditTranslation,
  selectedLanguages,
  checked,
  onCheckedToggle,
}) => {
  const editEnabled = true; // TODO: Permissions handling
  const [editingTermId, editingLanguageCode] = editingTranslation;

  return (
    <StyledRow key={item.id}>
      <GlossaryListTermCell
        item={item}
        editEnabled={editEnabled}
        baseLanguage={baseLanguage}
        checked={checked}
        onCheckedToggle={onCheckedToggle}
      />
      {selectedLanguages?.map((tag, i) => {
        const translation = item.translations?.find(
          (t) => t.languageCode === tag
        );
        return (
          <GlossaryListTranslationCell
            key={i + 1}
            organizationId={organizationId}
            glossaryId={glossaryId}
            termId={item.id}
            translation={translation}
            languageCode={tag}
            editEnabled={editEnabled}
            isEditing={editingTermId === item.id && editingLanguageCode === tag}
            onEdit={() => onEditTranslation(item.id, tag)}
            onCancel={() => onEditTranslation(item.id, undefined)}
            onSave={() => onEditTranslation(item.id, undefined)}
          />
        );
      })}
    </StyledRow>
  );
};
