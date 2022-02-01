import React from 'react';
import { ReactCropperElement } from 'react-cropper';
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@material-ui/core';
import { T } from '@tolgee/react';
import { AvatarEdit } from './AvatarEdit';
import Button from '@material-ui/core/Button';
import LoadingButton from '../form/LoadingButton';

export const AvatarEditDialog = (props: {
  src: string;
  cropperRef: React.MutableRefObject<ReactCropperElement>;
  onCancel: () => void;
  onSave: () => void;
  isUploading: boolean;
}) => (
  <Dialog open={true} onClose={props.onCancel}>
    <DialogTitle id="transfer-dialog-title">
      <T>user-profile.edit-avatar</T>
    </DialogTitle>
    <DialogContent>
      <AvatarEdit src={props.src} cropperRef={props.cropperRef} />
    </DialogContent>
    <DialogActions>
      <Button
        data-cy="global-confirmation-cancel"
        onClick={props.onCancel}
        type="button"
        color="default"
      >
        <T>confirmation_dialog_cancel</T>
      </Button>
      <LoadingButton
        loading={props.isUploading}
        data-cy="global-confirmation-confirm"
        color="primary"
        autoFocus
        type="submit"
        onClick={props.onSave}
      >
        <T>global_form_save</T>
      </LoadingButton>
    </DialogActions>
  </Dialog>
);
