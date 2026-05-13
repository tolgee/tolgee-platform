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
          title={<T keyName="glossary_empty_placeholder_add_term_title" />}
          description={
            <T keyName="glossary_empty_placeholder_add_term_description" />
          }
          buttonLabel={
            <T keyName="glossary_empty_placeholder_add_term_button" />
          }
          buttonDisabled={!onCreateTerm}
          onClick={() => onCreateTerm?.()}
          footer={
            <Link
              href="https://docs.tolgee.io/platform/glossaries/managing_glossaries"
              sx={{ visibility: 'hidden' }}
            >
              <Typography variant="body2">
                <T keyName="glossary_empty_placeholder_add_term_best_practices" />
              </Typography>
            </Link>
          }
        />
        <EmptyWizardCard
          dataCy="glossary-empty-import-terms-button"
          icon={<UploadCloud02 />}
          title={<T keyName="glossary_empty_placeholder_import_terms_title" />}
          description={
            <T keyName="glossary_empty_placeholder_import_terms_description" />
          }
          buttonLabel={
            <T keyName="glossary_empty_placeholder_import_terms_button" />
          }
          buttonDisabled={!onImport}
          onClick={() => onImport?.()}
          footer={
            <Link href="https://docs.tolgee.io/platform/glossaries/importing_and_exporting_glossaries">
              <Typography variant="body2">
                <T keyName="glossary_empty_placeholder_import_terms_csv_format" />
              </Typography>
            </Link>
          }
        />
      </StyledBox>
    </EmptyState>
  );
};
