import { Formik } from 'formik';
import * as Yup from 'yup';
import { useHistory } from 'react-router';
import { T } from '@tolgee/react';
import { Box, Button } from '@material-ui/core';
import { container } from 'tsyringe';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { MessageService } from 'tg.service/MessageService';
import { TranslationsForm, LanguageType } from './TranslationsForm';

const messaging = container.resolve(MessageService);

export type ValuesType = {
  key: string;
  translations: Record<string, string>;
};

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

type Props = {
  translation: KeyWithTranslationsModel;
  languages: LanguageType[];
};

export const KeyEditForm: React.FC<Props> = ({ translation, languages }) => {
  const project = useProject();
  const history = useHistory();

  const updateKey = useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/translations',
  });

  const handleSubmit = (values: ValuesType) => {
    return updateKey.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': values },
      },
      {
        onSuccess(data) {
          messaging.success(<T>translations_key_edited</T>);
          history.replace(
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
  if (translation?.translations) {
    Object.entries(translation?.translations).forEach(([key, value]) => {
      translationValues[key] = value.text || '';
    });
  }

  return (
    <Formik
      initialValues={{
        key: translation.keyName,
        translations: translationValues,
      }}
      onSubmit={handleSubmit}
      validationSchema={Yup.object().shape({
        key: Yup.string().required(),
      })}
      enableReinitialize
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
              disabled={updateKey.isLoading || !form.dirty}
              onClick={() => form.resetForm()}
            >
              <T>global_form_reset</T>
            </Button>
            <Box ml={1}>
              <LoadingButton
                data-cy="global-form-save-button"
                loading={updateKey.isLoading}
                color="primary"
                variant="contained"
                disabled={!form.dirty}
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
