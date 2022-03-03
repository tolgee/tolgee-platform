import { Box, IconButton } from '@material-ui/core';
import React, { createRef, FC, useRef, useState } from 'react';
import makeStyles from '@material-ui/core/styles/makeStyles';
import EditIcon from '@material-ui/icons/Edit';
import { T } from '@tolgee/react';
import { ReactCropperElement } from 'react-cropper';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { AvatarImg } from './AvatarImg';
import { AvatarEditMenu } from './AvatarEditMenu';
import { AvatarEditDialog } from './AvatarEditDialog';
import { useConfig } from 'tg.hooks/useConfig';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { components } from 'tg.service/apiSchema.generated';
import { AutoAvatarType } from './AutoAvatar';

export type AvatarOwner = {
  name?: string;
  id: number | string;
  avatar?: components['schemas']['Avatar'];
  type: 'ORG' | 'USER' | 'PROJECT';
};

const useStyles = makeStyles((theme) => ({
  editButton: {
    opacity: 0,
  },
  editButtonWrapper: {
    position: 'absolute',
    width: '100%',
    height: '100%',
    top: 0,
    left: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    cursor: 'pointer',
    '&:hover $editButton': {
      backgroundColor: 'rgba(234,234,234,0.63)',
      opacity: 1,
      color: '#000000',
    },
    position: 'relative',
  },
}));

const file2Base64 = (file: File): Promise<string> => {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result?.toString() || '');
    reader.onerror = (error) => reject(error);
  });
};

const messageService = container.resolve(MessageService);
const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

export const ProfileAvatar: FC<{
  onUpload: (blob: Blob) => Promise<any>;
  onRemove: () => Promise<any>;
  owner: AvatarOwner;
  autoAvatarType: AutoAvatarType;
  circle?: boolean;
}> = (props) => {
  const classes = useStyles();
  const fileRef = createRef<HTMLInputElement>();
  const [uploaded, setUploaded] = useState(null as string | null | undefined);
  const cropperRef = createRef<ReactCropperElement>();
  const [uploading, setUploading] = useState(false);
  const config = useConfig();

  const onSave = () => {
    const imageElement: any = cropperRef?.current;
    const cropper: any = imageElement?.cropper;
    setUploading(true);
    cropper.getCroppedCanvas().toBlob(async (blob) => {
      try {
        await props.onUpload(blob);
        setUploaded(undefined);
        setAvatarMenuAnchorEl(undefined);
      } catch (e) {
        // eslint-disable-next-line no-console
        console.error(e);
        if (e.code == 'file_too_big') {
          messageService.error(<T>file_too_big</T>);
        }
        messageService.error(<T>global-upload-not-successful</T>);
      } finally {
        setUploading(false);
        if (fileRef.current) {
          fileRef.current.files = new DataTransfer().files;
        }
      }
    });
  };

  const editAvatarRef = useRef<HTMLButtonElement | null | undefined>();
  const [avatarMenuAnchorEl, setAvatarMenuAnchorEl] = useState(
    undefined as Element | undefined | null
  );

  const onFileInputChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const file = e.target?.files?.[0];
    if (file) {
      if (file.size > config.maxUploadFileSize * 1024) {
        messageService.error(<T>file_too_big</T>);
        return;
      }
      file2Base64(file).then((base64) => {
        setUploaded(base64);
      });
    }
  };

  return (
    <>
      <input
        data-cy={'avatar-upload-file-input'}
        type="file"
        style={{ display: 'none' }}
        ref={fileRef}
        onChange={onFileInputChange}
        accept={ALLOWED_UPLOAD_TYPES.join(',')}
      />
      <Box
        className={classes.box}
        display="inline-block"
        onClick={() => {
          setAvatarMenuAnchorEl(editAvatarRef.current);
        }}
      >
        <AvatarImg
          owner={props.owner}
          size={200}
          autoAvatarType={props.autoAvatarType}
          circle={props.circle}
        />
        <Box className={classes.editButtonWrapper}>
          <IconButton
            data-cy="avatar-menu-open-button"
            size="small"
            className={classes.editButton}
            ref={editAvatarRef as any}
          >
            <EditIcon />
          </IconButton>
        </Box>
      </Box>
      <AvatarEditMenu
        canRemove={!!props.owner.avatar}
        anchorEl={avatarMenuAnchorEl}
        onUpload={() => {
          fileRef.current?.click();
          setAvatarMenuAnchorEl(undefined);
        }}
        onRemove={async () => {
          try {
            await props.onRemove();
          } catch (e) {
            // eslint-disable-next-line no-console
            console.error(e);
            const parsed = parseErrorResponse(e);
            parsed.forEach((error) => messageService.error(<T>{error}</T>));
          }
          setAvatarMenuAnchorEl(undefined);
        }}
        onClose={() => setAvatarMenuAnchorEl(undefined)}
      />
      {uploaded && (
        <AvatarEditDialog
          src={uploaded}
          cropperRef={cropperRef as any}
          isUploading={uploading}
          onCancel={() => {
            setUploaded(undefined);
          }}
          onSave={() => {
            onSave();
          }}
        />
      )}
    </>
  );
};
