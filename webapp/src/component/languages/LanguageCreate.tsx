import React, { FunctionComponent, useEffect, useState } from 'react';
import { container } from 'tsyringe';
import { LanguageActions } from '../../store/languages/LanguageActions';
import { useProject } from '../../hooks/useProject';
import { components } from '../../service/apiSchema.generated';
import { Box } from '@material-ui/core';
import { ErrorResponseDto } from '../../service/response.types';
import { ResourceErrorComponent } from '../common/form/ResourceErrorComponent';
import { CreateLanguageField } from './CreateLanguageField';

const actions = container.resolve(LanguageActions);
export const LanguageCreate: FunctionComponent<{
  onCancel: () => void;
  onCreated?: (language: components['schemas']['LanguageModel']) => void;
}> = (props) => {
  const createLoadable = actions.useSelector((s) => s.loadables.create);
  const project = useProject();
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (values) => {
    setSubmitted(true);
    actions.loadableActions.create.dispatch({
      path: {
        projectId: project.id,
      },
      content: {
        'application/json': values,
      },
    });
  };

  useEffect(() => {
    if (createLoadable.loaded && submitted) {
      props.onCreated && props.onCreated(createLoadable.data!);
      setSubmitted(false);
      setValue(null);
    }
  }, [createLoadable.loading]);

  const [value, setValue] = useState(
    null as components['schemas']['LanguageDto'] | null
  );
  const [serverError, setServerError] = useState(
    undefined as ErrorResponseDto | undefined
  );

  useEffect(() => {
    setServerError(createLoadable.error);
  }, [createLoadable.error]);

  return (
    <Box>
      {serverError && (
        <Box ml={2} mr={2}>
          <ResourceErrorComponent error={serverError} />
        </Box>
      )}
      <CreateLanguageField
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
