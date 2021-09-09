import clsx from 'clsx';
import { makeStyles, Typography } from '@material-ui/core';
import { T } from '@tolgee/react';
import { FastField, FieldProps } from 'formik';

import { Editor } from 'tg.component/editor/Editor';
import { TranslationVisual } from '../TranslationVisual';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    rowGap: theme.spacing(2),
    marginBottom: theme.spacing(2),
  },
  label: {
    fontWeight: 'bold',
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
  error: {
    display: 'flex',
    minHeight: '1.2rem',
  },
}));

export type LanguageType = {
  tag: string;
  name: string;
};

type Props = {
  languages: LanguageType[];
};

export const TranslationsForm: React.FC<Props> = ({ languages }) => {
  const classes = useStyles();
  return (
    <div className={classes.container}>
      <FastField name="key">
        {({ field, form, meta }: FieldProps<any>) => {
          return (
            <div>
              <div className={classes.label}>
                <T>translation_grid_key_text</T>
              </div>
              <div className={classes.field}>
                <div className={classes.editorWrapper}>
                  <Editor
                    plaintext
                    value={field.value}
                    onChange={(val) => form.setFieldValue(field.name, val)}
                    onBlur={() => form.setFieldTouched(field.name, true)}
                    minHeight="unset"
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
      {languages.map((lang) => {
        return (
          <FastField key={lang.tag} name={`translations.${lang.tag}`}>
            {({ field, form, meta }) => (
              <div key={lang.tag}>
                <div className={classes.label}>{lang.name}</div>
                <div className={clsx(classes.field, classes.split)}>
                  <div className={classes.editorWrapper}>
                    <Editor
                      value={field.value || ''}
                      onChange={(val) => form.setFieldValue(field.name, val)}
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
        );
      })}
    </div>
  );
};
