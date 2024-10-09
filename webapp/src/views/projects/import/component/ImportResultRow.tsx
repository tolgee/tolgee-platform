import React from 'react';
import {
  Box,
  Button,
  IconButton,
  styled,
  TableCell,
  TableRow,
} from '@mui/material';
import {
  CheckCircle,
  AlertCircle,
  AlertTriangle,
  Trash01,
  Edit02,
} from '@untitled-ui/icons-react';
import clsx from 'clsx';

import { ChipButton } from 'tg.component/common/buttons/ChipButton';
import { components } from 'tg.service/apiSchema.generated';

import { LanguageSelector } from './LanguageSelector';
import { ImportNamespaceSelector } from './ImportNamespaceSelector';
import { useImportLanguageHelper } from '../hooks/useImportLanguageHelper';

const StyledTableRow = styled(TableRow)`
  &:hover {
    background-color: ${({ theme }) => theme.palette.emphasis['50']};
  }

  & .helperIcon {
    font-size: 20px;
    opacity: 0;
    color: ${({ theme }) => theme.palette.emphasis['500']};
  }

  &:hover .helperIcon {
    opacity: 1;
  }

  & .resolvedIcon {
    font-size: 16px;
    margin-right: 4px;
  }

  & .warningIcon {
    color: ${({ theme }) => theme.palette.warning.main};
  }

  & .resolvedSuccessIcon {
    color: ${({ theme }) => theme.palette.success.main};
  }

  & .resolvedErrorIcon {
    color: ${({ theme }) => theme.palette.error.main};
  }

  & .resolvebutton {
    margin-left: 25px;
    padding-right: calc(25px + ${({ theme }) => theme.spacing(0.5)});
  }

  & .pencil {
    padding-left: ${({ theme }) => theme.spacing(0.5)};
  }
`;

export const ImportResultRow = (props: {
  row: components['schemas']['ImportLanguageModel'];
  onResolveConflicts: () => void;
  onShowFileIssues: () => void;
  onShowData: () => void;
}) => {
  const helper = useImportLanguageHelper(props.row);

  return (
    <React.Fragment>
      <StyledTableRow data-cy="import-result-row">
        <TableCell scope="row" data-cy="import-result-language-menu-cell">
          <LanguageSelector
            value={props.row.existingLanguageId}
            row={props.row}
          />
        </TableCell>
        <TableCell scope="row" data-cy="import-result-namespace-cell">
          <ImportNamespaceSelector row={props.row} />
        </TableCell>
        <TableCell scope="row" data-cy="import-result-file-cell">
          <span>
            {props.row.importFileName} ({props.row.name})
          </span>
          {props.row.importFileIssueCount ? (
            <Box data-cy="import-result-file-warnings">
              <ChipButton
                data-cy="import-file-issues-button"
                onClick={() => {
                  props.onShowFileIssues();
                }}
                beforeIcon={
                  <AlertTriangle
                    className="warningIcon"
                    width={18}
                    height={18}
                  />
                }
              >
                {props.row.importFileIssueCount}
              </ChipButton>
            </Box>
          ) : (
            <></>
          )}
        </TableCell>
        <TableCell
          scope="row"
          align="center"
          data-cy="import-result-total-count-cell"
        >
          <ChipButton
            data-cy="import-result-show-all-translations-button"
            onClick={() => {
              props.onShowData();
            }}
          >
            {props.row.totalCount}
          </ChipButton>
        </TableCell>
        <TableCell
          scope="row"
          align="center"
          data-cy="import-result-resolved-conflicts-cell"
        >
          <Button
            data-cy="import-result-resolve-button"
            disabled={props.row.conflictCount < 1}
            onClick={() => props.onResolveConflicts()}
            size="small"
            className="resolveButton"
          >
            {props.row.resolvedCount < props.row.conflictCount ? (
              <AlertCircle
                className={clsx('resolvedIcon', 'resolvedErrorIcon')}
                width={18}
                height={18}
              />
            ) : (
              <CheckCircle
                className={clsx('resolvedIcon', 'resolvedSuccessIcon')}
                width={18}
                height={18}
              />
            )}
            {props.row.resolvedCount} / {props.row.conflictCount}
            {props.row.conflictCount > 0 && (
              <Edit02
                className={clsx('pencil', 'helperIcon')}
                width={20}
                height={20}
              />
            )}
          </Button>
        </TableCell>
        <TableCell scope="row" align={'right'}>
          <IconButton
            onClick={helper.onDelete}
            data-cy="import-result-delete-language-button"
          >
            <Trash01 width={20} height={20} />
          </IconButton>
        </TableCell>
      </StyledTableRow>
    </React.Fragment>
  );
};
