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

export const KeyGeneral = () => {
  const { t } = useTranslate();
  const { values, setFieldValue, submitForm, errors } =
    useFormikContext<KeyFormType>();
  const theme = useTheme();

  return (
    <>
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
        <FieldLabel>{t('translations_key_edit_label_namespace')}</FieldLabel>
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
    </>
  );
};
