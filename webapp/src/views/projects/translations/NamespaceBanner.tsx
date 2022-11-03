import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import { useHeaderNsDispatch } from './context/HeaderNsContext';
import { useTranslate } from '@tolgee/react';

const StyledNamespace = styled('div')`
  background: ${({ theme }) => theme.palette.emphasis[50]};
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
    <StyledNamespace ref={elRef}>
      {namespace || t('namespace_default')}
    </StyledNamespace>
  );
};
