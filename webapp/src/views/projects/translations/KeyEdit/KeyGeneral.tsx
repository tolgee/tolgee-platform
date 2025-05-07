import { styled, useTheme } from '@mui/material';
import { useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';

import { Editor } from 'tg.component/editor/Editor';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { FieldError, FieldLabel } from 'tg.component/FormField';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { TagInput } from '../Tags/TagInput';
import { KeyFormType } from './types';
import { Tag } from '../Tags/Tag';
import { RequiredField } from 'tg.component/common/form/RequiredField';
import { LabelHint } from 'tg.component/common/LabelHint';
import { PluralFormCheckbox } from 'tg.component/common/form/PluralFormCheckbox';
import { useProject } from 'tg.hooks/useProject';
import clsx from 'clsx';

const StyledSection = styled('div')``;

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

export const KeyGeneral = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { values, setFieldValue, submitForm, errors } =
    useFormikContext<KeyFormType>();
  const theme = useTheme();

  return (
    <>
      <StyledKeyNsContainer
        className={clsx({ useNamespaces: project.useNamespaces })}
      >
        <StyledSection>
          <FieldLabel>
            <RequiredField>{t('translations_key_edit_label')}</RequiredField>
          </FieldLabel>
          <EditorWrapper data-cy="translations-key-edit-key-field">
            <Editor
              autofocus
              value={values.name}
              onChange={(val) => setFieldValue('name', val)}
              shortcuts={[
                {
                  key: 'Enter',
                  run: () => (submitForm(), true),
                },
              ]}
              mode="plain"
              minHeight="unset"
            />
          </EditorWrapper>
          <FieldError error={errors.name} />
        </StyledSection>
        {project.useNamespaces && (
          <StyledSection>
            <FieldLabel>
              <LabelHint
                title={t('translations_key_edit_label_namespace_hint')}
              >
                {t('translations_key_edit_label_namespace')}
              </LabelHint>
            </FieldLabel>
            <NamespaceSelector
              value={values.namespace}
              onChange={(value) => setFieldValue('namespace', value)}
              SearchSelectProps={{
                SelectProps: {
                  sx: { background: theme.palette.background.default },
                },
              }}
            />
            <FieldError error={errors.namespace} />
          </StyledSection>
        )}
      </StyledKeyNsContainer>

      <StyledSection>
        <FieldLabel>
          <LabelHint
            title={t('translations_key_edit_label_description_markdown_hint')}
          >
            {t('translations_key_edit_label_description')}
          </LabelHint>
        </FieldLabel>
        <EditorWrapper data-cy="translations-key-edit-description-field">
          <Editor
            value={values.description || ''}
            onChange={(val) => setFieldValue('description', val)}
            shortcuts={[
              {
                key: 'Enter',
                run: () => (submitForm(), true),
              },
            ]}
            mode="plain"
            minHeight={50}
          />
        </EditorWrapper>
        <FieldError error={errors.description} />
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

      <PluralFormCheckbox
        pluralParameterName="pluralParameter"
        isPluralName="isPlural"
      />
    </>
  );
};
