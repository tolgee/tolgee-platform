import { useState } from 'react';
import { styled } from '@mui/material';

import { OperationProps } from './types';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  overflow: hidden;
  gap: 4px;
  padding: 6px;
  position: relative;
  max-width: 450px;
`;

const StyledTag = styled(Tag)`
  border-color: ${({ theme }) => theme.palette.primary.main};
`;

type Props = OperationProps;

export const OperationRemoveTags = ({ disabled, onStart }: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  const selection = useTranslationsSelector((c) => c.selection);

  const [tags, setTags] = useState<string[]>([]);

  function handleAddTag(tag: string) {
    if (!tags.includes(tag)) {
      setTags([...tags, tag]);
    }
  }

  function handleDelete(tag: string) {
    setTags((tags) => tags.filter((t) => t !== tag));
  }

  const batchTranslate = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/untag-keys',
    method: 'post',
  });

  function handleSubmit() {
    batchTranslate.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            tags,
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
      <StyledTags>
        {tags.map((tag) => (
          <StyledTag key={tag} name={tag} onDelete={() => handleDelete(tag)} />
        ))}
        <TagInput
          onAdd={handleAddTag}
          placeholder={t('batch_operation_tag_remove_input_placeholder')}
          noNew
          filtered={tags}
        />
      </StyledTags>
      <BatchOperationsSubmit
        loading={batchTranslate.isLoading}
        disabled={disabled || tags.length === 0}
        onClick={handleSubmit}
      />
    </OperationContainer>
  );
};
