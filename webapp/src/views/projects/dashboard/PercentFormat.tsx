import { useTranslate } from '@tolgee/react';

type Props = {
  number: number;
};

export const PercentFormat: React.FC<React.PropsWithChildren<Props>> = ({
  number,
}) => {
  const t = useTranslate();
  return (
    <>
      {!isNaN(number)
        ? number > 0 && number < 1
          ? t('project_dashboard_translations_less_then_1_percent')
          : t(
              'project_dashboard_percent_count',
              {
                percentage: number / 100,
              },
              '{percentage, number, :: % }'
            )
        : t('project_dashboard_percent_nan', '-')}
    </>
  );
};
