import React, { FC } from 'react';
import Cropper, { ReactCropperElement } from 'react-cropper';
import 'cropperjs/dist/cropper.css';
import Box from '@mui/material/Box';
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles(() => ({
  box: {
    '& .cropper-crop-box, & .cropper-view-box': {
      borderRadius: `50%`,
    },
  },
}));

export const AvatarEdit: FC<{
  src: string;
  cropperRef: React.RefObject<ReactCropperElement>;
}> = ({ src, cropperRef }) => {
  const classes = useStyles();

  return (
    <Box className={classes.box}>
      <Cropper
        src={src}
        style={{ height: 400, width: 400 }}
        autoCropArea={1}
        aspectRatio={1}
        viewMode={3}
        guides={false}
        ref={cropperRef}
      />
    </Box>
  );
};
