import React, {
  FunctionComponent,
  ReactNode,
  useEffect,
  useState,
} from 'react';
import { Box, Button, Collapse, IconButton } from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';
import { Alert, AlertTitle } from '@material-ui/lab';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

const actions = container.resolve(ImportActions);
export const ImportAlertError: FunctionComponent<{
  error: components['schemas']['ImportAddFilesResultModel']['errors'][0];
}> = (props) => {
  const [moreOpen, setMoreOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  const addFilesLoadable = actions.useSelector((s) => s.loadables.addFiles);

  let text = undefined as ReactNode | undefined;
  let params = [] as string[];

  if (props.error?.code === 'cannot_parse_file') {
    text = <T>import_error_cannot_parse_file</T>;
    params = props.error.params as any as string[];
  }

  useEffect(() => {
    setCollapsed(true);
    if (addFilesLoadable.loaded && !addFilesLoadable.loading) {
      setCollapsed(false);
      setMoreOpen(false);
    }
  }, [addFilesLoadable.loading]);

  const open = !collapsed && !!text;

  return (
    <Collapse in={open}>
      <Box mt={4} data-cy="import-file-error">
        <Alert
          color="error"
          action={
            <>
              <Box display="inline" mr={1}>
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => setMoreOpen(!moreOpen)}
                  data-cy="import-file-error-more-less-button"
                >
                  {moreOpen ? (
                    <T>import_error_less_button</T>
                  ) : (
                    <T>import_error_more_button</T>
                  )}
                </Button>
              </Box>
              <IconButton
                data-cy="import-file-error-collapse-button"
                aria-label="close"
                color="inherit"
                size="small"
                onClick={() => {
                  setCollapsed(true);
                }}
              >
                <CloseIcon fontSize="inherit" />
              </IconButton>
            </>
          }
        >
          <AlertTitle>{text}</AlertTitle>
          {params[0] && (
            <T
              parameters={{
                name: params[0],
              }}
            >
              import_cannot_parse_file_message
            </T>
          )}
          <Box pt={2}>{moreOpen && params[1]}</Box>
        </Alert>
      </Box>
    </Collapse>
  );
};
