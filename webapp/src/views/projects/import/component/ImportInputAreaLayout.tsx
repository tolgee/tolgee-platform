import { Box, styled, Typography } from '@mui/material';
import React, { FC, ReactNode } from 'react';

export const ImportInputAreaLayout = styled(Box)`
  justify-content: center;
  align-items: center;
  flex-direction: column;
  display: flex;
  padding-top: 40px;
  padding-bottom: 40px;
  height: 100%;
`;

export const ImportInputAreaLayoutCenter = styled(Box)`
  height: 76px;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const ImportInputAreaLayoutTop = styled(Box)`
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const ImportInputAreaLayoutBottom = styled(Box)`
  min-height: 24px;
`;

export const ImportInputAreaLayoutTitle: FC<{
  icon?: ReactNode;
}> = (props) => {
  return (
    <>
      <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
        {props.children}
      </Typography>
      {props.icon}
    </>
  );
};
