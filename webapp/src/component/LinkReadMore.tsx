import { useTranslate } from '@tolgee/react';
import { LinkExternal } from './LinkExternal';

type Props = {
  url: string;
};

export const LinkReadMore = ({ url }: Props) => {
  const { t } = useTranslate();
  return <LinkExternal href={url}>{t('global_read_more_link')}</LinkExternal>;
};
