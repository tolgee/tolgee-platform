import { useState } from 'react';
import { T } from '@tolgee/react';
import { Box, Button, CircularProgress, Typography } from '@mui/material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PanelContentProps } from '../common/types';

export function SourceContext(props: PanelContentProps) {
  return <ContextRows key={props.keyData.keyId} {...props} />;
}

function ContextRows({
  project,
  keyData,
  language,
  baseLanguage,
}: PanelContentProps) {
  const [expanded, setExpanded] = useState(false);
  const radius = expanded ? 10 : 2;
  const result = useApiQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path: { projectId: project.id },
    query: {
      contextKeyId: keyData.keyId,
      contextRadius: radius,
      branch: project.useBranching ? keyData.branch : undefined,
      languages: Array.from(new Set([baseLanguage.tag, language.tag])),
      sort: ['listId', 'keyId'],
      size: radius * 2 + 1,
    },
  });
  if (result.isLoading) return <CircularProgress size={18} />;
  if (result.isError)
    return (
      <Button onClick={() => result.refetch()}>
        <T keyName="context_retry" defaultValue="Retry" />
      </Button>
    );
  const rows = result.data?._embedded?.keys ?? [];
  return (
    <Box px={2} pb={1}>
      {rows.map((row) => (
        <Box
          key={row.keyId}
          py={0.5}
          pl={1}
          sx={{
            borderLeft: 2,
            borderColor:
              row.keyId === keyData.keyId ? 'primary.main' : 'transparent',
            bgcolor:
              row.keyId === keyData.keyId ? 'action.selected' : undefined,
            overflowWrap: 'anywhere',
            whiteSpace: 'pre-wrap',
          }}
        >
          <Button
            size="small"
            title={row.keyName}
            aria-label={row.keyName}
            sx={{ minWidth: 0, mr: 1 }}
            onClick={() => navigator.clipboard.writeText(row.keyName)}
          >
            {row.listId}
          </Button>
          {row.translations[baseLanguage.tag]?.text}
          {baseLanguage.tag !== language.tag && (
            <Typography variant="body2" color="text.secondary">
              {row.translations[language.tag]?.text}
            </Typography>
          )}
        </Box>
      ))}
      {(expanded || rows.length >= 3) && (
        <Button size="small" onClick={() => setExpanded(!expanded)}>
          {expanded ? (
            <T keyName="source_context_less" defaultValue="Show less" />
          ) : (
            <T keyName="source_context_more" defaultValue="Show more context" />
          )}
        </Button>
      )}
    </Box>
  );
}
