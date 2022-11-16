import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import { useHeaderNsDispatch } from '../context/HeaderNsContext';
import { NamespaceContent } from './NamespaceContent';
import { NsBannerRecord } from '../context/useNsBanners';

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
  namespace: NsBannerRecord;
  columnSizes: any;
};

export const NamespaceBanner: React.FC<Props> = ({
  namespace,
  columnSizes,
}) => {
  const elRef = useRef<HTMLDivElement>(null);
  const dispatch = useHeaderNsDispatch();
  const { name, row } = namespace;

  useEffect(() => {
    dispatch({
      type: 'NS_REF_REGISTER',
      payload: { index: row, el: elRef.current || undefined },
    });

    return () => {
      dispatch({
        type: 'NS_REF_REGISTER',
        payload: { index: row, el: undefined },
      });
    };
  }, [columnSizes, name, row]);

  return (
    <StyledNsRow data-cy="translations-namespace-banner">
      <NamespaceContent namespace={namespace} ref={elRef} />
    </StyledNsRow>
  );
};
