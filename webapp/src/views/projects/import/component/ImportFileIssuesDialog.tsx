import React, { FunctionComponent, useState } from 'react';
import { Alert, Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { Warning } from '@mui/icons-material';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const ImportFileIssuesDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const project = useProject();
  const row = props.row;
  const [page, setPage] = useState(0);

  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/result/files/{importFileId}/issues',
    method: 'get',
    path: {
      projectId: project.id,
      importFileId: row?.importFileId as any,
    },
    options: {
      enabled: !!row,
    },
    query: {
      page: page,
    },
  });

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
              <T params={{ fileName: row.importFileName }}>
                import_file_issues_title
              </T>
            </DialogTitle>
            <DialogContent>
              <PaginatedHateoasList
                loadable={loadable}
                onPageChange={setPage}
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
                                  params={{ value: param.value! }}
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
