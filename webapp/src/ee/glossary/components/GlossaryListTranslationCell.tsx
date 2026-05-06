import clsx from 'clsx';
import { styled, Tooltip } from '@mui/material';
import React from 'react';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { EditableTextCellForm } from 'tg.component/entriesList/EditableTextCellForm';
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

  return (
    <Tooltip title={!editEnabled && editDisabledReason} placement="bottom">
      <StyledRowTranslationCell
        className={clsx({
          hoverable: editEnabled || editDisabledReason,
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
            <EditableTextCellForm
              value={value}
              onChange={setValue}
              onSave={save}
              onCancel={() => onCancel?.()}
              saving={saveMutation.isLoading}
              minRows={3}
              fieldDataCy="glossary-translation-edit-field"
              cancelDataCy="glossary-translation-cancel-button"
              saveDataCy="glossary-translation-save-button"
              cancelLabel={<T keyName="translate_glossary_term_cell_cancel" />}
              saveLabel={<T keyName="translate_glossary_term_cell_save" />}
            />
          </StyledEditBox>
        )}
      </StyledRowTranslationCell>
    </Tooltip>
  );
};
