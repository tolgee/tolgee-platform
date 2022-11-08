import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import { useHeaderNsDispatch } from '../context/HeaderNsContext';
import { useTranslate } from '@tolgee/react';
import { useNamespaceFilter } from './useNamespaceFilter';

const StyledNamespace = styled('div')`
  cursor: pointer;
  background: ${({ theme }) => theme.palette.emphasis[50]};
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px 0px 1px 0px;
  padding: ${({ theme }) => theme.spacing(0, 1)};
  margin-bottom: -1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  namespace: string;
  columnSizes: any;
  index: number;
};

export const NamespaceBanner: React.FC<Props> = ({
  namespace,
  index,
  columnSizes,
}) => {
  const t = useTranslate();
  const elRef = useRef<HTMLDivElement>(null);
  const dispatch = useHeaderNsDispatch();

  const { toggle } = useNamespaceFilter(namespace);

  useEffect(() => {
    dispatch({
      type: 'NS_REF_REGISTER',
      payload: { index, el: elRef.current || undefined },
    });

    return () => {
      dispatch({
        type: 'NS_REF_REGISTER',
        payload: { index, el: undefined },
      });
    };
  }, [columnSizes, namespace, index]);

  return (
    <StyledNamespace
      role="button"
      onClick={toggle}
      ref={elRef}
      data-cy="translations-namespace-banner"
    >
      {namespace || t('namespace_default')}
    </StyledNamespace>
  );
};
