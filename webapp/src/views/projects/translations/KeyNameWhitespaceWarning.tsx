import { Link, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { AlertTriangle } from '@untitled-ui/icons-react';

import { hasOuterWhitespace } from 'tg.fixtures/keyName';

const StyledSlot = styled('div')`
  min-height: 1.25rem;
`;

const StyledWarning = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.warning.main};

  & svg {
    flex-shrink: 0;
  }
`;

const StyledMessage = styled('span')`
  flex-grow: 1;
`;

type Props = {
  value: string;
  onTrim: () => void;
};

export const KeyNameWhitespaceWarning: React.FC<Props> = ({
  value,
  onTrim,
}) => {
  return (
    <StyledSlot>
      {Boolean(value) && hasOuterWhitespace(value) && (
        <StyledWarning data-cy="key-name-whitespace-warning">
          <AlertTriangle width={15} height={15} />
          <StyledMessage>
            <T
              keyName="key_whitespace_warning_message"
              defaultValue="This key has extra spaces. They can cause duplicate or mismatched keys."
            />
          </StyledMessage>
          <Link
            component="button"
            type="button"
            data-cy="key-name-whitespace-trim"
            onClick={onTrim}
          >
            <T keyName="key_whitespace_warning_trim" defaultValue="Trim" />
          </Link>
        </StyledWarning>
      )}
    </StyledSlot>
  );
};
