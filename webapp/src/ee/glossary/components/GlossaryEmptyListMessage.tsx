import React, { ComponentProps } from 'react';
import { Box, Button, Card, Link, styled, Typography } from '@mui/material';
import { PlusCircle, UploadCloud02 } from '@untitled-ui/icons-react';
import { EmptyState } from 'tg.component/common/EmptyState';
import { T } from '@tolgee/react';

// Two-column grid with `minmax(0, 1fr)` instead of flex with a fixed 490px card width.
// The grid keeps both cards exactly the same width regardless of inner copy, and the
// `max-width` on the container caps the wizard at roughly the previous footprint
// (~720px) instead of letting the cards stretch across the full content area.
const StyledBox = styled(Box)`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing(2)};
  margin: ${({ theme }) => theme.spacing(2)} auto;
  max-width: 720px;
  text-align: center;

  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr;
    max-width: 490px;
  }
`;

const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border-radius: 20px;
  padding: ${({ theme }) => theme.spacing(4)};
  background-color: ${({ theme }) =>
    theme.palette.tokens.background.onDefaultGrey};
`;

const StyledPlusCircle = styled(PlusCircle)`
  color: ${({ theme }) => theme.palette.primary.light};
  width: 32px;
  height: 32px;
`;

const StyledUploadCloud02 = styled(UploadCloud02)`
  color: ${({ theme }) => theme.palette.primary.light};
  width: 32px;
  height: 32px;
`;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-bottom: ${({ theme }) => theme.spacing(5)};
`;

type Props = {
  loading?: boolean;
  wrapperProps?: ComponentProps<typeof Box>;
  onCreateTerm?: () => void;
  onImport?: () => void;
};

export const GlossaryEmptyListMessage: React.VFC<Props> = ({
  loading,
  wrapperProps,
  onCreateTerm,
  onImport,
}) => {
  return (
    <EmptyState loading={loading} wrapperProps={wrapperProps}>
      <StyledBox>
        <StyledCard elevation={0}>
          <StyledPlusCircle />
          <Typography variant="h4">
            <T keyName="glossary_empty_placeholder_add_term_title" />
          </Typography>
          <StyledDescription variant="body1">
            <T keyName="glossary_empty_placeholder_add_term_description" />
          </StyledDescription>
          <Button
            onClick={onCreateTerm}
            disabled={!onCreateTerm}
            variant="contained"
            color="primary"
            data-cy="glossary-empty-add-term-button"
          >
            <T keyName="glossary_empty_placeholder_add_term_button" />
          </Button>
          <Link
            href="https://docs.tolgee.io/platform/glossaries/managing_glossaries"
            sx={{ visibility: 'hidden' }}
          >
            <Typography variant="body2">
              <T keyName="glossary_empty_placeholder_add_term_best_practices" />
            </Typography>
          </Link>
        </StyledCard>
        <StyledCard elevation={0}>
          <StyledUploadCloud02 />
          <Typography variant="h4">
            <T keyName="glossary_empty_placeholder_import_terms_title" />
          </Typography>
          <StyledDescription variant="body1">
            <T keyName="glossary_empty_placeholder_import_terms_description" />
          </StyledDescription>
          <Button
            onClick={onImport}
            disabled={!onImport}
            variant="contained"
            color="primary"
            data-cy="glossary-empty-import-terms-button"
          >
            <T keyName="glossary_empty_placeholder_import_terms_button" />
          </Button>
          <Link href="https://docs.tolgee.io/platform/glossaries/importing_and_exporting_glossaries">
            <Typography variant="body2">
              <T keyName="glossary_empty_placeholder_import_terms_csv_format" />
            </Typography>
          </Link>
        </StyledCard>
      </StyledBox>
    </EmptyState>
  );
};
