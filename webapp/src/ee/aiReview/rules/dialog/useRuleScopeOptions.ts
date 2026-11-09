import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';

export const useRuleScopeOptions = () => {
  const { t } = useTranslate();
  const project = useProject();
  const isBranchingEnabled = useIsBranchingEnabled();

  const tagsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const tagOptions = [
    '',
    ...(tagsLoadable.data?._embedded?.tags?.map((tag) => tag.name) ?? []),
  ];

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
  });
  const namespaceOptions = [
    '',
    ...(namespacesLoadable.data?._embedded?.namespaces
      ?.map((namespace) => namespace.name ?? '')
      .filter((name) => name !== '') ?? []),
  ];

  const branchesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches',
    method: 'get',
    path: { projectId: project.id },
    query: { page: 0, size: 1000 },
    options: { enabled: isBranchingEnabled },
  });
  const branchOptions =
    branchesLoadable.data?._embedded?.branches?.map((branch) => branch.name) ??
    [];

  const withoutTagsLabel = t(
    'ai_review_rule_form_tags_without',
    'Without tags'
  );
  const defaultNamespaceLabel = t(
    'ai_review_rule_form_namespaces_default',
    'Default namespace'
  );

  return {
    isBranchingEnabled,
    tagOptions,
    namespaceOptions,
    branchOptions,
    withoutTagsLabel,
    defaultNamespaceLabel,
  };
};
