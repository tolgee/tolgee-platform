import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  findByExportParams,
  getFormatById,
  getStructureDelimiter,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';
import { getTagFilterParams } from 'tg.views/projects/export/exportSettings';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import {
  CdValues,
  ContentDeliveryConfigModel,
} from 'tg.views/projects/developer/contentDelivery/getCdEditInitialValues';
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

  function getRequestBody(
    values: CdValues,
    original?: ContentDeliveryConfigModel
  ) {
    const format = getFormatById(values.format);
    const keepsFormat =
      original !== undefined && findByExportParams(original).id === format.id;
    const formParams = {
      name: values.name,
      format: format.format,
      filterState: values.states,
      languages: values.languages,
      structureDelimiter: keepsFormat
        ? original.structureDelimiter
        : getStructureDelimiter(format),
      fileStructureTemplate: keepsFormat
        ? original.fileStructureTemplate
        : undefined,
      filterNamespace: undefinedIfAllNamespaces(
        values.namespaces,
        allNamespaces
      ),
      ...getTagFilterParams(values),
      autoPublish: values.autoPublish,
      contentStorageId: values.contentStorageId,
      supportArrays: values.supportArrays || false,
      messageFormat:
        // strict message format is prioritized
        format.messageFormat ?? normalizeSelectedMessageFormat(values),
      slug: values.contentStorageId ? values.slug : undefined,
      pruneBeforePublish: values.pruneBeforePublish ?? true,
      zip: values.zip ?? false,
      escapeHtml: values.escapeHtml ?? false,
      filterBranch: values.filterBranch || undefined,
    };
    return { ...getSavedExportParams(original), ...formParams };
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
      original: ContentDeliveryConfigModel
    ) {
      updateCd.mutate(
        {
          path: { projectId: project.id, id: original.id },
          content: {
            'application/json': getRequestBody(values, original),
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

function getSavedExportParams(original?: ContentDeliveryConfigModel) {
  if (!original) {
    return {};
  }
  const {
    id: _id,
    storage: _storage,
    publicUrl: _publicUrl,
    lastPublished: _lastPublished,
    lastPublishedFiles: _lastPublishedFiles,
    branchName: _branchName,
    filterTag: _filterTag,
    ...exportParams
  } = original;
  return exportParams;
}
