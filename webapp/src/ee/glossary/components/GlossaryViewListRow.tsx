import { styled } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { GlossaryListTranslationCell } from 'tg.ee.module/glossary/components/GlossaryListTranslationCell';
import { GlossaryListTermCell } from 'tg.ee.module/glossary/components/GlossaryListTermCell';
import { SelectionService } from 'tg.service/useSelectionService';
import { T } from '@tolgee/react';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

type SimpleGlossaryTermWithTranslationsModel =
  components['schemas']['SimpleGlossaryTermWithTranslationsModel'];

const StyledRow = styled('div')`
  display: grid;
  position: relative;
  grid-auto-columns: minmax(200px, 1fr);
  grid-auto-flow: column;
  grid-template-columns: minmax(300px, 1fr);

  &.deleted {
    text-decoration: line-through;
    pointer-events: none;
  }
`;

type Props = {
  item: SimpleGlossaryTermWithTranslationsModel;
  editingTranslation: [number | undefined, string | undefined];
  onEditTranslation: (termId?: number, languageTag?: string) => void;
  selectedLanguages: string[] | undefined;
  selectionService: SelectionService<number>;
};

export const GlossaryViewListRow: React.VFC<Props> = ({
  item,
  editingTranslation,
  onEditTranslation,
  selectedLanguages,
  selectionService,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const editEnabled = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

  const [editingTermId, editingLanguageTag] = editingTranslation;

  return (
    <StyledRow key={item.id}>
      <GlossaryListTermCell
        item={item}
        editEnabled={editEnabled}
        selectionService={selectionService}
      />
      {selectedLanguages?.slice(1)?.map((tag) => {
        const realTag = item.flagNonTranslatable
          ? glossary.baseLanguageTag
          : tag;
        const translation = item.translations?.find(
          (t) => t.languageTag === realTag
        );
        return (
          <GlossaryListTranslationCell
            key={`${item.id}-${tag}`}
            termId={item.id}
            translation={translation}
            languageTag={tag}
            editEnabled={editEnabled && !item.flagNonTranslatable}
            editDisabledReason={
              item.flagNonTranslatable && (
                <T keyName="glossary_term_edit_disabled_non_translatable_tooltip" />
              )
            }
            isEditing={editingTermId === item.id && editingLanguageTag === tag}
            onEdit={() => onEditTranslation(item.id, tag)}
            onCancel={() => onEditTranslation(item.id, undefined)}
            onSave={() => onEditTranslation(item.id, undefined)}
          />
        );
      })}
    </StyledRow>
  );
};

export const estimateGlossaryViewListRowHeight = (
  row?: SimpleGlossaryTermWithTranslationsModel
): number => {
  if (!row) {
    return 84;
  }

  const base = 58;
  const tags =
    row.flagNonTranslatable ||
    row.flagAbbreviation ||
    row.flagCaseSensitive ||
    row.flagForbiddenTerm
      ? 25
      : 0;
  const description = row.description ? 26 : 0;

  return base + tags + description;
};
