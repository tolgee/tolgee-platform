import { useApiQuery } from 'tg.service/http/useQueryApi';
import { CheckBoxGroupMultiSelect } from '../common/form/fields/CheckBoxGroupMultiSelect';
import { default as React } from 'react';
import { Formik } from 'formik';

export const ScopeSelector = () => {
  const availableScopesLoadable = useApiQuery({
    url: '/v2/api-keys/availableScopes',
    method: 'get',
  });

  const availableScopes = [];

  if (!availableScopesLoadable.data) {
    return null;
  }

  Object.values(availableScopesLoadable.data).forEach((it) =>
    it.forEach((scope) => availableScopes.push(scope))
  );

  console.log(availableScopes);

  return (
    <Formik initialValues={{ scopes: new Set() }}>
      <CheckBoxGroupMultiSelect
        label="Scopes"
        name="scopes"
        options={new Set(availableScopes)}
      />
    </Formik>
  );
};
