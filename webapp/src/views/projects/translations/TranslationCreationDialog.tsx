import { useContext } from 'react';
import Dialog from '@material-ui/core/Dialog';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import { useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { ResourceErrorComponent } from 'tg.component/common/form/ResourceErrorComponent';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';
import { TranslationActions } from 'tg.store/project/TranslationActions';

import { TranslationListContext } from './TtranslationsGridContextProvider';

export type TranslationCreationValue = {
  key: string;
  translations: { [abbreviation: string]: string };
};

const redirectionActions = container.resolve(RedirectionActions);
const translationActions = container.resolve(TranslationActions);
const messaging = container.resolve(MessageService);

export function TranslationCreationDialog() {
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
          listContext.loadData();
          onClose();
        },
      }
    );
  }

  const selectedLanguages = translationActions.useSelector(
    (s) => s.selectedLanguages
  );

  const listContext = useContext(TranslationListContext);

  function onClose() {
    redirectionActions.redirect.dispatch(
      LINKS.PROJECT_TRANSLATIONS.build({
        [PARAMS.PROJECT_ID]: projectDTO.id,
      })
    );
  }

  const initialTranslations =
    selectedLanguages!.reduce((res, l) => ({ ...res, [l]: '' }), {}) || {};

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

        <LanguagesMenu context="creation-dialog" />
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

          {listContext.listLanguages.map((s) => (
            <TextField
              multiline
              lang={s}
              key={s}
              name={'translations.' + s}
              label={s}
            />
          ))}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
}
