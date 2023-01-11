import { styled } from '@mui/material';

const StyledEditorWrapper = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.emphasis[400]};
  overflow: hidden;
  border-radius: 4px;
  cursor: text;

  &:hover {
    border: 1px solid ${({ theme }) => theme.palette.emphasis[900]};
  }

  &:focus-within {
    border-color: ${({ theme }) => theme.palette.primary.main};
    border-width: 2px;
  }

  & > * {
    padding: 8px 10px;
  }

  &:focus-within > * {
    padding: 7px 9px;
  }
`;

export const EditorWrapper: React.FC = ({ children }) => {
  const handleClick: React.MouseEventHandler<HTMLDivElement> = (e) => {
    const editor = (e.target as HTMLDivElement).querySelector(
      '.CodeMirror-code'
    ) as HTMLDivElement | undefined;
    editor?.focus();
  };

  return (
    <StyledEditorWrapper
      onMouseDown={(e) => e.preventDefault()}
      onClick={handleClick}
    >
      {children}
    </StyledEditorWrapper>
  );
};
