import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  styled,
} from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { Formik } from 'formik';
import { Editor } from 'tg.component/editor/Editor';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { FieldLabel } from 'tg.component/FormField';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { useTranslationsActions } from '../context/TranslationsContext';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Button } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { FieldError } from 'tg.component/FormField';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
  justify-content: stretch;
  max-width: 100%;
  position: relative;
`;

const StyledSection = styled('div')`
  display: grid;
  align-items: stretch;
  max-width: 100%;
`;

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  overflow: hidden;

  & > * {
    margin: 0px 3px 3px 0px;
  }

  position: relative;
`;

type Props = {
  keyId: number;
  name: string;
  namespace: string | undefined;
  tags: string[];
  onClose: () => void;
};

export const KeyEditModal: React.FC<Props> = ({
  keyId,
  name,
  namespace = '',
  tags,
  onClose,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const { updateKey } = useTranslationsActions();
  const messaging = useMessage();

  const updateKeyLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}/complex-update',
    method: 'put',
    fetchOptions: {
      disableBadRequestHandling: true,
    },
  });

  return (
    <Formik
      initialValues={{ name, namespace, tags }}
      onSubmit={(values, helpers) => {
        updateKeyLoadable.mutate(
          {
            path: { projectId: project.id, id: keyId },
            content: {
              'application/json': values,
            },
          },
          {
            onSuccess(data) {
              onClose();
              updateKey({
                keyId,
                value: {
                  keyName: data.name,
                  keyNamespace: data.namespace,
                  keyTags: data.tags,
                },
              });
            },
            onError(e) {
              if (e.STANDARD_VALIDATION) {
                helpers.setErrors(e.STANDARD_VALIDATION);
              } else {
                parseErrorResponse(e).forEach((message) =>
                  messaging.error(<T>{message}</T>)
                );
              }
            },
          }
        );
      }}
    >
      {({ values, errors, setFieldValue, submitForm }) => {
        return (
          <Dialog open={true} onClose={onClose} fullWidth>
            <DialogTitle>{t('translations_key_edit_title')}</DialogTitle>
            <StyledDialogContent>
              <StyledSection>
                <FieldLabel>{t('translations_key_edit_label')}</FieldLabel>
                <EditorWrapper>
                  <Editor
                    autofocus
                    value={values.name}
                    onChange={(val) => setFieldValue('name', val)}
                    onSave={submitForm}
                    plaintext
                    minHeight="unset"
                  />
                </EditorWrapper>
                <FieldError error={errors.name} />
              </StyledSection>
              <StyledSection>
                <FieldLabel>
                  {t('translations_key_edit_label_namespace')}
                </FieldLabel>
                <NamespaceSelector
                  value={values.namespace}
                  onChange={(value) => setFieldValue('namespace', value)}
                />
                <FieldError error={errors.namespace} />
              </StyledSection>
              <StyledSection>
                <FieldLabel>{t('translations_key_edit_label_tags')}</FieldLabel>
                <StyledTags>
                  {values.tags.map((tag, index) => {
                    return (
                      <Tag
                        key={tag}
                        name={tag}
                        onDelete={() =>
                          setFieldValue(
                            'tags',
                            values.tags.filter((val) => val !== tag)
                          )
                        }
                      />
                    );
                  })}
                  <TagInput
                    existing={values.tags}
                    onAdd={(name) =>
                      !values.tags.includes(name) &&
                      setFieldValue('tags', [...values.tags, name])
                    }
                    placeholder={t('translations_key_edit_placeholder')}
                  />
                </StyledTags>
                <FieldError error={errors.tags} />
              </StyledSection>
            </StyledDialogContent>
            <DialogActions>
              <Button
                data-cy="translations-cell-cancel-button"
                onClick={onClose}
              >
                <T>global_cancel_button</T>
              </Button>
              <Box ml={1}>
                <LoadingButton
                  data-cy="translations-cell-save-button"
                  loading={updateKeyLoadable.isLoading}
                  color="primary"
                  variant="contained"
                  type="submit"
                  onClick={() => submitForm()}
                >
                  <T>global_form_save</T>
                </LoadingButton>
              </Box>
            </DialogActions>
          </Dialog>
        );
      }}
    </Formik>
  );
};
