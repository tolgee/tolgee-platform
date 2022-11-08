import { DiffValue } from '../types';
import { getNoDiffChange } from './getNoDiffChange';
import { useTranslate } from '@tolgee/react';

type Props = {
  input: DiffValue<any>;
};

const NamespaceComponent: React.FC<Props> = ({ input }) => {
  const t = useTranslate();

  function getName(namespace: string | undefined) {
    if (namespace === undefined) {
      return t({ key: 'namespace_default', noWrap: true });
    }
    return namespace;
  }

  const transformed = {
    old: getName(input?.old?.data?.name),
    new: getName(input?.new?.data?.name),
  };

  return <>{getNoDiffChange(transformed)}</>;
};

export const getNamespaceChange = (input: DiffValue<any>) => {
  return <NamespaceComponent input={input} />;
};
