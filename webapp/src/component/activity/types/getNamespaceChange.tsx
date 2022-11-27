import { DiffValue } from '../types';
import { getNoDiffChange } from './getNoDiffChange';
import { getGeneralChange } from './getGeneralChange';

type Props = {
  input: DiffValue<any>;
  diffEnabled: boolean;
};

const NamespaceComponent: React.FC<Props> = ({ input, diffEnabled }) => {
  function getName(namespace: string | undefined) {
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
