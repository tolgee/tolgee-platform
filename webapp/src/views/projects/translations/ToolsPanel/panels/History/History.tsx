import React, { useState } from 'react';
import { T } from '@tolgee/react';
import { Box, FormControlLabel, styled, Switch } from '@mui/material';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';
import { StickyDateSeparator } from 'tg.views/projects/translations/ToolsPanel/common/StickyDateSeparator';

import { HistoryItem } from './HistoryItem';
import { PanelContentProps } from '../../common/types';
import { TabMessage } from '../../common/TabMessage';
import { useHistory } from './useHistory';
import {
  StyledLoadMore,
  StyledLoadMoreButton,
} from '../../common/StyledLoadMore';
import { arraySplit } from '../../common/splitByParameter';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  flex-basis: 100px;
  position: relative;
  margin-top: 4px;
`;

const StyledDifferenceToggle = styled(FormControlLabel)`
  position: absolute;
  right: 24px;
  top: 2px;
  z-index: 2;
  display: flex;
  align-items: start;
  & > span {
    font-size: 14px;
  }
`;

export const History: React.FC<PanelContentProps> = ({ keyData, language }) => {
  const translation = keyData.translations[language.tag];
  const [limit, setLimit] = useState(true);

  const { fetchMore, historyItems, loading, ...history } = useHistory({
    keyId: keyData.keyId,
    translation,
    language,
  });

  function handleShowMore() {
    if (limit) {
      setLimit(false);
    } else {
      fetchMore();
    }
  }

  const [showdifferences, setShowDifferences] = useState(true);
  const toggleDifferences = () => setShowDifferences((val) => !val);

  const trimmedHistory = limit ? historyItems.slice(-4) : historyItems;
  const showLoadMore =
    history.hasNextPage || (limit && historyItems.length > 4);

  if (!trimmedHistory.length) {
    return (
      <StyledContainer>
        <TabMessage>
          {loading ? (
            <LoadingSkeletonFadingIn variant="text" />
          ) : (
            <T keyName="translations_history_no_activity"></T>
          )}
        </TabMessage>
      </StyledContainer>
    );
  }

  const dayGroups = arraySplit(trimmedHistory, (i) =>
    new Date(i.timestamp).toLocaleDateString()
  );

  return (
    <StyledContainer>
      <StyledDifferenceToggle
        label={<T keyName="translations-history-differences-toggle" />}
        labelPlacement="start"
        control={
          <Switch
            size="small"
            checked={showdifferences}
            onChange={toggleDifferences}
          />
        }
      />
      {dayGroups.map((items, gIndex) => (
        <Box key={gIndex} display="grid">
          <StickyDateSeparator date={new Date(items[0].timestamp)} />
          {items?.map((entry, cIndex) => {
            return (
              <React.Fragment key={entry.timestamp}>
                {showLoadMore && cIndex + gIndex === 0 && (
                  <StyledLoadMore>
                    <StyledLoadMoreButton
                      role="button"
                      onClick={handleShowMore}
                      data-cy="translations-history-load-more-button"
                    >
                      <T keyName="translations_history_previous_items" />
                    </StyledLoadMoreButton>
                  </StyledLoadMore>
                )}
                <HistoryItem
                  languageTag={language.tag}
                  key={entry.timestamp}
                  entry={entry}
                  showDifferences={showdifferences}
                />
              </React.Fragment>
            );
          })}
        </Box>
      ))}
    </StyledContainer>
  );
};
