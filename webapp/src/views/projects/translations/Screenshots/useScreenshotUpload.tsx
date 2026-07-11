import { ReactNode, RefObject, SyntheticEvent } from 'react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ALLOWED_UPLOAD_TYPES, MAX_FILE_COUNT } from './Screenshots';
import { T } from '@tolgee/react';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';
import { messageService } from 'tg.service/MessageService';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

type Props = {
  keyId: number;
  fileRef: RefObject<HTMLInputElement>;
};

export const useScreenshotUpload = ({ keyId, fileRef }: Props) => {
  const { updateScreenshots } = useTranslationsActions();
  const project = useProject();
  const config = useConfig();
  const uploadLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots',
    method: 'post',
  });

  const screenshotsLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/screenshots',
    method: 'get',
  });

  function onFileSelected(e: SyntheticEvent) {
    const files = (e.target as HTMLInputElement).files;
    if (!files) {
      return;
    }
    const toUpload: FilesType = [];
    for (let i = 0; i < files.length; i++) {
      const item = files.item(i);
      if (item) {
        toUpload.push({ file: item, name: item.name });
      }
    }
    validateAndUpload(toUpload);
  }

  const validate = (files: FilesType) => {
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
      if (file.file.size > config.maxUploadFileSize * 1024) {
        result.errors.push(
          <T
            keyName="translations.screenshots.validation.file_too_big"
            params={{ filename: file.name }}
          />
        );
      }
      if (ALLOWED_UPLOAD_TYPES.indexOf(file.file.type) < 0) {
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

  const validateAndUpload = async (files: FilesType) => {
    const validation = validate(files);
    let errorHappened = false;
    if (validation.valid) {
      await Promise.all(
        files.map((file) =>
          uploadLoadable
            .mutateAsync({
              path: { projectId: project.id, keyId },
              content: {
                'multipart/form-data': {
                  screenshot: file.file as any,
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

      screenshotsLoadable.mutate(
        {
          path: { keyId, projectId: project.id },
        },
        {
          onSuccess: (data) => {
            updateScreenshots({
              keyId,
              screenshots: data._embedded?.screenshots || [],
            });
          },
        }
      );

      if (errorHappened) {
        messageService.error(
          <T keyName="translations.screenshots.some_screenshots_not_uploaded" />
        );
      }
    } else {
      validation.errors.forEach((e) => messageService.error(e));
    }
  };

  function openFiles() {
    fileRef.current?.dispatchEvent(new MouseEvent('click'));
  }

  return {
    openFiles,
    validateAndUpload,
    onFileSelected,
  };
};
