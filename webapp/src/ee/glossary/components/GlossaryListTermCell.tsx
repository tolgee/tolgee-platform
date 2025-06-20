import { Checkbox, styled } from '@mui/material';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import clsx from 'clsx';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import Box from '@mui/material/Box';
import { GlossaryTermEditDialog } from 'tg.ee.module/glossary/views/GlossaryTermEditDialog';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { SelectionService } from 'tg.service/useSelectionService';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

type SimpleGlossaryTermWithTranslationsModel =
  components['schemas']['SimpleGlossaryTermWithTranslationsModel'];

const StyledRowTermCell = styled(GlossaryListStyledRowCell)`
  grid-template-areas:
    'checkbox text'
    '.        description'
    '.        tags';
    '..       ..';
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto 1fr;
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  margin-left: ${({ theme }) => theme.spacing(-1.5)};
  margin-top: ${({ theme }) => theme.spacing(-1.5)};
`;

const StyledText = styled(Box)`
  grid-area: text;
  overflow: hidden;
  margin-top: ${({ theme }) => theme.spacing(-0.25)};
  margin-bottom: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledDescription = styled(Box)`
  grid-area: description;
  overflow: hidden;
  margin: ${({ theme }) => theme.spacing(0.5, 0)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

const StyledTags = styled(Box)`
  grid-area: tags;
  margin-top: ${({ theme }) => theme.spacing(0.5)};
  margin-bottom: ${({ theme }) => theme.spacing(0.25)};
`;

type Props = {
  item: SimpleGlossaryTermWithTranslationsModel;
  editEnabled: boolean;
  selectionService: SelectionService<number>;
};

export const GlossaryListTermCell: React.VFC<Props> = ({
  item,
  editEnabled,
  selectionService,
}) => {
  const glossary = useGlossary();
  const [isEditingTerm, setIsEditingTerm] = React.useState(false);

  const baseTranslation = item.translations?.find(
    (t) => t.languageTag === glossary.baseLanguageTag
  );

  return (
    <StyledRowTermCell
      className={clsx({
        clickable: editEnabled,
      })}
      onClick={
        editEnabled && !isEditingTerm ? () => setIsEditingTerm(true) : undefined
      }
      data-cy="glossary-term-list-item"
    >
      <StyledCheckbox
        size="small"
        checked={selectionService.isSelected(item.id)}
        onChange={() => selectionService.toggle(item.id)}
        onClick={(e) => e.stopPropagation()}
        disabled={selectionService.isLoading}
      />
      <StyledText>
        <LimitedHeightText maxLines={3}>
          {baseTranslation?.text}
        </LimitedHeightText>
      </StyledText>
      {item.description && (
        <StyledDescription>
          <LimitedHeightText maxLines={5}>{item.description}</LimitedHeightText>
        </StyledDescription>
      )}
      <StyledTags>
        <GlossaryTermTags term={item} />
      </StyledTags>
      {editEnabled && isEditingTerm && (
        <GlossaryTermEditDialog
          open={isEditingTerm}
          onClose={() => setIsEditingTerm(false)}
          onFinished={() => setIsEditingTerm(false)}
          termId={item.id}
        />
      )}
    </StyledRowTermCell>
  );
};
