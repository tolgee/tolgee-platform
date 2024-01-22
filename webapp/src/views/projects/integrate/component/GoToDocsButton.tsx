import { Button, ButtonProps, styled } from '@mui/material';
import { T } from '@tolgee/react';

const StyledButton = styled(Button)`
  margin-left: ${({ theme }) => theme.spacing(2)};
  &:hover {
    text-decoration: none;
  }
`;

export const GoToDocsButton = (props: ButtonProps & { href: string }) => {
  return (
    <>
      <StyledButton
        // @ts-ignore
        target="_blank"
        size="large"
        color="primary"
        variant="contained"
        style={{ ...props.style }}
        data-cy="integrate-go-to-docs-button"
        {...props}
      >
        <T keyName="integrate_guides_go_to_docs" />
      </StyledButton>
    </>
  );
};
