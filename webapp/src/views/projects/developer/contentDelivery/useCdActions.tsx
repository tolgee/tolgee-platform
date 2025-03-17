import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  getFormatById,
  normalizeSelectedMessageFormat,
} from '../../export/components/formatGroups';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { CdValues } from './getCdEditInitialValues';
import { FormikHelpers } from 'formik/dist/types';
import { ReactNode } from 'react';

interface UseCdActionsProps {
  allNamespaces?: string[];
  onClose: () => void;
}

export function useCdActions({ allNamespaces, onClose }: UseCdActionsProps) {
  const createCd = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const updateCd = useApiMutation({
    url: '/v2/projects/{projectId}/content-delivery-configs/{id}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/content-delivery-configs',
  });

  const project = useProject();

  const messaging = useMessage();

  function getRequestBody(values: CdValues) {
    const format = getFormatById(values.format);
    return {
      name: values.name,
      format: format.format,
      filterState: values.states,
      languages: values.languages,
      structureDelimiter: format.structured
        ? format.defaultStructureDelimiter
        : '',
      filterNamespace: undefinedIfAllNamespaces(
        values.namespaces,
        allNamespaces
      ),
      autoPublish: values.autoPublish,
      contentStorageId: values.contentStorageId,
      supportArrays: values.supportArrays || false,
      messageFormat:
        // strict message format is prioritized
        format.messageFormat ?? normalizeSelectedMessageFormat(values),
      slug: values.contentStorageId ? values.slug : undefined,
      pruneBeforePublish: values.pruneBeforePublish ?? true,
      escapeHtml: values.escapeHtml ?? false,
    };
  }

  function getOptions(
    formikHelpers: FormikHelpers<CdValues>,
    message: ReactNode
  ) {
    return {
      onSuccess() {
        onClose();
        messaging.success(message);
      },
      onSettled() {
        formikHelpers.setSubmitting(false);
      },
    };
  }

  return {
    create(values: CdValues, formikHelpers: FormikHelpers<CdValues>) {
      createCd.mutate(
        {
          path: { projectId: project.id },
          content: {
            'application/json': getRequestBody(values),
          },
        },
        getOptions(
          formikHelpers,
          <T keyName="content_delivery_create_success" />
        )
      );
    },
    update(
      values: CdValues,
      formikHelpers: FormikHelpers<CdValues>,
      id: number
    ) {
      updateCd.mutate(
        {
          path: { projectId: project.id, id },
          content: {
            'application/json': getRequestBody(values),
          },
        },
        getOptions(
          formikHelpers,
          <T keyName="content_delivery_update_success" />
        )
      );
    },
  };
}

function undefinedIfAllNamespaces(
  selectedNamespaces: string[],
  allNamespaces: string[] | undefined
) {
  if (!allNamespaces) {
    return selectedNamespaces;
  }
  if (selectedNamespaces.length === allNamespaces.length) {
    return undefined;
  }
  return selectedNamespaces;
}
