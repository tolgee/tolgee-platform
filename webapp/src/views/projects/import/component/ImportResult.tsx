import React, { FunctionComponent, useState } from 'react';
import {
  Box,
  styled,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
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

export const ImportResult: FunctionComponent<ImportResultProps> = (props) => {
  const project = useProject();
  const rows = props.result?._embedded?.languages;
  const [viewFileIssuesRow, setViewFileIssuesRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );
  const [showDataRow, setShowDataRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );

  if (!rows) {
    return null;
  }

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
              {rows.map((row) => (
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
      </Box>
    </ProjectLanguagesProvider>
  );
};
