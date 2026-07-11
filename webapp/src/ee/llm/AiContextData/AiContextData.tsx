import { Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { BoxLoading } from 'tg.component/common/BoxLoading';

import { AiLanguagesTable } from './AiLanguagesTable';
import { AiProjectDescription } from './AiProjectDescription';

export const AiContextData = () => {
  const project = useProject();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
  });

  const descriptionLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-prompt-customization',
    method: 'get',
    path: { projectId: project.id },
  });

  const languageDescriptionsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/language-ai-prompt-customizations',
    method: 'get',
    path: { projectId: project.id },
  });

  if (
    languagesLoadable.isLoading ||
    descriptionLoadable.isLoading ||
    languageDescriptionsLoadable.isLoading
  ) {
    return <BoxLoading />;
  }

  const languages =
    languagesLoadable.data?._embedded?.languages?.filter(({ base }) => !base) ||
    [];

  return (
    <Box>
      <Box mt={4} mb={3} gap={3} display="grid">
        <Typography variant="h5">
          <T keyName="ai_customization_title" />
        </Typography>
        <Typography>
          <T keyName="ai_customization_description" />
        </Typography>
        <Typography variant="h6">
          <T keyName="project_ai_prompt_title" />
        </Typography>

        <AiProjectDescription
          description={descriptionLoadable.data?.description}
        />

        {languages.length > 0 && (
          <>
            <Typography variant="h6">
              <T keyName="language_ai_prompts_title" />
            </Typography>

            <AiLanguagesTable
              languages={languagesLoadable.data?._embedded?.languages || []}
              descriptions={
                languageDescriptionsLoadable.data?._embedded
                  ?.promptCustomizations || []
              }
            />
          </>
        )}
      </Box>
    </Box>
  );
};
