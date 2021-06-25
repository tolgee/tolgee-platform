import React from 'react';
import {
  Box,
  Button,
  IconButton,
  makeStyles,
  TableCell,
  TableRow,
} from '@material-ui/core';
import { CheckCircle, Error, Warning } from '@material-ui/icons';
import DeleteIcon from '@material-ui/icons/Delete';
import EditIcon from '@material-ui/icons/Edit';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { container } from 'tsyringe';

import { ChipButton } from 'tg.component/common/buttons/ChipButton';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportRowLanguageMenu } from './ImportRowLanguageMenu';

const useStyles = makeStyles((theme) => ({
  root: {
    '&:hover': {
      backgroundColor: theme.palette.grey['50'],
    },
    '&:hover $helperIcon': {
      opacity: 1,
    },
  },
  resolvedIcon: {
    fontSize: 16,
    marginRight: 4,
  },
  resolvedSuccessIcon: {
    color: theme.palette.success.main,
  },
  resolvedErrorIcon: {
    color: theme.palette.error.main,
  },
  resolveButton: {
    marginLeft: 25,
    paddingRight: 25 + theme.spacing(0.5),
  },
  pencil: {
    position: 'absolute',
    right: theme.spacing(0.5),
  },
  helperIcon: {
    fontSize: 20,
    opacity: 0,
    color: theme.palette.grey['500'],
  },
  warningIcon: {
    color: theme.palette.warning.main,
  },
}));

const actions = container.resolve(ImportActions);
export const ImportResultRow = (props: {
  row: components['schemas']['ImportLanguageModel'];
  onResolveConflicts: () => void;
  onShowFileIssues: () => void;
  onShowData: () => void;
}) => {
  const project = useProject();

  const classes = useStyles();

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
      <TableRow className={classes.root} data-cy="import-result-row">
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
                beforeIcon={<Warning className={classes.warningIcon} />}
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
            className={classes.resolveButton}
          >
            {props.row.resolvedCount < props.row.conflictCount ? (
              <Error
                className={clsx(
                  classes.resolvedIcon,
                  classes.resolvedErrorIcon
                )}
              />
            ) : (
              <CheckCircle
                className={clsx(
                  classes.resolvedIcon,
                  classes.resolvedSuccessIcon
                )}
              />
            )}
            {props.row.resolvedCount} / {props.row.conflictCount}
            {props.row.conflictCount > 0 && (
              <EditIcon className={clsx(classes.pencil, classes.helperIcon)} />
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
      </TableRow>
    </React.Fragment>
  );
};
