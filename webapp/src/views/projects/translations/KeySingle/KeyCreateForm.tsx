import { T } from '@tolgee/react';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { Box, Button } from '@material-ui/core';
import { container } from 'tsyringe';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { TranslationsForm, LanguageType } from './TranslationsForm';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUrlSearch } from 'tg.hooks/useUrlSearch.ts';
import { useHistory } from 'react-router';
import { LINKS, PARAMS } from 'tg.constants/links';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { MessageService } from 'tg.service/MessageService';

const messaging = container.resolve(MessageService);

export type ValuesCreateType = {
  key: string;
  translations: Record<string, string>;
};

type Props = {
  languages: LanguageType[];
};

export const KeyCreateForm: React.FC<Props> = ({ languages }) => {
  const project = useProject();
  const history = useHistory();

  const keyName = useUrlSearch().key as string;

  const createKey = useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/translations',
  });

  const handleSubmit = (values: ValuesCreateType) => {
    return createKey.mutateAsync(
      {
        path: { projectId: project.id },
        content: { 'application/json': values },
      },
      {
        onSuccess(data) {
          messaging.success(<T>translations_key_created</T>);
          history.push(
            LINKS.PROJECT_TRANSLATIONS_SINGLE.build({
              [PARAMS.PROJECT_ID]: project.id,
            }) +
              queryEncode({
                key: data.keyName,
                languages: languages.map((l) => l.tag),
              })
          );
        },
      }
    );
  };

  const translationValues = {};
  languages.forEach(({ tag }) => {
    translationValues[tag] = '';
  });

  return (
    <Formik
      initialValues={{
        key: keyName,
        translations: translationValues,
      }}
      onSubmit={handleSubmit}
      validationSchema={Yup.object().shape({
        key: Yup.string().required(),
      })}
    >
      {(form) => (
        <>
          <TranslationsForm languages={languages} />
          <Box
            display="flex"
            alignItems="flex-end"
            mb={2}
            justifySelf="flex-end"
          >
            <Button
              data-cy="global-form-cancel-button"
              disabled={createKey.isLoading || !form.dirty}
              onClick={() => form.resetForm()}
            >
              <T>global_form_reset</T>
            </Button>
            <Box ml={1}>
              <LoadingButton
                data-cy="global-form-save-button"
                loading={createKey.isLoading}
                color="primary"
                variant="contained"
                disabled={!form.isValid}
                type="submit"
                onClick={() => form.handleSubmit()}
              >
                <T>global_form_save</T>
              </LoadingButton>
            </Box>
          </Box>
        </>
      )}
    </Formik>
  );
};
