import { useErrorTranslation } from './useErrorTranslation';

type Props = {
  code: string;
};

export function TranslatedError({ code }: Props) {
  const translateError = useErrorTranslation();
  return <>{translateError(code)}</>;
}
