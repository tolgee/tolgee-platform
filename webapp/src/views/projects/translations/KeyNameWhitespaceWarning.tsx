import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { AlertTriangle } from '@untitled-ui/icons-react';

import { hasOuterWhitespace } from 'tg.fixtures/keyName';

const StyledWarning = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.warning.main};

  & svg {
    flex-shrink: 0;
  }
`;

const StyledMessage = styled('span')`
  flex-grow: 1;
`;

const StyledTrim = styled('button')`
  flex-shrink: 0;
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  font: inherit;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.primary.main};

  &:hover {
    text-decoration: underline;
  }
`;

type Props = {
  value: string;
  onTrim: () => void;
};

export const KeyNameWhitespaceWarning: React.FC<Props> = ({
  value,
  onTrim,
}) => {
  if (!value || !hasOuterWhitespace(value)) {
    return null;
  }

  return (
    <StyledWarning data-cy="key-name-whitespace-warning">
      <AlertTriangle width={16} height={16} />
      <StyledMessage>
        <T
          keyName="key_whitespace_warning_message"
          defaultValue="This key has extra spaces. They can cause duplicate or mismatched keys."
        />
      </StyledMessage>
      <StyledTrim
        type="button"
        data-cy="key-name-whitespace-trim"
        onClick={onTrim}
      >
        <T keyName="key_whitespace_warning_trim" defaultValue="Trim" />
      </StyledTrim>
    </StyledWarning>
  );
};
