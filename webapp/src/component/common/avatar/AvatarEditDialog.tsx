import React from 'react';
import { ReactCropperElement } from 'react-cropper';
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import { T } from '@tolgee/react';
import { AvatarEdit, CropperOptions } from './AvatarEdit';
import Button from '@mui/material/Button';
import LoadingButton from '../form/LoadingButton';

export const AvatarEditDialog = (props: {
  src: string;
  cropperRef: React.MutableRefObject<ReactCropperElement>;
  onCancel: () => void;
  onSave: () => void;
  isUploading: boolean;
  cropperProps?: Partial<CropperOptions>;
}) => (
  <Dialog open={true} onClose={props.onCancel}>
    <DialogTitle id="transfer-dialog-title">
      <T keyName="user-profile.edit-avatar" />
    </DialogTitle>
    <DialogContent>
      <AvatarEdit
        src={props.src}
        cropperRef={props.cropperRef}
        cropperProps={props.cropperProps}
      />
    </DialogContent>
    <DialogActions>
      <Button
        data-cy="global-confirmation-cancel"
        onClick={props.onCancel}
        type="button"
      >
        <T keyName="confirmation_dialog_cancel" />
      </Button>
      <LoadingButton
        loading={props.isUploading}
        data-cy="global-confirmation-confirm"
        color="primary"
        autoFocus
        type="submit"
        onClick={props.onSave}
      >
        <T keyName="global_form_save" />
      </LoadingButton>
    </DialogActions>
  </Dialog>
);
