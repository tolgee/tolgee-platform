import { Box, Button, styled } from '@mui/material';
import { OperationStatusType, OperationType } from './ImportFileInput';
import React, { useEffect, useState } from 'react';
import clsx from 'clsx';
import {
  ImportInputAreaLayout,
  ImportInputAreaLayoutBottom,
  ImportInputAreaLayoutCenter,
  ImportInputAreaLayoutTop,
} from './ImportInputAreaLayout';
import { ImportProgressBar } from './ImportProgress';
import { ImportOperationStatus } from './ImportOperationStatus';
import { ImportOperationTitle } from './ImportOperationTitle';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';

const StyledRoot = styled(Box)`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: ${({ theme }) => theme.palette.background.paper};
  z-index: 1;
  opacity: 0;
  transition: opacity 0.1s ease-in-out;

  &.visible {
    opacity: 1;
  }
`;

export const ImportProgressOverlay = (props: {
  operation?: OperationType;
  filesUploaded?: boolean;
  importDone: boolean;
  loading: boolean;
  operationStatus?: OperationStatusType;
  onImportMore: () => void;
  onActiveChange: (isActive: boolean) => void;
}) => {
  const project = useProject();

  const [{ visible, filesUploaded, operation }, setState] = useState({
    visible: false,
    filesUploaded: false,
    operation: undefined as OperationType | undefined,
  });

  const [previousOperation, setPreviousOperation] = useState<OperationType>();

  useEffect(() => {
    const localPreviousOperation = previousOperation;
    setPreviousOperation(props.operation);

    if (localPreviousOperation == 'addFiles' && !props.loading) {
      setState({
        visible: true,
        filesUploaded: true,
        operation: props.operation,
      });
      const timeout = setTimeout(() => {
        setState({
          visible: props.loading || props.importDone,
          filesUploaded: false,
          operation: props.operation,
        });
        setPreviousOperation(undefined);
      }, 1000);
      return () => clearTimeout(timeout);
    }

    setState({
      visible: props.loading || props.importDone,
      filesUploaded: false,
      operation: props.operation,
    });
  }, [props.loading, props.operation, props.importDone]);

  useEffect(() => {
    props.onActiveChange(visible);
  }, [visible]);

  const showFilesUploaded =
    filesUploaded || (props.filesUploaded && visible && !operation);

  const showImportDone = props.importDone;

  return (
    <StyledRoot
      className={clsx({ visible })}
      sx={{
        pointerEvents: props.importDone ? 'all' : 'none',
      }}
      data-cy="import-progress-overlay"
    >
      <ImportInputAreaLayout>
        <ImportInputAreaLayoutTop>
          <ImportOperationTitle
            operation={operation}
            filesUploaded={
              (filesUploaded || props.filesUploaded) &&
              !props.importDone &&
              !operation
            }
            importDone={props.importDone}
          />
        </ImportInputAreaLayoutTop>
        <ImportInputAreaLayoutCenter>
          <ImportProgressBar
            loading={props.loading}
            loaded={showFilesUploaded || showImportDone}
          />
        </ImportInputAreaLayoutCenter>
        <ImportInputAreaLayoutBottom>
          {props.importDone ? (
            <Box>
              <Button
                component={Link}
                color="primary"
                to={LINKS.PROJECT_TRANSLATIONS.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })}
              >
                <T keyName="import-done-go-to-translations-button" />
              </Button>
              <Button
                color="primary"
                sx={{ ml: 1 }}
                onClick={props.onImportMore}
              >
                <T keyName="import-done-go-import-more-button" />
              </Button>
            </Box>
          ) : (
            <ImportOperationStatus status={props.operationStatus} />
          )}
        </ImportInputAreaLayoutBottom>
      </ImportInputAreaLayout>
    </StyledRoot>
  );
};
