import { useEffect, useRef } from 'react';
import { styled } from '@mui/material';
import {
  useHeaderNsActions,
  useHeaderNsContext,
} from '../context/HeaderNsContext';
import { NamespaceContent } from './NamespaceContent';
import { NsBannerRecord } from '../context/useNsBanners';

const StyledNsRow = styled('div')`
  display: flex;
  height: 0px;
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
  maxWidth: number | undefined;
  topSpacing?: boolean;
};

export const NamespaceBanner: React.FC<Props> = ({ namespace, maxWidth }) => {
  const elRef = useRef<HTMLDivElement>(null);
  const { nsRefRegister } = useHeaderNsActions();
  const { name, row } = namespace;
  const topNamespace = useHeaderNsContext((c) => c.topNamespace);
  const isTopBanner = topNamespace?.row === namespace.row;

  useEffect(() => {
    nsRefRegister(row, elRef.current || undefined);

    return () => {
      nsRefRegister(row, undefined);
    };
  }, [maxWidth, name, row]);

  return (
    <StyledNsRow data-cy="translations-namespace-banner">
      <NamespaceContent
        maxWidth={maxWidth}
        namespace={namespace}
        ref={elRef}
        hideShadow={isTopBanner}
      />
    </StyledNsRow>
  );
};
