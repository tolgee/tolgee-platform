import { useCallback, useEffect, useState } from 'react';
import { Box, Button, makeStyles, Typography } from '@material-ui/core';
import { useTranslate, T } from '@tolgee/react';
import { FastField, FieldArray, FieldProps, useFormikContext } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { FieldLabel } from '../KeySingle/FieldLabel';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import clsx from 'clsx';
import { TranslationVisual } from '../TranslationVisual';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { ToolsPottomPanel } from '../TranslationTools/ToolsBottomPanel';
import { useTranslationTools } from '../TranslationTools/useTranslationTools';
import { useProject } from 'tg.hooks/useProject';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    rowGap: theme.spacing(2),
    marginBottom: theme.spacing(2),
  },
  split: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    alignItems: 'start',
  },
  field: {
    background: theme.palette.grey[100],
    border: `1px solid ${theme.palette.divider}`,
    '&:focus-within': {
      '-webkit-box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2)',
      'box-shadow': '0px 0px 10px rgba(0, 0, 0, 0.2)',
    },
    '& > *': {
      padding: theme.spacing(),
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
  const t = useTranslate();
  const form = useFormikContext<any>();
  const project = useProject();

  const onFocus = (lang: string | null) => {
    setEditedLang(lang);
  };

  const onBlur = (lang: string | null) => {
    setEditedLang((val) => (val === lang ? null : val));
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

  const classes = useStyles();
  return (
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
            <FastField name="tags">
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
                        placeholder={t('translation_single_tag_placeholder')}
                      />
                    </div>
                  </div>
                );
              }}
            </FastField>
          )}
        />
        {languages.map((lang) => (
          <FastField key={lang.tag} name={`translations.${lang.tag}`}>
            {({ field, form, meta }) => (
              <div key={lang.tag}>
                <FieldLabel>{lang.name}</FieldLabel>
                <div className={clsx(classes.field, classes.split)}>
                  <div
                    className={classes.editorWrapper}
                    data-cy="translation-create-translation-input"
                  >
                    <Editor
                      value={field.value || ''}
                      onSave={() => form.handleSubmit()}
                      onChange={(val) => {
                        form.setFieldValue(field.name, val);
                      }}
                      onBlur={() => onBlur(lang.tag)}
                      onFocus={() => onFocus(lang.tag)}
                      minHeight={50}
                      scrollMargins={{ bottom: 150 }}
                      autoScrollIntoView
                    />
                  </div>
                  <TranslationVisual
                    text={field.value}
                    locale={lang.tag}
                    width={0}
                  />
                </div>
                <Typography
                  color="error"
                  variant="caption"
                  className={classes.error}
                >
                  {meta.touched && meta.error}
                </Typography>
              </div>
            )}
          </FastField>
        ))}
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
            loading={form.isSubmitting}
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
      {hintDisplayed && <ToolsPottomPanel data={toolsData} />}
    </>
  );
};
