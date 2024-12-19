import { Alert, AlertTitle } from '@mui/material';
import { useImportWarningTranslation } from 'tg.translationTools/useImportWarningTranslation';

type Props = {
  code: string;
};

export function TranslatedWarningBox({ code }: Props) {
  const importWarningTranslation = useImportWarningTranslation();
  const translation = importWarningTranslation(code);

  return (
    <Alert severity="warning">
      <AlertTitle>{translation.title}</AlertTitle>
      {translation.message}
    </Alert>
  );
}
