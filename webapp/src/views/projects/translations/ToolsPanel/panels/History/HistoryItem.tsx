import { useMemo, useState } from 'react';
import { Box, styled, Tooltip } from '@mui/material';
import { DotsVertical } from '@untitled-ui/icons-react';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { getTextDiff } from 'tg.component/activity/types/getTextDiff';
import { getStateChange } from 'tg.component/activity/types/getStateChange';
import { getAutoChange } from 'tg.component/activity/types/getAutoChange';
import { DiffInput } from './HistoryTypes';
import { ActivityDetailDialog } from 'tg.component/activity/ActivityDetail/ActivityDetailDialog';
import { mapHistoryToActivity } from './mapHistoryToActivity';
import { SmallActionButton } from '../../common/SmallActionButton';
import { getNoDiffChange } from 'tg.component/activity/types/getNoDiffChange';
import { UserName } from 'tg.component/common/UserName';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';

type TranslationHistoryModel = components['schemas']['TranslationHistoryModel'];

const StyledContainer = styled('div')`
  display: grid;
  padding: 4px 9px 4px 9px;
  margin: 3px;
  grid-template-areas: 'avatar changes time menu';
  grid-template-columns: auto auto 1fr auto;
  grid-template-rows: auto 1fr;
  background: transparent;
  transition: background 0.1s ease-out;
  border-radius: 8px;

  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[50]};
    transition: background 0.1s ease-in;
  }

  & .hoverVisible {
    opacity: 0;
    transition: opacity 0.1s ease-out;
  }

  &:hover .hoverVisible {
    opacity: 1;
    transition: opacity 0.5s ease-in;
  }
`;

const StyledAvatar = styled('div')`
  grid-area: avatar;
  margin: ${({ theme }) => theme.spacing(0.5, 1, 0.5, 0)};
  align-self: start;
`;

const StyledChanges = styled('div')`
  margin: 7px 4px 4px 0px;
  grid-area: changes;
  overflow: hidden;
`;

const StyledText = styled('div')`
  padding: 0px;
  margin: 0px;

  line-height: 1.2;
  font-family: ${({ theme }) => theme.typography.fontFamily};
`;

const StyledState = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledTime = styled('div')`
  grid-area: time;
  display: flex;
  font-size: 11px;
  justify-content: flex-end;
  white-space: nowrap;
`;

const StyledSmallActionButton = styled(SmallActionButton)`
  color: ${({ theme }) => theme.palette.text.primary};
  height: 26px;
  width: 26px;
`;

type Props = {
  entry: TranslationHistoryModel;
  showDifferences: boolean;
  languageTag: string;
};

export const HistoryItem: React.FC<Props> = ({
  entry,
  showDifferences,
  languageTag,
}) => {
  const lang = useCurrentLanguage();
  const [detailOpen, setDetailOpen] = useState(false);

  const date = new Date(entry.timestamp);

  const textChanges = entry.modifications?.['text'] as DiffInput;
  const stateChanges = entry.modifications?.['state'] as DiffInput;
  const mtChanges = entry.modifications?.['mtProvider'] as DiffInput;
  const autoChanges = entry.modifications?.['auto'] as DiffInput<boolean>;

  const textDiff = useMemo(
    () =>
      showDifferences
        ? getTextDiff(textChanges, languageTag)
        : getNoDiffChange(textChanges, languageTag),
    [textChanges, showDifferences]
  );
  const stateDiff = useMemo(() => getStateChange(stateChanges), [stateChanges]);
  const mtProviderDiff = getAutoChange(mtChanges || autoChanges);

  return textDiff || stateDiff || mtProviderDiff ? (
    <StyledContainer data-cy="translation-history-item">
      <Tooltip title={<UserName {...entry.author} />}>
        <StyledAvatar>
          <AvatarImg
            owner={{
              ...entry.author,
              type: 'USER',
              id: entry.author?.id as number,
            }}
            size={24}
          />
        </StyledAvatar>
      </Tooltip>

      <StyledChanges>
        {textDiff && (
          <LimitedHeightText maxLines={3}>
            <StyledText>{textDiff}</StyledText>
          </LimitedHeightText>
        )}
        {mtProviderDiff ? (
          <StyledText>{mtProviderDiff}</StyledText>
        ) : (
          stateDiff && <StyledState>{stateDiff}</StyledState>
        )}
      </StyledChanges>
      <StyledTime>
        {date.toLocaleTimeString(lang, {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </StyledTime>
      <Box gridArea="menu">
        <StyledSmallActionButton
          className="hoverVisible"
          onClick={() => setDetailOpen(true)}
        >
          <DotsVertical width={20} height={20} color="inherit" />
        </StyledSmallActionButton>
      </Box>
      {detailOpen && (
        <ActivityDetailDialog
          maxWidth="lg"
          open={detailOpen}
          onClose={() => setDetailOpen(false)}
          initialDiffEnabled={showDifferences}
          detailId={0}
          data={mapHistoryToActivity(entry)}
        />
      )}
    </StyledContainer>
  ) : null;
};
