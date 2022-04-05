import React, { FC } from 'react';
import Cropper, { ReactCropperElement } from 'react-cropper';
import { styled } from '@mui/material';
import Box from '@mui/material/Box';
import 'cropperjs/dist/cropper.css';

const StyledBox = styled(Box)`
  .cropper-crop-box,
  .cropper-view-box {
    border-radius: 50%;
  }
`;

export const AvatarEdit: FC<{
  src: string;
  cropperRef: React.RefObject<ReactCropperElement>;
}> = ({ src, cropperRef }) => {
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
      />
    </StyledBox>
  );
};
