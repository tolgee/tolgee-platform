import { Checkbox, styled } from '@mui/material';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import clsx from 'clsx';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import Box from '@mui/material/Box';
import { GlossaryTermCreateUpdateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateUpdateDialog';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { SelectionService } from 'tg.service/useSelectionService';

type GlossaryTermWithTranslationsModel =
  components['schemas']['GlossaryTermWithTranslationsModel'];

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
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

const StyledDescription = styled(Box)`
  grid-area: description;
  overflow: hidden;
  margin: ${({ theme }) => theme.spacing(0.5, 0)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

type Props = {
  organizationId: number;
  glossaryId: number;
  item: GlossaryTermWithTranslationsModel;
  editEnabled: boolean;
  baseLanguage: string | undefined;
  selectionService: SelectionService<number>;
};

export const GlossaryListTermCell: React.VFC<Props> = ({
  organizationId,
  glossaryId,
  item,
  editEnabled,
  baseLanguage,
  selectionService,
}) => {
  const [isEditingTerm, setIsEditingTerm] = React.useState(false);

  const baseTranslation = item.translations?.find(
    (t) => t.languageTag === baseLanguage
  );

  return (
    <StyledRowTermCell
      className={clsx({
        clickable: editEnabled,
      })}
      onClick={
        editEnabled && !isEditingTerm ? () => setIsEditingTerm(true) : undefined
      }
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
      <GlossaryTermTags term={item} />
      {editEnabled && isEditingTerm && (
        <GlossaryTermCreateUpdateDialog
          open={isEditingTerm}
          onClose={() => setIsEditingTerm(false)}
          onFinished={() => setIsEditingTerm(false)}
          organizationId={organizationId}
          glossaryId={glossaryId}
          editTermId={item.id}
        />
      )}
    </StyledRowTermCell>
  );
};
