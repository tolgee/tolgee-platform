import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

const StyledHint = styled('div')`
  display: grid;
  gap: 8px;
`;

const StyledMessage = styled('div')`
  font-weight: bold;
  margin-bottom: 10px;
`;

const StyledHintLabel = styled('div')`
  font-weight: bold;
`;

const StyledHintData = styled('div')`
  display: grid;
`;

type Props = {
  scopes: string[];
  customMessage?: string;
};

export function ScopesHint({ scopes, customMessage }: Props) {
  const { t } = useTranslate();

  return (
    <StyledHint>
      <div>
        {customMessage && <StyledMessage>{customMessage}</StyledMessage>}
        <StyledHintLabel>{t('permissions_settings_scopes')}</StyledHintLabel>
        <StyledHintData>
          {scopes.map((scope) => (
            <div key={scope}>{scope}</div>
          ))}
        </StyledHintData>
      </div>
    </StyledHint>
  );
}
