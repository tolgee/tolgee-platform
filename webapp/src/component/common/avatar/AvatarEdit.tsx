import React, { FC } from 'react';
import Cropper, { ReactCropperElement, ReactCropperProps } from 'react-cropper';
import { styled } from '@mui/material';
import Box from '@mui/material/Box';
import 'cropperjs/dist/cropper.css';
import clsx from 'clsx';

const StyledBox = styled(Box)`
  &.rounded .cropper-crop-box,
  &.rounded .cropper-view-box {
    border-radius: 50%;
  }
`;

export type CropperOptions = ReactCropperProps & { rounded: boolean };

export const AvatarEdit: FC<{
  src: string;
  cropperRef: React.RefObject<ReactCropperElement>;
  cropperProps?: Partial<CropperOptions>;
}> = ({ src, cropperRef, cropperProps }) => {
  const { rounded, ...other } = { ...cropperProps, rounded: true };
  return (
    <StyledBox>
      <Cropper
        src={src}
        style={{ height: 400, width: 400 }}
        autoCropArea={1}
        aspectRatio={1}
        viewMode={3}
        guides={false}
        ref={cropperRef}
        className={clsx({ rounded })}
        {...other}
      />
    </StyledBox>
  );
};
