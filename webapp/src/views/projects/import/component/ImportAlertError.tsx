import React, {
  FunctionComponent,
  ReactNode,
  useEffect,
  useState,
} from 'react';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Collapse,
  IconButton,
} from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useImportDataHelper } from '../hooks/useImportDataHelper';

export const ImportAlertError: FunctionComponent<{
  error: components['schemas']['ImportAddFilesResultModel']['errors'][0];
  addFilesMutation: ReturnType<typeof useImportDataHelper>['addFilesMutation'];
}> = (props) => {
  const [moreOpen, setMoreOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  let text = undefined as ReactNode | undefined;
  let params = [] as string[];

  if (props.error?.code === 'cannot_parse_file') {
    text = <T keyName="import_error_cannot_parse_file" />;
    params = props.error.params as any as string[];
  }

  useEffect(() => {
    setCollapsed(true);
    if (props.addFilesMutation.isSuccess && !props.addFilesMutation.isLoading) {
      setCollapsed(false);
      setMoreOpen(false);
    }
  }, [props.addFilesMutation.isLoading]);

  const open = !collapsed && !!text;

  return (
    <Collapse in={open}>
      <Box mt={4} data-cy="import-file-error">
        <Alert
          severity="error"
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
                    <T keyName="import_error_less_button" />
                  ) : (
                    <T keyName="import_error_more_button" />
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
                <XClose />
              </IconButton>
            </>
          }
        >
          <AlertTitle>{text}</AlertTitle>
          {params[0] && (
            <T
              keyName="import_cannot_parse_file_message"
              params={{
                name: params[0],
              }}
            />
          )}
          <Box pt={2}>{moreOpen && params[1]}</Box>
        </Alert>
      </Box>
    </Collapse>
  );
};
