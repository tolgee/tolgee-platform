import { Alert, Box, Switch, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { LinkExternal } from 'tg.component/LinkExternal';
import { FieldLabel } from 'tg.component/FormField';
import { TextField } from 'tg.component/common/TextField';
import {
  InContextValue,
  isInContextValueValid,
} from 'tg.views/projects/project/components/communityAppUrl';

const IN_CONTEXT_DOCS_URL =
  'https://docs.tolgee.io/platform/translation_process/community-translation-in-context';

type Props = {
  value: InContextValue;
  onChange: (value: InContextValue) => void;
};

export const CommunityInContextContent = ({ value, onChange }: Props) => {
  const { t } = useTranslate();
  const invalidUrl = !isInContextValueValid(value);

  return (
    <Box display="grid" gap={1} data-cy="community-in-context-content">
      <Alert severity="info" icon={false} data-cy="community-in-context-info">
        <Box display="grid" gap={0.5}>
          <span>
            {t(
              'community_in_context_info',
              'Contributors hold the Alt (Option) key and click a text in your app to suggest a translation. Needs the Tolgee SDK.'
            )}
          </span>
          <LinkExternal
            href={IN_CONTEXT_DOCS_URL}
            underline="hover"
            variant="body2"
            sx={{ justifySelf: 'start' }}
            data-cy="community-in-context-docs"
          >
            {t('community_settings_in_context_docs', 'Setup guide')}
          </LinkExternal>
        </Box>
      </Alert>
      <Box mt={1}>
        <Box
          display="flex"
          alignItems="center"
          justifyContent="space-between"
          gap={2}
        >
          <Box display="grid">
            <Typography variant="body2">
              {t('community_in_context_show_banner', 'Show on public page')}
            </Typography>
            <Typography variant="caption" color="text.secondary" fontSize={13}>
              {t(
                'community_in_context_show_banner_hint',
                'Turn it on once your app uses the Tolgee SDK.'
              )}
            </Typography>
          </Box>
          <Switch
            checked={value.communityInContext}
            onChange={(_, communityInContext) =>
              onChange({ ...value, communityInContext })
            }
            data-cy="community-in-context-switch"
          />
        </Box>
      </Box>
      {value.communityInContext && (
        <Box mt={1}>
          <FieldLabel>
            {t('community_in_context_app_url', 'Link to your app (optional)')}
          </FieldLabel>
          <TextField
            value={value.communityAppUrl ?? ''}
            onChange={(e) =>
              onChange({ ...value, communityAppUrl: e.target.value })
            }
            placeholder={t(
              'community_in_context_app_url_placeholder',
              'https://your-app.com'
            )}
            error={invalidUrl}
            helperText={
              invalidUrl
                ? t(
                    'community_in_context_app_url_invalid',
                    'Enter a valid web address (http or https)'
                  )
                : undefined
            }
            fullWidth
            minHeight={false}
            data-cy="community-in-context-app-url"
          />
        </Box>
      )}
    </Box>
  );
};
