import React, { FunctionComponent, useState } from 'react';
import {
  Box,
  makeStyles,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@material-ui/core';
import { T } from '@tolgee/react';

import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { components } from 'tg.service/apiSchema.generated';

import { ImportFileIssuesDialog } from './ImportFileIssuesDialog';
import { ImportResultRow } from './ImportResultRow';
import { ImportShowDataDialog } from './ImportShowDataDialog';

type ImportResultProps = {
  result?: components['schemas']['PagedModelImportLanguageModel'];
  onLoadData: () => void;
  onResolveRow: (row: components['schemas']['ImportLanguageModel']) => void;
};

const useStyles = makeStyles((theme) => ({
  table: {
    '& th': {
      fontWeight: 'bold',
    },
  },
}));

export const ImportResult: FunctionComponent<ImportResultProps> = (props) => {
  const classes = useStyles();

  const rows = props.result?._embedded?.languages;
  const [viewFileIssuesRow, setViewFileIssuesRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );
  const [showDataRow, setShowDataRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );

  if (!rows) {
    return <></>;
  }

  return (
    <ProjectLanguagesProvider>
      <ImportShowDataDialog
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
          <Table className={classes.table}>
            <TableHead>
              <TableRow>
                <TableCell>
                  <T>import_result_language_name_header</T>
                </TableCell>
                <TableCell>
                  <T>import_result_file_name_header</T>
                </TableCell>
                <TableCell align="center">
                  <T>import_result_total_count_header</T>
                </TableCell>
                <TableCell align="center">
                  <T>import_result_total_conflict_count_header</T>
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
          </Table>
        </TableContainer>
      </Box>
    </ProjectLanguagesProvider>
  );
};
