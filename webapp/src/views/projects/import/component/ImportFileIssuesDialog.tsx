import { FunctionComponent } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { Warning } from '@mui/icons-material';
import { Alert } from '@mui/material';
import { T } from '@tolgee/react';

import { SimplePaginatedHateoasList } from 'tg.component/common/list/SimplePaginatedHateoasList';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { importActions } from 'tg.store/project/ImportActions';

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
                actions={importActions}
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
                wrapperComponentProps={{ sx: { mb: 2 } }}
                listComponent={Box}
                sortBy={[]}
                renderItem={(i) => (
                  <>
                    <Alert severity="warning" icon={<Warning />}>
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
