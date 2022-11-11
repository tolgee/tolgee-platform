import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import { useHeaderNsDispatch } from '../context/HeaderNsContext';
import { NamespaceContent } from './NamespaceContent';

const StyledNsRow = styled('div')`
  display: flex;
  background: ${({ theme }) => theme.palette.emphasis[50]};
  height: 17px;
  overflow: visible;
  position: relative;
  margin-bottom: -1px;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  ::after {
    content: '';
    position: absolute;
    height: 24px;
    width: 20px;
    top: -3px;
    left: 0px;
    background: ${({ theme }) => theme.palette.background.default};
  }
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
    <StyledNsRow>
      <NamespaceContent namespace={namespace} ref={elRef} />
    </StyledNsRow>
  );
};
