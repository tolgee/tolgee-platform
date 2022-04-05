import React from 'react';
import {
  Box,
  Button,
  IconButton,
  styled,
  TableCell,
  TableRow,
} from '@mui/material';
import { CheckCircle, Error, Warning } from '@mui/icons-material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { container } from 'tsyringe';

import { ChipButton } from 'tg.component/common/buttons/ChipButton';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportRowLanguageMenu } from './ImportRowLanguageMenu';

const StyledTableRow = styled(TableRow)`
  &:hover {
    background-color: ${({ theme }) => theme.palette.grey['50']};
  }
  & .helperIcon {
    font-size: 20px;
    opacity: 0;
    color: ${({ theme }) => theme.palette.grey['500']};
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
  & .resolvebutton: {
    margin-left: 25px;
    padding-right: calc(25px + ${({ theme }) => theme.spacing(0.5)});
  }
  & .pencil: {
    position: absolute;
    right: ${({ theme }) => theme.spacing(0.5)};
  }
`;

const actions = container.resolve(ImportActions);
export const ImportResultRow = (props: {
  row: components['schemas']['ImportLanguageModel'];
  onResolveConflicts: () => void;
  onShowFileIssues: () => void;
  onShowData: () => void;
}) => {
  const project = useProject();

  const deleteLanguage = () => {
    confirmation({
      onConfirm: () =>
        actions.loadableActions.deleteLanguage.dispatch({
          path: {
            languageId: props.row.id,
            projectId: project.id,
          },
        }),
      title: <T>import_delete_language_dialog_title</T>,
      message: (
        <T parameters={{ languageName: props.row.name }}>
          import_delete_language_dialog_message
        </T>
      ),
    });
  };

  return (
    <React.Fragment>
      <StyledTableRow data-cy="import-result-row">
        <TableCell scope="row" data-cy="import-result-language-menu-cell">
          <ImportRowLanguageMenu
            value={props.row.existingLanguageId}
            importLanguageId={props.row.id}
          />
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
                beforeIcon={<Warning className="warningIcon" />}
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
              <Error className={clsx('resolvedIcon', 'resolvedErrorIcon')} />
            ) : (
              <CheckCircle
                className={clsx('resolvedIcon', 'resolvedSuccessIcon')}
              />
            )}
            {props.row.resolvedCount} / {props.row.conflictCount}
            {props.row.conflictCount > 0 && (
              <EditIcon className={clsx('pencil', 'helperIcon')} />
            )}
          </Button>
        </TableCell>
        <TableCell scope="row" align={'right'}>
          <IconButton
            onClick={deleteLanguage}
            size="small"
            style={{ padding: 0 }}
            data-cy="import-result-delete-language-button"
          >
            <DeleteIcon />
          </IconButton>
        </TableCell>
      </StyledTableRow>
    </React.Fragment>
  );
};
