import { Box, IconButton, styled } from '@mui/material';
import React, { createRef, FC, useRef, useState } from 'react';
import { Edit02 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { ReactCropperElement } from 'react-cropper';
import { messageService } from 'tg.service/MessageService';
import { AvatarImg } from './AvatarImg';
import { AvatarEditMenu } from './AvatarEditMenu';
import { AvatarEditDialog } from './AvatarEditDialog';
import { useConfig } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';

export type AvatarOwner = {
  name?: string;
  id: number | string;
  avatar?: components['schemas']['Avatar'];
  type: 'ORG' | 'USER' | 'PROJECT';
  deleted?: boolean;
};

const StyledEditButton = styled(IconButton)`
  opacity: 0;
  background: rgba(234, 234, 234, 0.63) !important;
  transition: opacity 0.2s ease-in-out;
  color: #000000;
`;

const EditButtonWrapper = styled(Box)`
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0px;
  left: 0px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const StyledBox = styled(Box)`
  position: relative;

  &:hover .button {
    opacity: 1;
  }
`;

const file2Base64 = (file: File): Promise<string> => {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result?.toString() || '');
    reader.onerror = (error) => reject(error);
  });
};

const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

export const ProfileAvatar: FC<{
  disabled?: boolean;
  onUpload: (blob: Blob) => Promise<any>;
  onRemove: () => Promise<any>;
  owner: AvatarOwner;
}> = (props) => {
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
      } catch (e: any) {
        // eslint-disable-next-line no-console
        console.error(e);
        if (e.code == 'file_too_big') {
          messageService.error(<T keyName="file_too_big" />);
        }
        messageService.error(<T keyName="global-upload-not-successful" />);
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
        messageService.error(<T keyName="file_too_big" />);
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
      <StyledBox
        display="inline-block"
        onClick={() => {
          setAvatarMenuAnchorEl(editAvatarRef.current);
        }}
        sx={{ cursor: props.disabled ? 'default' : 'pointer' }}
      >
        <AvatarImg owner={props.owner} size={200} />
        {!props.disabled && (
          <EditButtonWrapper>
            <StyledEditButton
              data-cy="avatar-menu-open-button"
              size="small"
              ref={editAvatarRef as any}
              className="button"
            >
              <Edit02 />
            </StyledEditButton>
          </EditButtonWrapper>
        )}
      </StyledBox>
      <AvatarEditMenu
        canRemove={!!props.owner.avatar}
        anchorEl={avatarMenuAnchorEl}
        onUpload={() => {
          fileRef.current?.click();
          setAvatarMenuAnchorEl(undefined);
        }}
        onRemove={async () => {
          await props.onRemove();
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
