import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import {
  useHeaderNsContext,
  useHeaderNsDispatch,
} from './context/HeaderNsContext';

const StyledNamespace = styled('div')`
  background: ${({ theme }) => theme.palette.emphasis[50]};
`;

type Props = {
  nsBefore?: string | undefined;
  namespace: string;
  columnSizes: any;
  index: number;
};

export const NamespaceBanner: React.FC<Props> = ({
  namespace,
  columnSizes,
  index,
}) => {
  const elRef = useRef<HTMLDivElement>(null);
  const dispatch = useHeaderNsDispatch();
  const topBarHeight = useHeaderNsContext((c) => c.topBarHeight);

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
  }, [topBarHeight, columnSizes, namespace]);

  return <StyledNamespace ref={elRef}>{namespace}</StyledNamespace>;
};
