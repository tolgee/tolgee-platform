import { FunctionComponent, useEffect, useState } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { ResourceErrorComponent } from '../common/form/ResourceErrorComponent';
import { CreateLanguageField } from './CreateLanguageField';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { ApiError } from 'tg.service/http/ApiError';
import { messageService } from 'tg.service/MessageService';

type LanguageDto = components['schemas']['LanguageDto'];

export const CreateSingleLanguage: FunctionComponent<{
  onCancel: () => void;
  onCreated?: (language: components['schemas']['LanguageModel']) => void;
  autoFocus?: boolean;
}> = (props) => {
  const project = useProject();
  const { refetchUsage } = useGlobalActions();
  const createLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages',
    method: 'post',
    fetchOptions: {
      disableErrorNotification: true,
    },
  });

  const [value, setValue] = useState(null as LanguageDto | null);

  const onSubmit = (values) => {
    createLoadable.mutate(
      {
        path: {
          projectId: project.id,
        },
        content: {
          'application/json': values,
        },
      },
      {
        onSuccess(data) {
          refetchUsage();
          props.onCreated && props.onCreated(data);
          setValue(null);
          messageService.success(<T keyName="language_created_message" />);
        },
      }
    );
  };

  const [serverError, setServerError] = useState(
    undefined as ApiError | undefined | null
  );

  useEffect(() => {
    setServerError(createLoadable.error);
  }, [createLoadable.error]);

  return (
    <Box>
      {serverError && (
        <Box ml={2} mr={2}>
          <ResourceErrorComponent error={serverError} limit={1} />
        </Box>
      )}
      <CreateLanguageField
        autoFocus={props.autoFocus}
        value={value}
        onSubmit={(values) => onSubmit(values)}
        showSubmitButton={true}
        onChange={(value) => {
          setValue(value);
          setServerError(undefined);
        }}
        onPreparedLanguageEdit={() => setServerError(undefined)}
      />
    </Box>
  );
};
