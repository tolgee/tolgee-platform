import React, { FunctionComponent, useEffect, useState } from 'react';
import { StandardForm } from '../common/form/StandardForm';
import { Validation } from '../../constants/GlobalValidationSchema';
import { LanguageDTO } from '../../service/response.types';
import { container } from 'tsyringe';
import { LanguageActions } from '../../store/languages/LanguageActions';
import { useProject } from '../../hooks/useProject';
import { LanguageCreateField } from './LanguageCreateFIeld';

const actions = container.resolve(LanguageActions);
export const LanguageCreateForm: FunctionComponent<{
  onCancel: () => void;
  onCreated?: (language: LanguageDTO) => void;
}> = (props) => {
  const createLoadable = actions.useSelector((s) => s.loadables.create);
  const project = useProject();
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (values) => {
    setSubmitted(true);
    const dto: LanguageDTO = {
      ...values,
    };
    actions.loadableActions.create.dispatch(project.id, dto);
  };

  useEffect(() => {
    if (createLoadable.loaded && submitted) {
      props.onCreated && props.onCreated(createLoadable.data!);
      setSubmitted(false);
    }
  }, [createLoadable.loading]);

  return (
    <StandardForm
      initialValues={{ englishName: '', originalName: '', tag: '' }}
      onSubmit={onSubmit}
      onCancel={props.onCancel}
      saveActionLoadable={createLoadable}
      validationSchema={Validation.LANGUAGE}
    >
      <LanguageCreateField />
    </StandardForm>
  );
};
