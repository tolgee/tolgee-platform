import { useTranslate } from '@tolgee/react';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { ScopeWithLanguage } from 'tg.fixtures/permissions';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useTranslationsSelector } from '../../context/TranslationsContext';

type Props = Omit<React.ComponentProps<typeof LanguagesSelect>, 'context'> & {
  languagePermission?: ScopeWithLanguage;
};

export function BatchOperationsLanguagesSelect({
  languagePermission,
  ...props
}: Props) {
  const { t } = useTranslate();

  const permissions = useProjectPermissions();
  const languages = useTranslationsSelector((c) => c.languages) || [];
  const disabledLanguages = languagePermission
    ? languages
        .filter(
          ({ id }) =>
            !permissions.satisfiesLanguageAccess(languagePermission, id) ||
            props.disabledLanguages?.includes(id)
        )
        .map((l) => l.id)
    : props.disabledLanguages;

  return (
    <LanguagesSelect
      enableEmpty
      placeholder={t('batch_operations_select_languages_placeholder')}
      context="batch-operations"
      placement="top"
      {...props}
      disabledLanguages={disabledLanguages}
    />
  );
}
