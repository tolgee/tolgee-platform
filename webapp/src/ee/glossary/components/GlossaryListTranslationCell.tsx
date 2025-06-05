import clsx from 'clsx';
import { Button, styled, Tooltip } from '@mui/material';
import React from 'react';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/TextField';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';

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

const StyledPreviewBox = styled('div')`
  grid-area: text;
  overflow: hidden;
  margin-top: ${({ theme }) => theme.spacing(-0.25)};
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
  termId: number;
  translation?: GlossaryTermTranslationModel;
  languageTag: string;
  editEnabled: boolean;
  editDisabledReason?: React.ReactNode;
  isEditing?: boolean;
  onEdit?: () => void;
  onCancel?: () => void;
  onSave?: () => void;
};

export const GlossaryListTranslationCell: React.VFC<Props> = ({
  termId,
  translation,
  languageTag,
  editEnabled,
  editDisabledReason,
  isEditing,
  onEdit,
  onCancel,
  onSave,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const [value, setValue] = React.useState(translation?.text || '');
  const [replacementText, setReplacementText] = React.useState('');

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
        path: {
          organizationId: preferredOrganization!.id,
          glossaryId: glossary.id,
          termId,
        },
        content: {
          'application/json': {
            languageTag: languageTag,
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

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      if (e.shiftKey) {
        return;
      }

      e.preventDefault();
      save();
    }
  };

  return (
    <Tooltip title={!editEnabled && editDisabledReason} placement="bottom">
      <StyledRowTranslationCell
        className={clsx({
          clickable: editEnabled,
          editing: isEditing,
        })}
        onClick={onHandleEdit}
        data-cy="glossary-translation-cell"
      >
        {!isEditing ? (
          <StyledPreviewBox>
            <LimitedHeightText maxLines={3}>
              {translation?.text}
            </LimitedHeightText>
          </StyledPreviewBox>
        ) : (
          <StyledEditBox>
            <TextField
              onChange={(e) => {
                setValue(e.target.value);
              }}
              value={value}
              onKeyDown={handleKeyDown}
              multiline
              minRows={3}
              autoFocus
              data-cy="glossary-translation-edit-field"
            />
            <StyledControls>
              <Button
                onClick={onCancel}
                color="primary"
                variant="outlined"
                size="small"
                data-cy="glossary-translation-cancel-button"
              >
                <T keyName="translate_glossary_term_cell_cancel" />
              </Button>
              <LoadingButton
                onClick={save}
                color="primary"
                size="small"
                variant="contained"
                loading={saveMutation.isLoading}
                data-cy="glossary-translation-save-button"
              >
                <T keyName="translate_glossary_term_cell_save" />
              </LoadingButton>
            </StyledControls>
          </StyledEditBox>
        )}
      </StyledRowTranslationCell>
    </Tooltip>
  );
};
