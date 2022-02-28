import React, { FunctionComponent } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@material-ui/core';
import { Warning } from '@material-ui/icons';
import { Alert } from '@material-ui/lab';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import { SimplePaginatedHateoasList } from 'tg.component/common/list/SimplePaginatedHateoasList';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

const actions = container.resolve(ImportActions);
export const ImportFileIssuesDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const project = useProject();

  const row = props.row;

  return (
    <div>
      <Dialog
        maxWidth="lg"
        open={!!row}
        onClose={props.onClose}
        aria-labelledby="import-file-issues-dialog"
        data-cy="import-file-issues-dialog"
      >
        {row && (
          <>
            <DialogTitle id="import-file-issues-dialog">
              <T parameters={{ fileName: row.importFileName }}>
                import_file_issues_title
              </T>
            </DialogTitle>
            <DialogContent>
              <SimplePaginatedHateoasList
                actions={actions}
                loadableName="getFileIssues"
                dispatchParams={[
                  {
                    path: {
                      projectId: project.id,
                      importFileId: row.importFileId,
                    },
                  },
                ]}
                wrapperComponent={Box}
                wrapperComponentProps={{ mb: 2 }}
                listComponent={Box}
                sortBy={[]}
                renderItem={(i) => (
                  <>
                    <Alert color="warning" icon={<Warning />}>
                      {i.type && (
                        <T>{`file_issue_type_${i.type!.toLowerCase()}`}</T>
                      )}
                      &nbsp;(
                      {i.params &&
                        i.params!.map(
                          (param, idx) =>
                            param.value && (
                              <>
                                <T
                                  parameters={{ value: param.value! }}
                                >{`import_file_issue_param_type_${param.type.toLowerCase()}`}</T>
                                {idx < i.params!.length - 1 && ', '}
                              </>
                            )
                        )}
                      )
                    </Alert>
                  </>
                )}
              />
            </DialogContent>
          </>
        )}
      </Dialog>
    </div>
  );
};
