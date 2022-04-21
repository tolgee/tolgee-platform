import { useMemo } from 'react';
import { useCurrentLanguage } from '@tolgee/react';
import { Tooltip, styled } from '@mui/material';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { getTextDiff } from './getTextDiff';
import { getStateChange } from './getStateChange';
import { getMtChange } from './getMtChange';
import { DiffInput } from './types';
import { getAutoChange } from './getAutoChange';
import { LimitedHeightText } from '../LimitedHeightText';

type TranslationHistoryModel = components['schemas']['TranslationHistoryModel'];

const StyledContainer = styled('div')`
  display: grid;
  padding: 7px 12px 7px 12px;
  grid-template-areas:
    'avatar state changes   time          '
    'avatar .     changes   autoIndicator ';
  grid-template-columns: auto auto 1fr auto;
  grid-template-rows: auto 1fr;
  background: transparent;
  transition: background 0.1s ease-out;
  &:hover {
    background: ${({ theme }) => theme.palette.extraLightBackground.main};
    transition: background 0.1s ease-in;
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
  display: grid;
  align-items: center;
`;

const StyledText = styled('div')`
  padding: 0px;
  margin: 0px;

  line-height: 1.2;
  font-family: ${({ theme }) => theme.typography.fontFamily};
`;

const StyledState = styled('div')``;

const StyledAuto = styled('div')`
  grid-area: autoIndicator;
  display: flex;
  font-size: 12px;
  padding-top: 5px;
`;

const StyledTime = styled('div')`
  grid-area: time;
  display: flex;
  font-size: 11px;
  justify-content: flex-end;
`;

type Props = {
  entry: TranslationHistoryModel;
};

export const HistoryItem: React.FC<Props> = ({ entry }) => {
  const lang = useCurrentLanguage();

  const date = new Date(entry.timestamp);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();

  const textChanges = entry.modifications?.['text'] as DiffInput;
  const stateChanges = entry.modifications?.['state'] as DiffInput;
  const mtChanges = entry.modifications?.['mtProvider'] as DiffInput;
  const autoChanges = entry.modifications?.['auto'] as DiffInput<boolean>;

  const textDiff = useMemo(() => getTextDiff(textChanges), [textChanges]);
  const stateDiff = useMemo(() => getStateChange(stateChanges), [stateChanges]);
  const mtProviderDiff = getMtChange(mtChanges) || getAutoChange(autoChanges);

  return textDiff || stateDiff || mtProviderDiff ? (
    <StyledContainer data-cy="translation-history-item">
      <Tooltip title={entry.author?.name || entry.author?.username || ''}>
        <StyledAvatar>
          <AvatarImg
            owner={{
              ...entry.author,
              type: 'USER',
              id: entry.author?.id as number,
            }}
            size={24}
            autoAvatarType="IDENTICON"
            circle
          />
        </StyledAvatar>
      </Tooltip>
      <StyledChanges>
        {textDiff && (
          <LimitedHeightText maxLines={3}>
            <StyledText>{textDiff}</StyledText>
          </LimitedHeightText>
        )}
        {stateDiff && <StyledState>{stateDiff}</StyledState>}
      </StyledChanges>
      {mtProviderDiff && <StyledAuto>{mtProviderDiff}</StyledAuto>}
      <StyledTime>
        {!isToday && date.toLocaleDateString(lang()) + ' '}
        {date.toLocaleTimeString(lang(), {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </StyledTime>
    </StyledContainer>
  ) : null;
};
