import Dialog from '@material-ui/core/Dialog';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import { useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { useContextSelector } from 'use-context-selector';

import { ResourceErrorComponent } from 'tg.component/common/form/ResourceErrorComponent';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { TranslationsContext } from './TranslationsContext';

export type TranslationCreationValue = {
  key: string;
  translations: { [abbreviation: string]: string };
};

const messaging = container.resolve(MessageService);

type Props = {
  onClose: () => void;
  onAdd: () => void;
};

export const TranslationNewDialog: React.FC<Props> = ({ onClose, onAdd }) => {
  const projectDTO = useProject();

  const t = useTranslate();

  const createKey = useApiMutation({
    url: '/api/project/{projectId}/keys/create',
    method: 'post',
  });

  function onSubmit(values) {
    createKey.mutate(
      {
        path: { projectId: projectDTO.id },
        content: {
          'application/json': values,
        },
      },
      {
        onSuccess: () => {
          messaging.success(t('translation_grid_translation_created'));
          onAdd();
          onClose();
        },
      }
    );
  }

  const languages = useContextSelector(TranslationsContext, (v) => v.languages);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );

  if (!selectedLanguages) {
    return null;
  }

  const initialTranslations =
    selectedLanguages.reduce((res, l) => ({ ...res, [l]: '' }), {}) || {};

  return (
    <Dialog
      data-cy="translations-add-key-dialog"
      open
      onClose={() => onClose()}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
      fullWidth
    >
      <DialogTitle id="alert-dialog-title">
        {t('add_translation_dialog_title')}
      </DialogTitle>
      <DialogContent>
        {createKey.error && <ResourceErrorComponent error={createKey.error} />}

        {/* <LanguagesMenu context="creation-dialog" /> */}
        <StandardForm
          onSubmit={onSubmit}
          initialValues={{ key: '', translations: initialTranslations }}
          validationSchema={Validation.KEY_TRANSLATION_CREATION(
            selectedLanguages!
          )}
          onCancel={() => onClose()}
        >
          <TextField
            data-cy="translations-add-key-field"
            multiline
            name="key"
            label={t('translation_grid_key_text')}
            fullWidth
          />

          {languages?.map((s) => (
            <TextField
              multiline
              lang={s.tag}
              key={s.tag}
              name={'translations.' + s.tag}
              label={s.name}
            />
          ))}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
