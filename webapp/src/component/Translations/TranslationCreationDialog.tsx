import { useContext } from 'react';
import Dialog from '@material-ui/core/Dialog';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import { useProject } from '../../hooks/useProject';
import { LINKS, PARAMS } from '../../constants/links';
import { container } from 'tsyringe';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { StandardForm } from '../common/form/StandardForm';
import { LanguagesMenu } from '../common/form/LanguagesMenu';
import { TranslationActions } from '../../store/project/TranslationActions';
import { TextField } from '../common/form/fields/TextField';
import { ResourceErrorComponent } from '../common/form/ResourceErrorComponent';
import { MessageService } from '../../service/MessageService';
import { Validation } from '../../constants/GlobalValidationSchema';
import { TranslationListContext } from './TtranslationsGridContextProvider';
import { useTranslate } from '@tolgee/react';
import { useCreateKey } from '../../service/hooks/Translation';

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

  const createKey = useCreateKey(projectDTO.id);

  function onSubmit(v) {
    createKey.mutate(v, {
      onSuccess: () => {
        messaging.success(t('translation_grid_translation_created'));
        listContext.loadData();
        onClose();
      },
    });
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

  const initialTranslations = selectedLanguages.reduce(
    (res, l) => ({ ...res, [l]: '' }),
    {}
  );

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
            selectedLanguages
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

          {selectedLanguages.map((s) => (
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
