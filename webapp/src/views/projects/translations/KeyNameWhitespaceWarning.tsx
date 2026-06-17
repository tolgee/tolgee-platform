import { Link, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { hasOuterWhitespace } from 'tg.fixtures/keyName';

const StyledSlot = styled('div')`
  min-height: 1.25rem;
`;

const StyledWarning = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.warning.main};
`;

const StyledMessage = styled('span')``;

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
