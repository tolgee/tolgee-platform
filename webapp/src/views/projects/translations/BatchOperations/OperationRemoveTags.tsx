import { useState } from 'react';
import { ChevronRight } from '@mui/icons-material';
import { Box, styled } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { OperationProps } from './types';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsSelector } from '../context/TranslationsContext';

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  overflow: hidden;
  gap: 4px;
  margin: 6px 6px;
  position: relative;
  max-width: 450px;
`;

const StyledTag = styled(Tag)`
  border-color: ${({ theme }) => theme.palette.error.main};
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
    <Box display="flex" gap="10px">
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
      <LoadingButton
        data-cy="batch-operations-submit-button"
        loading={batchTranslate.isLoading}
        disabled={disabled || tags.length === 0}
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
        onClick={handleSubmit}
        variant="contained"
        color="primary"
      >
        <ChevronRight />
      </LoadingButton>
    </Box>
  );
};
