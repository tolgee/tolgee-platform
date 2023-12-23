import { Box, styled, Typography } from '@mui/material';
import React, { FC, ReactNode } from 'react';

export const ImportInputAreaLayout = styled(Box)`
  justify-content: center;
  align-items: center;
  flex-direction: column;
  display: flex;
  padding-top: 20px;
  padding-bottom: 20px;
  height: 100%;
`;

export const ImportInputAreaLayoutCenter = styled(Box)`
  height: 50px;
  width: 100%;
  display: flex;
  justify-content: center;
  margin-top: 8px;
  margin-bottom: 8px;
  align-items: center;
`;

export const ImportInputAreaLayoutTop = styled(Box)`
  height: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const ImportInputAreaLayoutBottom = styled(Box)`
  min-height: 40px;
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
