import React, { useMemo } from 'react';
import { Tooltip, Typography } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { GlossaryLink } from 'tg.ee.module/glossary/components/GlossaryLink';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

export const GlossaryLinksList = ({
  glossaries,
  organizationSlug,
  maxDisplay = 3,
}: {
  glossaries: SimpleGlossaryModel[];
  organizationSlug: string;
  maxDisplay?: number;
}) => {
  const sortedGlossaries = useMemo(
    () => [...glossaries].sort((a, b) => a.name.localeCompare(b.name)),
    [glossaries]
  );
  const displayedGlossaries = sortedGlossaries.slice(0, maxDisplay);
  const remainingGlossaries = sortedGlossaries.slice(maxDisplay);
  const hasMore = remainingGlossaries.length > 0;

  const content = (
    <span data-cy="glossary-panel-searched-list">
      {displayedGlossaries.map((glossary, index) => (
        <span key={glossary.id}>
          {index > 0 && ', '}
          <GlossaryLink
            organizationSlug={organizationSlug}
            glossary={glossary}
          />
        </span>
      ))}
      {hasMore && ', ...'}
    </span>
  );

  if (!hasMore) {
    return content;
  }

  return (
    <Tooltip
      placement="bottom-start"
      title={
        <Typography variant="body2" component="span">
          {remainingGlossaries.map((glossary) => (
            <div key={glossary.id}>
              <GlossaryLink
                organizationSlug={organizationSlug}
                glossary={glossary}
              />
            </div>
          ))}
        </Typography>
      }
    >
      <span>{content}</span>
    </Tooltip>
  );
};
