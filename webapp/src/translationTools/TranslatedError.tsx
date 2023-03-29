import { useTranslate } from '@tolgee/react';

type Props = {
  code: string;
};

export function TranslatedError({ code }: Props) {
  const { t } = useTranslate();

  return (
    <>
      {(() => {
        switch (code) {
          default:
            return code;
        }
      })()}
    </>
  );
}
