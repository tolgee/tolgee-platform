import clsx from 'clsx';
import { Button, styled } from '@mui/material';
import React from 'react';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/TextField';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import Box from '@mui/material/Box';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';

type GlossaryTermTranslationModel =
  components['schemas']['GlossaryTermTranslationModel'];

const StyledRowTranslationCell = styled(GlossaryListStyledRowCell)`
  grid-template-areas: 'text';

  border-left: 1px solid ${({ theme }) => theme.palette.divider1};

  &.editing {
    z-index: 1;
    background: transparent !important;
    box-shadow: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? '0px 0px 7px rgba(0, 0, 0, 1)'
        : '0px 0px 10px rgba(0, 0, 0, 0.2)'} !important;
  }
`;

const StyledEditBox = styled('div')`
  grid-area: text;
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  flex-flow: column;
`;

const StyledControls = styled('div')`
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  flex-grow: 1;
`;

type Props = {
  organizationId: number;
  glossaryId: number;
  termId: number;
  translation?: GlossaryTermTranslationModel;
  languageCode: string;
  editEnabled: boolean;
  isEditing?: boolean;
  onEdit?: () => void;
  onCancel?: () => void;
  onSave?: () => void;
};

export const GlossaryListTranslationCell: React.VFC<Props> = ({
  organizationId,
  glossaryId,
  termId,
  translation,
  languageCode,
  editEnabled,
  isEditing,
  onEdit,
  onCancel,
  onSave,
}) => {
  const [value, setValue] = React.useState(translation?.text || '');
  const [replacementText, setReplacementText] = React.useState('');

  const isSaveLoading = false;

  const saveMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}/translations',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
  });

  const handleEdit = () => {
    onEdit?.();
    setValue(replacementText || translation?.text || '');
  };

  const save = () => {
    saveMutation.mutate(
      {
        path: { organizationId, glossaryId, termId },
        content: {
          'application/json': {
            languageCode,
            text: value,
          },
        },
      },
      {
        onSuccess: () => {
          setReplacementText(value);
          onSave?.();
        },
      }
    );
  };

  const onHandleEdit = editEnabled && !isEditing ? handleEdit : undefined;

  return (
    <StyledRowTranslationCell
      className={clsx({
        clickable: editEnabled,
        editing: isEditing,
      })}
      onClick={onHandleEdit}
    >
      {!isEditing ? (
        <Box overflow="hidden" gridArea="text">
          <LimitedHeightText maxLines={3}>
            {translation?.text}
          </LimitedHeightText>
        </Box>
      ) : (
        <StyledEditBox>
          <TextField
            onChange={(e) => {
              setValue(e.target.value);
            }}
            value={value}
            multiline
            minRows={3}
            autoFocus
          />
          <StyledControls>
            <Button
              onClick={onCancel}
              color="primary"
              variant="outlined"
              size="small"
            >
              <T keyName="translate_glossary_term_cell_cancel" />
            </Button>
            <LoadingButton
              onClick={save}
              color="primary"
              size="small"
              variant="contained"
              loading={isSaveLoading}
            >
              <T keyName="translate_glossary_term_cell_save" />
            </LoadingButton>
          </StyledControls>
        </StyledEditBox>
      )}
    </StyledRowTranslationCell>
  );
};
