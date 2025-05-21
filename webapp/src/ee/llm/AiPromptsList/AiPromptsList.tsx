import { Box, Button, ListProps, PaperProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { getAiPlaygroundUrl } from 'tg.constants/links';
import { AiPromptItem } from './AiPromptItem';
import { useState } from 'react';
import { Plus } from '@untitled-ui/icons-react';
import { AiPromptsEmptyState } from './AiPromptsEmptyState';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

export const AiPromptsList = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const [page, setPage] = useState(0);
  const promptsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: {
      projectId: project.id,
    },
    query: {
      page,
      sort: ['name'],
    },
  });

  return (
    <Box display="grid" marginTop="32px" gap="20px">
      {promptsLoadable.data?._embedded?.prompts?.length &&
        satisfiesPermission('prompts.edit') && (
          <Box display="flex" justifyContent="end">
            <Button
              color="primary"
              variant="contained"
              component={Link}
              to={getAiPlaygroundUrl(project.id)}
              startIcon={<Plus width={19} height={19} />}
              data-cy="ai-prompts-add-prompt"
            >
              {t('ai_prompts_add')}
            </Button>
          </Box>
        )}
      <PaginatedHateoasList
        onPageChange={setPage}
        loadable={promptsLoadable}
        emptyPlaceholder={<AiPromptsEmptyState />}
        listComponentProps={
          {
            sx: {
              display: 'grid',
              gridTemplateColumns: 'minmax(35%, max-content) 1fr auto',
              alignItems: 'center',
            },
          } as ListProps
        }
        wrapperComponentProps={
          {
            sx: {
              border: 'none',
              background: 'none',
            },
          } as PaperProps
        }
        renderItem={(prompt) => {
          return <AiPromptItem prompt={prompt} />;
        }}
      />
    </Box>
  );
};
