import React, { ComponentProps } from 'react';
import { Box, Button, Card, Link, styled, Typography } from '@mui/material';
import { PlusCircle, UploadCloud02 } from '@untitled-ui/icons-react';
import { EmptyState } from 'tg.component/common/EmptyState';
import { T } from '@tolgee/react';

const StyledBox = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: center;
  justify-content: center;
  text-align: center;
  margin: ${({ theme }) => theme.spacing(2)};
  flex-wrap: wrap;
`;

const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border-radius: 20px;
  width: 490px;
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
  onCreate?: () => void;
  onImport?: () => void;
};

export const GlossaryEmptyListMessage: React.VFC<Props> = ({
  loading,
  wrapperProps,
  onCreate,
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
            onClick={onCreate}
            disabled={!onCreate}
            variant="contained"
            color="primary"
          >
            <T keyName="glossary_empty_placeholder_add_term_button" />
          </Button>
          <Link href="https://docs.tolgee.io/platform/projects_and_organizations/glossary">
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
          >
            <T keyName="glossary_empty_placeholder_import_terms_button" />
          </Button>
          <Link href="https://docs.tolgee.io/platform/projects_and_organizations/glossary/import/csv-format">
            <Typography variant="body2">
              <T keyName="glossary_empty_placeholder_import_terms_csv_format" />
            </Typography>
          </Link>
        </StyledCard>
      </StyledBox>
    </EmptyState>
  );
};
