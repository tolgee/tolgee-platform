import { useTranslate } from '@tolgee/react';

export const useSortOptions = () => {
  const { t } = useTranslate();
  return [
    { value: 'keyName', label: t('translation_sort_item_key_name_a_to_z') },
    {
      value: 'keyName,desc',
      label: t('translation_sort_item_key_name_z_to_a'),
    },
    { value: 'createdAt', label: t('translation_sort_item_first_added') },
    { value: 'createdAt,desc', label: t('translation_sort_item_last_added') },
  ];
};
