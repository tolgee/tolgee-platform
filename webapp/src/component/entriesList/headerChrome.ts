import { styled } from '@mui/material';

// Sticky grid header used by Glossary terms list and TM entries list. `margin-bottom: -1px`
// collapses the header's bottom border with the first row's top border.
export const HeaderRow = styled('div')`
  position: sticky;
  top: 0;
  z-index: ${({ theme }) => theme.zIndex.fab - 1};
  background: ${({ theme }) => theme.palette.background.default};
  display: grid;
  margin-bottom: -1px;
`;

export const HeaderCell = styled('div')`
  display: flex;
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const HeaderDataCell = styled(HeaderCell)`
  border-left: 1px solid ${({ theme }) => theme.palette.divider1};
`;
