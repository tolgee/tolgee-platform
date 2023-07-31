import { useTranslate } from '@tolgee/react';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';

type Props = Omit<React.ComponentProps<typeof LanguagesSelect>, 'context'>;

export function BatchOperationsLanguagesSelect(props: Props) {
  const { t } = useTranslate();

  return (
    <LanguagesSelect
      enableEmpty
      placeholder={t('batch_operations_select_languages_placeholder')}
      context="batch-operations"
      placement="top"
      {...props}
    />
  );
}
