import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { AutoTranslationIcon } from 'tg.component/AutoTranslationIcon';
import { DiffValue } from '../types';
import { useServiceName } from 'tg.hooks/useServiceName';
import { ServiceType } from 'tg.views/projects/languages/MachineTranslation/types';

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

type Props = {
  auto: string | true;
};

const AutoChange = ({ auto }: Props) => {
  const provider = typeof auto === 'string' ? (auto as ServiceType) : undefined;
  const getServiceName = useServiceName();
  const providerName = provider && getServiceName(provider);

  return (
    <>
      {provider ? (
        <T
          keyName="translations_auto_translated_provider"
          params={{
            provider: providerName,
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
    return (
      <StyledWrapper>
        <AutoChange auto={input.new} />
      </StyledWrapper>
    );
  } else if (input?.old) {
    return (
      <StyledRemoved>
        <AutoChange auto={input.old} />
      </StyledRemoved>
    );
  }
};
