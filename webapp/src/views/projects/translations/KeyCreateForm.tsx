import { T, useTranslate } from '@tolgee/react';
import { FastField, FieldArray, FieldProps, Formik, Field } from 'formik';
import * as Yup from 'yup';
import { Box, Button, makeStyles, Typography } from '@material-ui/core';
import { container } from 'tsyringe';

import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useUrlSearch } from 'tg.hooks/useUrlSearch.ts';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { LINKS } from 'tg.constants/links';
import { MessageService } from 'tg.service/MessageService';
import { Editor } from 'tg.component/editor/Editor';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';
import { Tag } from './Tags/Tag';
import { TagInput } from './Tags/TagInput';
import { FieldLabel } from './KeySingle/FieldLabel';
import { ProjectPermissionType } from 'tg.service/response.types';
import { useEffect } from 'react';

type KeyWithDataModel = components['schemas']['KeyWithDataModel'];

const messaging = container.resolve(MessageService);
const redirectionActions = container.resolve(RedirectionActions);

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    rowGap: theme.spacing(2),
    marginBottom: theme.spacing(2),
  },
  field: {
    background: theme.palette.grey[100],
    border: `1px solid ${theme.palette.grey[400]}`,
    overflow: 'hidden',
    borderRadius: 4,
    '&:hover': {
      border: `1px solid ${theme.palette.common.black}`,
    },
    '&:focus-within': {
      borderColor: theme.palette.primary.main,
      borderWidth: 2,
    },
    '& > *': {
      padding: 10,
    },
    '&:focus-within > *': {
      padding: 9,
    },
  },
  editorWrapper: {
    background: 'white',
    alignSelf: 'stretch',
    display: 'flex',
    alignItems: 'stretch',
  },
  tags: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'flex-start',
    overflow: 'hidden',
    '& > *': {
      margin: '0px 3px 3px 0px',
    },
    position: 'relative',
  },
  error: {
    display: 'flex',
    minHeight: '1.2rem',
  },
}));

type LanguageType = {
  tag: string;
  name: string;
};

export type ValuesCreateType = {
  name: string;
  translations: Record<string, string>;
  tags: string[];
};

type Props = {
  languages: LanguageType[];
  onSuccess?: (data: KeyWithDataModel) => void;
  onCancel?: () => void;
  autofocus?: boolean;
};

export const KeyCreateForm: React.FC<Props> = ({
  languages,
  onSuccess,
  onCancel,
  autofocus,
}) => {
  const classes = useStyles();
  const project = useProject();
  const permissions = useProjectPermissions();
  const t = useTranslate();

  const keyName = useUrlSearch().key as string;

  const createKey = useApiMutation({
    url: '/v2/projects/{projectId}/keys/create',
    method: 'post',
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
          onSuccess?.(data);
        },
        onError(e) {
          parseErrorResponse(e).forEach((message) =>
            messaging.error(<T>{message}</T>)
          );
        },
      }
    );
  };

  const translationValues = {};
  languages.forEach(({ tag }) => {
    translationValues[tag] = '';
  });

  const canEdit = permissions.satisfiesPermission(ProjectPermissionType.EDIT);
  useEffect(() => {
    if (!canEdit) {
      redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
      messaging.error(<T>translation_single_no_permission_create</T>);
    }
  }, [canEdit]);

  return canEdit ? (
    <Formik
      initialValues={{
        name: keyName,
        translations: translationValues,
        tags: [],
      }}
      onSubmit={handleSubmit}
      validationSchema={Yup.object().shape({
        name: Yup.string().required(),
      })}
    >
      {(form) => (
        <>
          <div className={classes.container}>
            <FastField name="name">
              {({ field, form, meta }: FieldProps<any>) => {
                return (
                  <div>
                    <FieldLabel>
                      <T>translation_single_label_key</T>
                    </FieldLabel>
                    <div className={classes.field}>
                      <div
                        className={classes.editorWrapper}
                        data-cy="translation-create-key-input"
                      >
                        <Editor
                          plaintext
                          value={field.value}
                          onChange={(val) =>
                            form.setFieldValue(field.name, val)
                          }
                          onSave={() => form.handleSubmit()}
                          onBlur={() => form.setFieldTouched(field.name, true)}
                          minHeight="unset"
                          autofocus={autofocus}
                        />
                      </div>
                    </div>
                    <Typography
                      color="error"
                      variant="caption"
                      className={classes.error}
                    >
                      {meta.touched && meta.error}
                    </Typography>
                  </div>
                );
              }}
            </FastField>

            <FieldArray
              name="tags"
              render={(helpers) => (
                <Field name="tags">
                  {({ field }: FieldProps<any>) => {
                    return (
                      <div>
                        <FieldLabel>
                          <T>translation_single_label_tags</T>
                        </FieldLabel>
                        <div className={classes.tags}>
                          {field.value.map((tag, index) => {
                            return (
                              <Tag
                                key={tag}
                                name={tag}
                                onDelete={() => helpers.remove(index)}
                              />
                            );
                          })}
                          <TagInput
                            existing={field.value}
                            onAdd={(name) =>
                              !field.value.includes(name) && helpers.push(name)
                            }
                            placeholder={t(
                              'translation_single_tag_placeholder'
                            )}
                          />
                        </div>
                      </div>
                    );
                  }}
                </Field>
              )}
            />
            {languages.map((lang) => {
              return (
                <FastField key={lang.tag} name={`translations.${lang.tag}`}>
                  {({ field, form, meta }) => (
                    <div key={lang.tag}>
                      <FieldLabel>{lang.name}</FieldLabel>
                      <div className={classes.field}>
                        <div
                          className={classes.editorWrapper}
                          data-cy="translation-create-translation-input"
                        >
                          <Editor
                            value={field.value || ''}
                            onSave={() => form.handleSubmit()}
                            onChange={(val) =>
                              form.setFieldValue(field.name, val)
                            }
                            minHeight={50}
                          />
                        </div>
                      </div>
                    </div>
                  )}
                </FastField>
              );
            })}
          </div>
          <Box display="flex" alignItems="flex-end" justifySelf="flex-end">
            {onCancel && (
              <Button data-cy="global-form-cancel-button" onClick={onCancel}>
                <T>global_cancel_button</T>
              </Button>
            )}
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
  ) : null;
};
