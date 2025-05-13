import {
  Box,
  Button,
  Typography,
  useTheme,
  Link as MuiLink,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import PromptImage from 'tg.svgs/prompts/promptImage.svg?react';
import { EmptyState } from 'tg.component/common/EmptyState';
import { getAiPlaygroundUrl } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

export const AiPromptsEmptyState = () => {
  const { t } = useTranslate();
  const theme = useTheme();
  const project = useProject();
  return (
    <EmptyState>
      <Box display="grid" justifyItems="center" gap={2} textAlign="center">
        <Typography variant="h4">{t('ai_prompts_empty_message')}</Typography>
        <Typography color={theme.palette.text.secondary}>
          <T
            keyName="ai_prompts_empty_description"
            params={{
              link: <MuiLink href="https://docs.tolgee.io/" target="_blank" />,
            }}
          />
        </Typography>
        <Box color={theme.palette.tokens.state.untranslated} mb={2}>
          <PromptImage />
        </Box>
        <Button
          component={Link}
          to={getAiPlaygroundUrl(project.id)}
          color="primary"
          variant="contained"
        >
          {t('ai_prompts_open_playground_label')}
        </Button>
      </Box>
    </EmptyState>
  );
};
