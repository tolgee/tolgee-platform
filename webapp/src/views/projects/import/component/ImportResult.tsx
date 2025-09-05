import React, { FunctionComponent, useState, useEffect } from 'react';
import {
  Box,
  styled,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Pagination,
} from '@mui/material';
import { T } from '@tolgee/react';

import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { components } from 'tg.service/apiSchema.generated';

import { ImportFileIssuesDialog } from './ImportFileIssuesDialog';
import { ImportResultRow } from './ImportResultRow';
import { ImportTranslationsDialog } from './ImportTranslationsDialog';
import { useProject } from 'tg.hooks/useProject';

type ImportResultProps = {
  result?: components['schemas']['PagedModelImportLanguageModel'];
  onLoadData: () => void;
  onResolveRow: (row: components['schemas']['ImportLanguageModel']) => void;
};

const StyledTable = styled(Table)`
  & th {
    font-weight: bold;
  }
`;

const ITEMS_PER_PAGE = 20;

export const ImportResult: FunctionComponent<ImportResultProps> = (props) => {
  const project = useProject();
  const rows = props.result?._embedded?.languages;
  const [viewFileIssuesRow, setViewFileIssuesRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );
  const [showDataRow, setShowDataRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );
  const [currentPage, setCurrentPage] = useState(1);

  // Reset pagination when new data arrives
  useEffect(() => {
    setCurrentPage(1);
  }, [rows?.length]);

  if (!rows || rows.length === 0) {
    return null;
  }

  // Calculate pagination
  const totalItems = rows.length;
  const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const currentPageRows = rows.slice(startIndex, endIndex);

  const handlePageChange = (
    event: React.ChangeEvent<unknown>,
    page: number
  ) => {
    setCurrentPage(page);
  };

  return (
    <ProjectLanguagesProvider>
      <ImportTranslationsDialog
        row={showDataRow}
        onClose={() => setShowDataRow(undefined)}
      />
      <ImportFileIssuesDialog
        onClose={() => {
          setViewFileIssuesRow(undefined);
        }}
        row={viewFileIssuesRow}
      />
      <Box mt={5}>
        <TableContainer>
          <StyledTable>
            <TableHead>
              <TableRow>
                <TableCell>
                  <T keyName="import_result_language_name_header" />
                </TableCell>
                {project.useNamespaces && (
                  <TableCell>
                    <T keyName="import_namespace_name_header" />
                  </TableCell>
                )}
                <TableCell>
                  <T keyName="import_result_file_name_header" />
                </TableCell>
                <TableCell align="center">
                  <T keyName="import_result_total_count_header" />
                </TableCell>
                <TableCell align="center">
                  <T keyName="import_result_total_conflict_count_header" />
                </TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {currentPageRows.map((row) => (
                <ImportResultRow
                  onShowFileIssues={() => setViewFileIssuesRow(row)}
                  onResolveConflicts={() => props.onResolveRow(row)}
                  onShowData={() => setShowDataRow(row)}
                  key={row.id}
                  row={row}
                />
              ))}
            </TableBody>
          </StyledTable>
        </TableContainer>
        {totalPages > 1 && (
          <Box display="flex" justifyContent="flex-end" mt={1} mb={1}>
            <Pagination
              count={totalPages}
              page={currentPage}
              onChange={handlePageChange}
              color="primary"
              data-cy="import-result-pagination"
            />
          </Box>
        )}
      </Box>
    </ProjectLanguagesProvider>
  );
};
