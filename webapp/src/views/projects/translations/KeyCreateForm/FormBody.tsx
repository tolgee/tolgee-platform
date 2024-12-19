import {
  FastField,
  Field,
  FieldArray,
  FieldProps,
  useFormikContext,
} from 'formik';
import { Box, Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useState } from 'react';
import clsx from 'clsx';

import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { FieldError } from 'tg.component/FormField';
import { Editor } from 'tg.component/editor/Editor';
import { useProject } from 'tg.hooks/useProject';
import { FieldLabel } from 'tg.component/FormField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LabelHint } from 'tg.component/common/LabelHint';

import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { RequiredField } from 'tg.component/common/form/RequiredField';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { PluralEditor } from '../translationVisual/PluralEditor';
import type { ValuesCreateType } from './KeyCreateForm';
import { PluralFormCheckbox } from 'tg.component/common/form/PluralFormCheckbox';
import { ControlsEditorSmall } from '../cell/ControlsEditorSmall';

const StyledContainer = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

const StyledKeyNsContainer = styled('div')`
  display: grid;
  gap: 0px 16px;
  grid-template-columns: 1fr;

  &.useNamespaces {
    grid-template-columns: 1fr 300px;
  }

  @media (max-width: 800px) {
    grid-template-columns: 1fr;
  }
`;

const StyledEdtorWrapper = styled('div')`
  background: ${({ theme }) => theme.palette.background.default};
  align-self: stretch;
  display: grid;
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
  onCancel?: () => void;
  autofocus?: boolean;
};

export const FormBody: React.FC<Props> = ({ onCancel, autofocus }) => {
  const { t } = useTranslate();
  const form = useFormikContext<ValuesCreateType>();
  const project = useProject();

  const baseLang = project.baseLanguage!;

  const isPlural = form.values.isPlural;

  const [mode, setMode] = useState<'placeholders' | 'syntax'>('placeholders');

  const actualParameter = isPlural
    ? form.values.pluralParameter || 'value'
    : undefined;

  return (
    <>
      <StyledContainer>
        <StyledKeyNsContainer
          className={clsx({ useNamespaces: project.useNamespaces })}
        >
          <FastField name="name">
            {({ field, form, meta }: FieldProps<any>) => {
              return (
                <div>
                  <FieldLabel>
                    <RequiredField>
                      <T keyName="translation_single_label_key" />
                    </RequiredField>
                  </FieldLabel>
                  <EditorWrapper>
                    <StyledEdtorWrapper data-cy="translation-create-key-input">
                      <Editor
                        mode="plain"
                        value={field.value}
                        onChange={(val) => {
                          form.setFieldValue(field.name, val);
                        }}
                        onBlur={() => form.setFieldTouched(field.name, true)}
                        minHeight="unset"
                        autofocus={autofocus}
                        scrollMargins={{ bottom: 150 }}
                        autoScrollIntoView
                        shortcuts={[
                          {
                            key: 'Enter',
                            run: () => (form.handleSubmit(), true),
                          },
                        ]}
                      />
                    </StyledEdtorWrapper>
                  </EditorWrapper>
                  <FieldError error={meta.touched && meta.error} />
                </div>
              );
            }}
          </FastField>

          {project.useNamespaces && (
            <FastField name="namespace">
              {({ field, form }: FieldProps<any>) => {
                return (
                  <div>
                    <FieldLabel>
                      <LabelHint title={t('translation_single_namespace_hint')}>
                        <T keyName="translation_single_label_namespace" />
                      </LabelHint>
                    </FieldLabel>
                    <StyledEdtorWrapper data-cy="translation-create-namespace-input">
                      <NamespaceSelector
                        value={field.value}
                        onChange={(value) =>
                          form.setFieldValue(field.name, value)
                        }
                      />
                    </StyledEdtorWrapper>
                  </div>
                );
              }}
            </FastField>
          )}
        </StyledKeyNsContainer>

        <FastField name={`description`}>
          {({ field, form }) => (
            <div>
              <FieldLabel>
                <LabelHint title={t('translation_single_description_hint')}>
                  <T keyName="translation_single_label_description" />
                </LabelHint>
              </FieldLabel>
              <EditorWrapper>
                <StyledEdtorWrapper data-cy="translation-create-description-input">
                  <Editor
                    mode="plain"
                    value={field.value}
                    onChange={(val) => {
                      form.setFieldValue(field.name, val);
                    }}
                    shortcuts={[
                      {
                        key: 'Enter',
                        run: () => (form.handleSubmit(), true),
                      },
                    ]}
                    onBlur={() => form.setFieldTouched(field.name, true)}
                    minHeight={50}
                    scrollMargins={{ bottom: 150 }}
                    autoScrollIntoView
                  />
                </StyledEdtorWrapper>
              </EditorWrapper>
            </div>
          )}
        </FastField>

        <FieldArray
          name="tags"
          render={(helpers) => (
            <FastField name="tags">
              {({ field }: FieldProps<any>) => {
                return (
                  <div>
                    <FieldLabel>
                      <T keyName="translation_single_label_tags" />
                    </FieldLabel>
                    <StyledTags>
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
                        placeholder={t('translation_single_tag_placeholder')}
                      />
                    </StyledTags>
                  </div>
                );
              }}
            </FastField>
          )}
        />

        <PluralFormCheckbox
          isPluralName="isPlural"
          pluralParameterName="pluralParameter"
        />

        <Field key={baseLang.tag} name="baseValue">
          {({ field, meta }) => (
            <div data-cy="translation-create-translation-input">
              <Box display="flex" justifyContent="space-between">
                <FieldLabel>
                  <Box display="flex" gap={0.5}>
                    <CircledLanguageIcon flag={baseLang.flagEmoji} size={16} />
                    {baseLang.name}
                  </Box>
                </FieldLabel>
                <ControlsEditorSmall
                  mode={mode}
                  onModeToggle={() =>
                    setMode(mode === 'syntax' ? 'placeholders' : 'syntax')
                  }
                />
              </Box>
              <PluralEditor
                value={{ ...field.value, parameter: actualParameter }}
                onChange={(val) => {
                  form.setFieldValue(field.name, val);
                }}
                locale={baseLang.tag}
                mode={mode}
                editorProps={{
                  autoScrollIntoView: true,
                  scrollMargins: { bottom: 150 },
                  shortcuts: [
                    {
                      key: 'Enter',
                      run: () => (form.handleSubmit(), true),
                    },
                  ],
                }}
              />
              <FieldError error={meta.touched && meta.error} />
            </div>
          )}
        </Field>
      </StyledContainer>

      <Box display="flex" alignItems="flex-end" justifySelf="flex-end">
        {onCancel && (
          <Button data-cy="global-form-cancel-button" onClick={onCancel}>
            <T keyName="global_cancel_button" />
          </Button>
        )}
        <Box ml={1}>
          <LoadingButton
            data-cy="global-form-save-button"
            loading={form.isSubmitting}
            color="primary"
            variant="contained"
            disabled={!form.isValid}
            type="submit"
            onClick={() => form.handleSubmit()}
          >
            <T keyName="global_form_save" />
          </LoadingButton>
        </Box>
      </Box>
    </>
  );
};
