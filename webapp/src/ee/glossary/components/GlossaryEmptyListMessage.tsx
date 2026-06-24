import React, { ComponentProps } from 'react';
import { Box, Link, styled, Typography } from '@mui/material';
import { PlusCircle, UploadCloud02 } from '@untitled-ui/icons-react';
import { EmptyState } from 'tg.component/common/EmptyState';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';
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
        <EmptyWizardCard
          dataCy="glossary-empty-add-term-button"
          icon={<PlusCircle />}
          title={
            <T
              keyName="glossary_empty_placeholder_add_term_title"
              defaultValue="Add a term"
            />
          }
          description={
            <T
              keyName="glossary_empty_placeholder_add_term_description"
              defaultValue="Define a term and its translations so the editor can suggest them consistently."
            />
          }
          buttonLabel={
            <T
              keyName="glossary_empty_placeholder_add_term_button"
              defaultValue="Add term"
            />
          }
          buttonDisabled={!onCreateTerm}
          onClick={() => onCreateTerm?.()}
          footer={
            <Link
              href="https://docs.tolgee.io/platform/glossaries/managing_glossaries"
              sx={{ visibility: 'hidden' }}
            >
              <Typography variant="body2">
                <T
                  keyName="glossary_empty_placeholder_add_term_best_practices"
                  defaultValue="Best practices for glossary terms"
                />
              </Typography>
            </Link>
          }
        />
        <EmptyWizardCard
          dataCy="glossary-empty-import-terms-button"
          icon={<UploadCloud02 />}
          title={
            <T
              keyName="glossary_empty_placeholder_import_terms_title"
              defaultValue="Import terms"
            />
          }
          description={
            <T
              keyName="glossary_empty_placeholder_import_terms_description"
              defaultValue="Bulk-load terms from a CSV file you already maintain elsewhere."
            />
          }
          buttonLabel={
            <T
              keyName="glossary_empty_placeholder_import_terms_button"
              defaultValue="Import CSV"
            />
          }
          buttonDisabled={!onImport}
          onClick={() => onImport?.()}
          footer={
            <Link href="https://docs.tolgee.io/platform/glossaries/importing_and_exporting_glossaries">
              <Typography variant="body2">
                <T
                  keyName="glossary_empty_placeholder_import_terms_csv_format"
                  defaultValue="CSV format reference"
                />
              </Typography>
            </Link>
          }
        />
      </StyledBox>
    </EmptyState>
  );
};
