import React, {
  createRef,
  ReactNode,
  SyntheticEvent,
  useEffect,
  useState,
} from 'react';
import { Skeleton, styled } from '@mui/material';
import Box from '@mui/material/Box';
import { Plus } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

import { ScreenshotDetail } from './ScreenshotDetail';
import { ScreenshotDropzone } from './ScreenshotDropzone';
import { ScreenshotThumbnail } from './ScreenshotThumbnail';
import { useTranslationsActions } from '../context/TranslationsContext';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { ScreenshotProps } from 'tg.component/ScreenshotWithLabels';

export interface ScreenshotGalleryProps {
  keyId: number;
}

const StyledAddIcon = styled(Plus)`
  width: 50px;
  height: 50px;
`;

const StyledAddBox = styled('div')`
  overflow: hidden;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  display: flex;
  margin: 1px;
  cursor: pointer;
  border-color: ${({ theme }) => theme.palette.emphasis[200]};
  color: ${({ theme }) => theme.palette.emphasis[200]};
  border: 1px dashed ${({ theme }) => theme.palette.emphasis[200]};

  &:hover {
    border-color: ${({ theme }) => theme.palette.primary.main};
    color: ${({ theme }) => theme.palette.primary.main};
  }

  flex: '0 0 auto';
`;

const StyledHintText = styled(Box)`
  overflow: hidden;
  // Adds a hyphen where the word breaks
  -ms-hyphens: auto;
  -moz-hyphens: auto;
  -webkit-hyphens: auto;
  hyphens: auto;
  text-align: justify;
`;

export const MAX_FILE_COUNT = 20;
const ALLOWED_UPLOAD_TYPES = ['image/png', 'image/jpeg', 'image/gif'];

export const ScreenshotGallery: React.FC<ScreenshotGalleryProps> = (props) => {
  const fileRef = createRef<HTMLInputElement>();
  const { satisfiesPermission } = useProjectPermissions();
  const canUploadScreenshots = satisfiesPermission('screenshots.upload');
  const config = useConfig();
  const project = useProject();
  const { updateScreenshotCount } = useTranslationsActions();
  const lang = useCurrentLanguage();
  const { t } = useTranslate();

  const screenshotsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots',
    method: 'get',
    path: { projectId: project.id, keyId: props.keyId },
  });

  const uploadLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots',
    method: 'post',
  });

  const [detailOpen, setDetailOpen] = useState(false);

  const [detailData, setDetailData] = useState<ScreenshotProps>();

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots/{ids}',
    method: 'delete',
  });

  const handleDelete = (id: number) => () => {
    deleteLoadable.mutate(
      {
        path: { projectId: project.id, ids: [id], keyId: props.keyId },
      },
      {
        onSuccess() {
          screenshotsLoadable.refetch();
        },
      }
    );
  };

  const addBox = canUploadScreenshots && (
    <StyledAddBox
      key="add"
      data-cy="add-box"
      //@ts-ignore
      onClick={() => fileRef.current.dispatchEvent(new MouseEvent('click'))}
    >
      <StyledAddIcon />
    </StyledAddBox>
  );

  const validate = (files: File[]) => {
    const result = {
      valid: false,
      errors: [] as ReactNode[],
    };

    if (files.length > MAX_FILE_COUNT) {
      result.errors.push(
        <T keyName="translations.screenshots.validation.too_many_files" />
      );
    }

    files.forEach((file) => {
      if (file.size > config.maxUploadFileSize * 1024) {
        result.errors.push(
          <T
            keyName="translations.screenshots.validation.file_too_big"
            params={{ filename: file.name }}
          />
        );
      }
      if (ALLOWED_UPLOAD_TYPES.indexOf(file.type) < 0) {
        result.errors.push(
          <T
            keyName="translations.screenshots.validation.unsupported_format"
            params={{ filename: file.name }}
          />
        );
      }
    });

    const valid = result.errors.length === 0;
    return { ...result, valid };
  };

  const validateAndUpload = async (files: File[]) => {
    const validation = validate(files);
    let errorHappened = false;
    if (validation.valid) {
      await Promise.all(
        files.map((file) =>
          uploadLoadable
            .mutateAsync({
              path: { projectId: project.id, keyId: props.keyId },
              content: {
                'multipart/form-data': {
                  screenshot: file as any,
                },
              },
            })
            .catch((e) => {
              // eslint-disable-next-line no-console
              console.error(e);
              errorHappened = true;
            })
        )
      );

      if (errorHappened) {
        messageService.error(
          <T keyName="translations.screenshots.some_screenshots_not_uploaded" />
        );
      }
      screenshotsLoadable.refetch();
    } else {
      validation.errors.forEach((e) => messageService.error(e));
    }
  };

  useEffect(() => {
    const listener = (e) => {
      e.preventDefault();
    };

    const pasteListener = (e: ClipboardEvent) => {
      const files: File[] = [];
      if (e.clipboardData == null) {
        return;
      }
      for (let i = 0; i < e.clipboardData.files.length; i++) {
        const item = e.clipboardData.files.item(i);
        if (item) {
          files.push(item);
        }
      }
      validateAndUpload(files);
    };

    window.addEventListener('dragover', listener, false);
    window.addEventListener('drop', listener, false);
    document.addEventListener('paste', pasteListener);

    return () => {
      window.removeEventListener('dragover', listener, false);
      window.removeEventListener('drop', listener, false);
      document.removeEventListener('paste', pasteListener);
    };
  }, []);

  useEffect(() => {
    if (screenshotsLoadable.data) {
      updateScreenshotCount({
        keyId: props.keyId,
        screenshotCount:
          screenshotsLoadable.data._embedded?.screenshots?.length,
      });
    }
  }, [screenshotsLoadable.data?._embedded?.screenshots?.length]);

  function onFileSelected(e: SyntheticEvent) {
    const files = (e.target as HTMLInputElement).files;
    if (!files) {
      return;
    }
    const toUpload: File[] = [];
    for (let i = 0; i < files.length; i++) {
      const item = files.item(i);
      if (item) {
        toUpload.push(item);
      }
    }
    validateAndUpload(toUpload);
  }

  const loadingSkeleton = uploadLoadable.isLoading ? (
    <Skeleton variant="rectangular" width={100} height={100} />
  ) : null;

  return (
    <>
      <input
        type="file"
        style={{ display: 'none' }}
        ref={fileRef}
        onChange={(e) => onFileSelected(e)}
        multiple
        accept={ALLOWED_UPLOAD_TYPES.join(',')}
      />
      <ScreenshotDropzone validateAndUpload={validateAndUpload}>
        {screenshotsLoadable.isLoading || !screenshotsLoadable.data ? (
          <BoxLoading />
        ) : Number(screenshotsLoadable.data?._embedded?.screenshots?.length) >
            0 || uploadLoadable.isLoading ? (
          <Box display="flex" flexWrap="wrap" overflow="visible">
            {screenshotsLoadable.data?._embedded?.screenshots?.map((s) => {
              const screenshot: ScreenshotProps = {
                src: config.screenshotsUrl + '/' + s.thumbnail,
                width: s.width!,
                height: s.height!,
                highlightedKeyId: props.keyId,
                keyReferences: s.keyReferences,
              };
              return (
                <ScreenshotThumbnail
                  key={s.id}
                  onClick={() => {
                    setDetailOpen(true);
                    setDetailData({
                      ...screenshot,
                      src: config.screenshotsUrl + '/' + s.filename,
                    });
                  }}
                  screenshot={screenshot}
                  onDelete={handleDelete(s.id)}
                />
              );
            })}
            {loadingSkeleton}
            {addBox}
          </Box>
        ) : (
          <>
            {addBox}
            <StyledHintText
              display="flex"
              alignItems="center"
              justifyContent="center"
              flexGrow={1}
              p={2}
              lang={lang}
            >
              {t('no_screenshots_yet')}{' '}
              {canUploadScreenshots && t('add_screenshots_message')}
            </StyledHintText>
          </>
        )}
      </ScreenshotDropzone>
      <ScreenshotDetail
        open={detailOpen}
        screenshot={detailData}
        onClose={() => setDetailOpen(false)}
        highlightedKeyId={props.keyId}
      />
    </>
  );
};
