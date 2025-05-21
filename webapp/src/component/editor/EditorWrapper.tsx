import { Box, BoxProps, styled } from '@mui/material';

const StyledEditorWrapper = styled(Box)`
  border: 1px solid
    ${({ theme }) => (theme.palette.mode === 'dark' ? '#535e6c' : '#bfbfbf')};
  border-radius: 4px;
  cursor: text;
  background: ${({ theme }) => theme.palette.input.background};
  padding: 1px;
  display: grid;

  &:hover {
    border: 1px solid ${({ theme }) => theme.palette.emphasis[900]};
  }

  &:focus-within {
    border-color: ${({ theme }) => theme.palette.primary.main};
    border-width: 2px;
    padding: 0px;
  }

  & > * {
    padding: 8px 10px;
  }
`;

export const EditorWrapper: React.FC<BoxProps> = ({ children, ...props }) => {
  const handleClick: React.MouseEventHandler<HTMLDivElement> = (e) => {
    const editor = (e.target as HTMLDivElement).querySelector('.cm-content') as
      | HTMLDivElement
      | undefined;
    editor?.focus();
  };

  return (
    <StyledEditorWrapper
      onMouseDown={(e) => e.preventDefault()}
      onClick={handleClick}
      {...props}
    >
      {children}
    </StyledEditorWrapper>
  );
};
