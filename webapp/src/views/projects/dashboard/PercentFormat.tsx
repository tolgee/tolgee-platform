import { useTranslate } from '@tolgee/react';

type Props = {
  number: number;
};

export const PercentFormat: React.FC<Props> = ({ number }) => {
  const { t } = useTranslate();
  return (
    <>
      {!isNaN(number)
        ? number > 0 && number < 1
          ? t('project_dashboard_translations_less_then_1_percent')
          : t(
              'project_dashboard_percent_count',
              '{percentage, number, :: % }',
              {
                percentage: number / 100,
              }
            )
        : t('project_dashboard_percent_nan', '-')}
    </>
  );
};
