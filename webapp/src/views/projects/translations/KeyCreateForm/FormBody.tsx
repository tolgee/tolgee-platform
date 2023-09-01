import { useCallback, useEffect, useState } from 'react';
import { FastField, FieldArray, FieldProps, useFormikContext } from 'formik';
import { Box, Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { FieldError } from 'tg.component/FormField';
import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { useProject } from 'tg.hooks/useProject';
import { FieldLabel } from 'tg.component/FormField';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { ToolsBottomPanel } from '../TranslationTools/ToolsBottomPanel';
import { useTranslationTools } from '../TranslationTools/useTranslationTools';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

const StyledKeyNsContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0px 16px;
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
  languages: LanguageModel[];
};

export const FormBody: React.FC<Props> = ({
  onCancel,
  autofocus,
  languages,
}) => {
  const [editedLang, setEditedLang] = useState<string | null>(null);
  const { t } = useTranslate();
  const form = useFormikContext<any>();
  const project = useProject();

  const onFocus = (lang: string | null) => {
    setEditedLang(lang);
  };

  const baseLang = project.baseLanguage?.tag;

  const baseText = form.values?.translations?.[baseLang || ''];
  const targetLang = languages.find(({ tag }) => tag === editedLang);

  const hintRelevant = Boolean(
    baseText && targetLang && editedLang !== baseLang
  );

  const [hintDisplayed, setHintDisplayed] = useState(false);

  const onValueUpdate = useCallback(
    (value: string) => {
      form.setFieldValue(`translations.${editedLang}`, value);
    },
    [editedLang]
  );

  useEffect(() => {
    if (hintRelevant) {
      if (!hintDisplayed) {
        setHintDisplayed(true);
      }
    } else if (!baseText) {
      setHintDisplayed(false);
    }
  }, [hintRelevant, baseText]);

  const toolsData = useTranslationTools({
    projectId: project.id,
    baseText,
    targetLanguageId: targetLang?.id as number,
    keyId: undefined as any,
    onValueUpdate,
    enabled: hintRelevant,
  });

  return (
    <>
      <StyledContainer>
        <StyledKeyNsContainer>
          <FastField name="name">
            {({ field, form, meta }: FieldProps<any>) => {
              return (
                <div>
                  <FieldLabel>
                    <T keyName="translation_single_label_key" />
                  </FieldLabel>
                  <EditorWrapper>
                    <StyledEdtorWrapper data-cy="translation-create-key-input">
                      <Editor
                        plaintext
                        value={field.value}
                        onChange={(val) => {
                          form.setFieldValue(field.name, val);
                        }}
                        onSave={() => form.handleSubmit()}
                        onBlur={() => form.setFieldTouched(field.name, true)}
                        minHeight="unset"
                        autofocus={autofocus}
                        scrollMargins={{ bottom: 150 }}
                        autoScrollIntoView
                      />
                    </StyledEdtorWrapper>
                  </EditorWrapper>
                  <FieldError error={meta.touched && meta.error} />
                </div>
              );
            }}
          </FastField>

          <FastField name="namespace">
            {({ field, form }: FieldProps<any>) => {
              return (
                <div>
                  <FieldLabel>
                    <T keyName="translation_single_label_namespace" />
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
        </StyledKeyNsContainer>

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
        {languages.map((lang, i) => (
          <FastField key={lang.tag} name={`translations.${lang.tag}`}>
            {({ field, form, meta }) => (
              <div key={lang.tag}>
                <FieldLabel>{lang.name}</FieldLabel>
                <EditorWrapper>
                  <StyledEdtorWrapper data-cy="translation-create-translation-input">
                    <Editor
                      value={field.value || ''}
                      onSave={() => form.handleSubmit()}
                      onChange={(val) => {
                        form.setFieldValue(field.name, val);
                      }}
                      direction={getLanguageDirection(lang.tag)}
                      onFocus={() => onFocus(lang.tag)}
                      minHeight={50}
                      scrollMargins={{ bottom: 150 }}
                      autoScrollIntoView
                    />
                  </StyledEdtorWrapper>
                </EditorWrapper>
                <FieldError error={meta.touched && meta.error} />
              </div>
            )}
          </FastField>
        ))}
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
      {hintDisplayed && targetLang && (
        <ToolsBottomPanel data={toolsData} languageTag={targetLang.tag} />
      )}
    </>
  );
};
