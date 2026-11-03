import { useState } from 'react';
import { Box, Switch, Tab, Tabs, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { FieldLabel } from 'tg.component/FormField';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';
import { LINKS, PARAMS } from 'tg.constants/links';

const BADGE_URL = 'https://docs.tolgee.io/img/badges/translate-with-tolgee.svg';

type Format = 'markdown' | 'html';

type Props = {
  projectId: number;
  shared: boolean;
  onSharedChange: (shared: boolean) => void;
};

export const CommunityShareContent = ({
  projectId,
  shared,
  onSharedChange,
}: Props) => {
  const { t } = useTranslate();
  const [format, setFormat] = useState<Format>('markdown');

  const pageUrl =
    window.location.origin +
    LINKS.PROJECT_COMMUNITY.build({ [PARAMS.PROJECT_ID]: projectId });

  const snippets: Record<Format, string> = {
    markdown: `[![Translate with Tolgee](${BADGE_URL})](${pageUrl})`,
    html: `<a href="${pageUrl}" target="_blank" rel="noopener"><img src="${BADGE_URL}" alt="Translate with Tolgee" height="28"></a>`,
  };

  return (
    <Box display="grid" gap={3}>
      <Box>
        <FieldLabel>
          {t('community_share_link_label', 'Project page link')}
        </FieldLabel>
        <ClipboardCopyInput
          value={pageUrl}
          inputProps={{ 'data-cy': 'community-share-link' }}
        />
      </Box>
      <Box>
        <FieldLabel>{t('community_share_badge_label', 'Badge')}</FieldLabel>
        <Box
          p={2}
          mb={1}
          borderRadius="8px"
          bgcolor="emphasis.50"
          display="flex"
          justifyContent="center"
        >
          <img src={BADGE_URL} alt="Translate with Tolgee" height={28} />
        </Box>
        <Tabs
          value={format}
          onChange={(_, value: Format) => setFormat(value)}
          sx={{ mb: 1 }}
        >
          <Tab
            value="markdown"
            label={t('community_share_markdown', 'Markdown')}
            data-cy="community-share-tab-markdown"
          />
          <Tab
            value="html"
            label={t('community_share_html', 'HTML')}
            data-cy="community-share-tab-html"
          />
        </Tabs>
        <ClipboardCopyInput
          value={snippets[format]}
          inputProps={{ 'data-cy': 'community-share-snippet' }}
        />
      </Box>
      <Box
        display="flex"
        alignItems="center"
        justifyContent="space-between"
        gap={2}
      >
        <Box display="grid">
          <Typography variant="body2">
            {t('community_share_mark_shared', 'Mark as shared')}
          </Typography>
          <Typography variant="caption" color="text.secondary" fontSize={13}>
            {t(
              'community_share_mark_shared_hint',
              'Your team will see it’s shared.'
            )}
          </Typography>
        </Box>
        <Switch
          checked={shared}
          onChange={(_, value) => onSharedChange(value)}
          data-cy="community-share-mark-shared"
        />
      </Box>
    </Box>
  );
};
