import { useState } from 'react';
import { styled } from '@mui/material';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { OperationProps } from 'tg.views/projects/translations/BatchOperations/types';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { BatchOperationsSubmit } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsSubmit';
import { OperationContainer } from 'tg.views/projects/translations/BatchOperations/components/OperationContainer';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { LabelControl } from 'tg.views/projects/translations/TranslationsList/Label/LabelControl';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';

type LabelModel = components['schemas']['LabelModel'];

const StyledLabels = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  overflow: hidden;
  gap: 6px;
  padding: 6px 6px;
  position: relative;
  max-width: 450px;
`;

type Props = OperationProps;

export const OperationUnassignTranslationLabel = ({
  disabled,
  onStart,
}: Props) => {
  const project = useProject();

  const selection = useTranslationsSelector((c) => c.selection);
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];

  const [labels, setLabels] = useState<LabelModel[]>([]);
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);

  function handleAddTag(label: LabelModel) {
    if (!labels.includes(label)) {
      setLabels([...labels, label]);
    }
  }

  function handleDelete(label: LabelModel) {
    setLabels((labels) => labels.filter((l) => l !== label));
  }

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/unassign-translation-label',
    method: 'post',
  });

  function handleSubmit() {
    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            languageIds: allLanguages
              ?.filter((l) => selectedLanguages?.includes(l.tag))
              .map((l) => l.id),
            labelIds: labels.map((l) => l.id),
          },
        },
      },
      {
        onSuccess(data) {
          onStart(data);
        },
      }
    );
  }

  return (
    <OperationContainer>
      <BatchOperationsLanguagesSelect
        languages={allLanguages || []}
        value={selectedLanguages || []}
        onChange={setSelectedLanguages}
        languagePermission="translations.edit"
      />
      <StyledLabels>
        {labels.map((label) => (
          <TranslationLabel
            key={label.id}
            label={label}
            onDelete={() => handleDelete(label)}
          />
        ))}
        <LabelControl
          onSelect={handleAddTag}
          existing={labels}
          menuAnchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          menuStyle={{ marginLeft: 8 }}
        />
      </StyledLabels>
      <BatchOperationsSubmit
        loading={batchLoadable.isLoading}
        disabled={disabled || labels.length === 0}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
