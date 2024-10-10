import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { AutoTranslationIcon } from 'tg.component/AutoTranslationIcon';
import { DiffValue } from '../types';

const StyledWrapper = styled('div')`
  & > div {
    display: inline-flex;
    vertical-align: text-bottom;
    align-items: flex-end;
    position: relative;
    top: -2px;
  }
`;

const StyledRemoved = styled(StyledWrapper)`
  text-decoration: line-through;
`;

const getContent = (auto: boolean | string) => {
  const provider = typeof auto === 'string' ? auto : undefined;
  return (
    <>
      {provider ? (
        <T
          keyName="translations_auto_translated_provider"
          params={{
            provider: provider,
          }}
        />
      ) : (
        <T keyName="translations_auto_translated_tm" />
      )}{' '}
      <AutoTranslationIcon provider={provider} noTooltip />
    </>
  );
};

export const getAutoChange = (input?: DiffValue<boolean | string>) => {
  if (input?.new) {
    return <StyledWrapper>{getContent(input.new)}</StyledWrapper>;
  } else if (input?.old) {
    return <StyledRemoved>{getContent(input.old)}</StyledRemoved>;
  }
};
