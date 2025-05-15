import { styled } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { GlossaryListTranslationCell } from 'tg.ee.module/glossary/components/GlossaryListTranslationCell';
import { GlossaryListTermCell } from 'tg.ee.module/glossary/components/GlossaryListTermCell';
import { SelectionService } from 'tg.service/useSelectionService';
import { T } from '@tolgee/react';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type SimpleGlossaryTermWithTranslationsModel =
  components['schemas']['SimpleGlossaryTermWithTranslationsModel'];

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
  item: SimpleGlossaryTermWithTranslationsModel;
  baseLanguage: string | undefined;
  editingTranslation: [number | undefined, string | undefined];
  onEditTranslation: (termId?: number, languageTag?: string) => void;
  selectedLanguages: string[] | undefined;
  selectionService: SelectionService<number>;
};

export const GlossaryViewListRow: React.VFC<Props> = ({
  item,
  baseLanguage,
  editingTranslation,
  onEditTranslation,
  selectedLanguages,
  selectionService,
}) => {
  const { preferredOrganization } = usePreferredOrganization();

  const editEnabled = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

  const [editingTermId, editingLanguageTag] = editingTranslation;

  return (
    <StyledRow key={item.id}>
      <GlossaryListTermCell
        item={item}
        editEnabled={editEnabled}
        baseLanguage={baseLanguage}
        selectionService={selectionService}
      />
      {selectedLanguages?.map((tag, i) => {
        const realTag = item.flagNonTranslatable ? baseLanguage : tag;
        const translation = item.translations?.find(
          (t) => t.languageTag === realTag
        );
        return (
          <GlossaryListTranslationCell
            key={i + 1}
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
