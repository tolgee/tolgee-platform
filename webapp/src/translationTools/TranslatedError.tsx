import { useErrorTranslation } from './useErrorTranslation';

type Props = {
  code: string;
  params?: string[];
};

export function TranslatedError({ code, params }: Props) {
  const translateError = useErrorTranslation();
  return <>{translateError(code, params)}</>;
}
