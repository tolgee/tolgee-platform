import { DiffValue } from '../types';
import { useTranslate } from '@tolgee/react';
import { getNoDiffChange } from './getNoDiffChange';
import { getGeneralChange } from './getGeneralChange';

type Props = {
  input: DiffValue<any>;
  diffEnabled: boolean;
};

const NamespaceComponent: React.FC<Props> = ({ input, diffEnabled }) => {
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

  return (
    <>
      {diffEnabled
        ? getGeneralChange(transformed)
        : getNoDiffChange(transformed)}
    </>
  );
};

export const getNamespaceChange = (
  input: DiffValue<any>,
  diffEnabled: boolean
) => {
  return <NamespaceComponent input={input} diffEnabled={diffEnabled} />;
};
