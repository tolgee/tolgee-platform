import React, { FC, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Badge,
  BadgeProps,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  Menu,
  MenuItem,
  styled,
  Tab,
  Tabs,
  Typography,
  useTheme,
} from '@mui/material';
import {
  AlertTriangle,
  CheckCircle,
  DotsVertical,
  Minus,
  Plus,
  RefreshCcw02,
} from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { useHistory, useParams } from 'react-router-dom';

import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import {
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { messageService } from 'tg.service/MessageService';
import { SimpleCellKey } from 'tg.views/projects/translations/SimpleCellKey';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { CellTranslation } from 'tg.views/projects/translations/TranslationsList/CellTranslation';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';
import clsx from 'clsx';
import { Branch, CheckDone } from 'tg.component/CustomIcons';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { SuccessChip } from 'tg.component/common/chips/SuccessChip';

const BranchesRow = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
  flex-wrap: wrap;
`;

const StatsRow = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(3)};
`;

const StatCard = styled(Box)`
  display: flex;
  flex: 1;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  border-radius: 20px;
  gap: ${({ theme }) => theme.spacing(0.5)};
  padding: ${({ theme }) => theme.spacing(2)};
  background-color: ${({ theme }) => theme.palette.tokens.background.hover};
`;

const ConflictsWrapper = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const TabsWrapper = styled(Box)`
  margin-top: ${({ theme }) => theme.spacing(3)};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const ChangeCard = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const ConflictsHeader = styled(Box)`
  display: flex;
`;

const ConflictsHeaderColumn = styled(Box)`
  display: flex;
  flex: 1;
  gap: 15px;
  align-items: center;
`;

const ConflictColumns = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const KeyPanel = styled(Box)`
  flex: 1;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: ${({ theme }) => theme.spacing(1)};
  display: grid;

  &.accepted {
    border-color: ${({ theme }) => theme.palette.tokens.success.main};
  }
`;

const KeyHeader = styled(Box)`
  display: grid;
  grid-template-columns: 1fr auto;
  background: ${({ theme }) => theme.palette.tokens.background.hover};
  padding: ${({ theme }) => theme.spacing(1.5, 2)};

  &.accepted {
    background: ${({ theme }) => theme.palette.tokens.success._states.selected};
  }
`;

const AcceptButton = styled(Box)`
  display: flex;
  align-items: center;
`;

const TranslationList = styled(Box)`
  display: grid;
`;

const StyledLanguageField = styled('div')`
  border-color: ${({ theme }) => theme.palette.divider1};
  border-width: 1px 1px 1px 0px;
  border-style: solid;

  & + & {
    border-top: 0px;
  }
`;

type BranchMergeModel = components['schemas']['BranchMergeModel'];
type BranchMergeConflictModel =
  components['schemas']['BranchMergeConflictModel'];
type BranchMergeChangeType =
  components['schemas']['BranchMergeChangeModel']['type'];

type RouteParams = {
  mergeId: string;
};

const StatsBlock: React.FC<{ merge: BranchMergeModel }> = ({ merge }) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const baseStats = useMemo(
    () => [
      {
        label: t('branch_merges_additions'),
        value: merge.keyAdditionsCount,
        icon: Plus,
      },
      {
        label: t('branch_merges_modifications'),
        value: merge.keyModificationsCount,
        icon: RefreshCcw02,
      },
      {
        label: t('branch_merges_deletions'),
        value: merge.keyDeletionsCount,
        icon: Minus,
      },
    ],
    [merge, t]
  );

  const conflictsTotal =
    merge.keyUnresolvedConflictsCount + merge.keyResolvedConflictsCount;
  const resolvedConflicts = merge.keyResolvedConflictsCount;

  return (
    <StatsRow>
      {baseStats.map((stat) => {
        const Icon = stat.icon as FC;
        return (
          <StatCard key={stat.label}>
            <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
              <Box display="flex" alignItems="center">
                <Icon />
              </Box>
              <Typography variant="h3">
                <div>{stat.value}</div>
              </Typography>
            </Box>
            <Typography variant="body1" fontSize={18}>
              {stat.label}
            </Typography>
          </StatCard>
        );
      })}
      <StatCard>
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            {conflictsTotal > 0 &&
              (resolvedConflicts === conflictsTotal ? (
                <CheckCircle color={theme.palette.tokens.success.main} />
              ) : (
                <AlertTriangle color={theme.palette.tokens.warning.main} />
              ))}
          </Box>
          <Typography variant="h3">
            <div>{resolvedConflicts + '/' + conflictsTotal}</div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {t('branch_merges_conflicts_resolved_unresolved')}
        </Typography>
      </StatCard>
    </StatsRow>
  );
};

const KeyTranslations: React.FC<{
  keyData: components['schemas']['KeyWithTranslationsModel'];
}> = ({ keyData }) => {
  const languages = useTranslationsSelector((c) => c.languages);
  return (
    <TranslationList>
      {Object.entries(keyData.translations ?? {}).map(([lang]) => {
        const language = languages?.find((l) => l.tag === lang);
        if (!language) {
          return;
        }
        return (
          <StyledLanguageField
            key={lang}
            data-cy="translation-edit-translation-field"
          >
            <CellTranslation
              data={keyData!}
              language={language}
              active={false}
              lastFocusable={false}
              readonly={true}
            />
          </StyledLanguageField>
        );
      })}
    </TranslationList>
  );
};

const ConflictKeyPanel: React.FC<{
  keyData: components['schemas']['KeyWithTranslationsModel'];
  conflictHandler?: () => void;
  accepted?: boolean;
}> = ({ keyData, accepted, conflictHandler }) => {
  const theme = useTheme();
  return (
    <KeyPanel className={clsx({ accepted })}>
      <KeyHeader className={clsx({ accepted })}>
        <SimpleCellKey data={keyData!} />
        <AcceptButton>
          {accepted ? (
            <div>
              <Chip
                style={{
                  backgroundColor: theme.palette.tokens.success.main,
                }}
                size="small"
                color="primary"
                label={<T keyName="branch_merges_conflict_accepted" />}
              />
            </div>
          ) : conflictHandler !== undefined ? (
            <Button
              variant="outlined"
              size="small"
              onClick={() => conflictHandler()}
              data-cy="project-branch-merge-accept"
            >
              <T keyName="branch_merges_accept" />
            </Button>
          ) : null}
        </AcceptButton>
      </KeyHeader>
      <KeyTranslations keyData={keyData} />
    </KeyPanel>
  );
};

const StyledBadge = styled(Badge)<BadgeProps>(() => ({
  '& .MuiBadge-badge': {
    right: -6,
  },
}));

export const BranchMergeDetailView: React.FC = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { mergeId } = useParams<RouteParams>();
  const history = useHistory();
  const formatDate = useDateFormatter();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const [refreshed, setRefreshed] = useState(false);
  const [selectedTab, setSelectedTab] =
    useState<BranchMergeChangeType>('CONFLICT');

  const numericMergeId = Number(mergeId);

  const previewLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/preview',
    method: 'get',
    path: { projectId: project.id, mergeId: numericMergeId },
  });
  const merge = previewLoadable.data;

  const changesLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/changes',
    method: 'get',
    path: { projectId: project.id, mergeId: numericMergeId },
    query: { size: 100, type: selectedTab },
    options: {
      enabled: Boolean(merge),
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { projectId: project.id, mergeId: numericMergeId },
            query: {
              size: 100,
              page: lastPage.page.number! + 1,
              type: selectedTab,
            },
          } as const;
        }
        return undefined;
      },
    },
  });

  const resolveMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/resolve',
    method: 'put',
  });

  const resolveAllMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/resolve-all',
    method: 'put',
  });

  const applyMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/apply',
    method: 'post',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}',
    method: 'delete',
  });

  const refreshPreviewMutation = useApiMutation({
    url: '/v2/projects/{projectId}/branches/merge/{mergeId}/refresh',
    method: 'post',
  });

  const changes =
    changesLoadable.data?.pages.flatMap(
      (p) => p._embedded?.branchMergeChanges ?? []
    ) ?? [];

  const handleResolve = async (
    conflict: BranchMergeConflictModel,
    resolution: 'SOURCE' | 'TARGET'
  ) => {
    await resolveMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': {
          changeId: conflict.id,
          resolve: resolution,
        },
      },
    });
    await changesLoadable.refetch?.();
    await previewLoadable.refetch?.();
  };

  const handleResolveAll = async (resolution: 'SOURCE' | 'TARGET') => {
    await resolveAllMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
      content: {
        'application/json': {
          resolve: resolution,
        },
      },
    });
    await Promise.all([
      changesLoadable.refetch?.(),
      previewLoadable.refetch?.(),
    ]);
  };

  const closeWith = (action?: () => void) => (e) => {
    e?.stopPropagation();
    setAnchorEl(undefined);
    action?.();
  };

  const handleOpen = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.stopPropagation();
    setAnchorEl(e.target as HTMLElement);
  };

  const handleCancel = async () => {
    await deleteMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
    });
    history.push(
      LINKS.PROJECT_BRANCHES.build({
        [PARAMS.PROJECT_ID]: project.id,
      })
    );
  };

  const handleApply = async () => {
    await applyMutation.mutateAsync({
      path: { projectId: project.id, mergeId: numericMergeId },
    });
    messageService.success(<T keyName="branch_merges_apply_success" />);
    history.push(
      LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.BRANCH]: merge!.targetBranchName,
      })
    );
  };

  const readyToMerge =
    merge?.keyUnresolvedConflictsCount === 0 && merge.outdated === false;

  const backLink = LINKS.PROJECT_BRANCHES.build({
    [PARAMS.PROJECT_ID]: project.id,
  });

  const goBackToBranches = () => {
    history.push(backLink);
  };

  const totalConflicts = merge
    ? merge.keyResolvedConflictsCount + merge.keyUnresolvedConflictsCount
    : 0;

  const tabsConfig = merge
    ? [
        {
          key: 'ADD' as BranchMergeChangeType,
          label: t('branch_merges_additions'),
          count: merge.keyAdditionsCount,
        },
        {
          key: 'UPDATE' as BranchMergeChangeType,
          label: t('branch_merges_modifications'),
          count: merge.keyModificationsCount,
        },
        {
          key: 'DELETE' as BranchMergeChangeType,
          label: t('branch_merges_deletions'),
          count: merge.keyDeletionsCount,
        },
        {
          key: 'CONFLICT' as BranchMergeChangeType,
          label: t('branch_merges_conflicts_title'),
          count: totalConflicts,
        },
      ]
    : [];

  useEffect(() => {
    if (!merge) return;
    if (selectedTab && tabsConfig.find((t) => t.key === selectedTab)) return;

    const conflictTab = tabsConfig.find((t) => t.key === 'CONFLICT');
    const firstWithData =
      conflictTab && conflictTab.count > 0
        ? conflictTab
        : tabsConfig.find((t) => t.count > 0);

    setSelectedTab((firstWithData?.key ?? tabsConfig[0]?.key) || 'CONFLICT');
  }, [merge, totalConflicts, tabsConfig.map((t) => t.count).join('|')]);

  useEffect(() => {
    if (previewLoadable.isFetched && merge && merge.outdated && !refreshed) {
      setRefreshed(true);
      refreshPreviewMutation.mutate(
        {
          path: { projectId: project.id, mergeId: numericMergeId },
        },
        {
          onSuccess: () => {
            previewLoadable.refetch?.();
          },
        }
      );
    }
  });

  return (
    <BaseProjectView
      maxWidth={1200}
      windowTitle={t('branch_merges_title')}
      navigation={[[t('branches_title'), backLink]]}
    >
      <Box display="flex" justifyContent="space-between" mb={2}>
        <Typography variant="h4">
          {merge?.mergedAt ? (
            <T keyName="branches_merge_merged_detail" />
          ) : (
            <T keyName="branches_merge_title" />
          )}
        </Typography>
        <Box display="flex" alignItems="center">
          {merge && (
            <>
              <>
                {merge.mergedAt && (
                  <SuccessChip
                    icon={<CheckCircle width={18} height={18} />}
                    label={<T keyName={'branch_merges_merged_button'} />}
                  />
                )}
              </>
              <>
                <IconButton
                  onClick={handleOpen}
                  data-cy="project-dashboard-language-menu"
                >
                  <DotsVertical />
                </IconButton>
                <Menu
                  anchorEl={anchorEl}
                  open={Boolean(anchorEl)}
                  onClose={closeWith()}
                >
                  <MenuItem onClick={closeWith(handleCancel)}>
                    <Typography color={(theme) => theme.palette.error.main}>
                      <T keyName="branch_merge_delete_button" />
                    </Typography>
                  </MenuItem>
                </Menu>
              </>
            </>
          )}
        </Box>
      </Box>

      {previewLoadable.isLoading ? (
        <CircularProgress />
      ) : merge ? (
        <Box data-cy="project-branch-merge-detail">
          <BranchesRow mb={3}>
            {merge.mergedAt == null ? (
              <T
                keyName="branch_merging_into_title"
                params={{
                  branch: <BranchNameChipNode />,
                  sourceName: merge.sourceBranchName,
                  targetName: merge.targetBranchName,
                }}
              />
            ) : (
              <T
                keyName="branch_merging_from_title_merged"
                params={{
                  branch: <BranchNameChipNode />,
                  sourceName: merge.sourceBranchName,
                  targetName: merge.targetBranchName,
                  date: formatDate(merge.mergedAt, {
                    dateStyle: 'short',
                    timeStyle: 'short',
                  }),
                }}
              />
            )}
          </BranchesRow>

          {merge.mergedAt == null && merge.outdated && (
            <Box mb={2}>
              <Alert severity="warning">
                <T keyName="branch_merges_status_outdated" />
              </Alert>
            </Box>
          )}

          <StatsBlock merge={merge} />
        </Box>
      ) : null}

      {merge && merge.keyUnresolvedConflictsCount > 0 && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          <T
            keyName="branch_merges_unresolved_conflicts_alert"
            params={{ value: merge.keyUnresolvedConflictsCount }}
          />
        </Alert>
      )}

      {merge && (
        <TabsWrapper>
          <Tabs
            value={selectedTab ?? 'CONFLICT'}
            onChange={(_, value) =>
              setSelectedTab(value as BranchMergeChangeType)
            }
            variant="scrollable"
            scrollButtons="auto"
          >
            {tabsConfig.map((tab) => (
              <Tab
                key={tab.key}
                value={tab.key}
                disabled={tab.count === 0}
                label={
                  <StyledBadge
                    color="primary"
                    badgeContent={tab.count}
                    max={999}
                    anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                  >
                    {tab.label}
                  </StyledBadge>
                }
              />
            ))}
          </Tabs>
        </TabsWrapper>
      )}

      <Box display="grid" mt={4} rowGap={2}>
        {merge && (
          <Box>
            {changesLoadable.isLoading ? (
              <CircularProgress />
            ) : changes.length ? (
              <ConflictsWrapper>
                <ConflictsHeader>
                  {selectedTab !== 'DELETE' && selectedTab !== 'ADD' && (
                    <ConflictsHeaderColumn>
                      <Box display="flex" gap={1}>
                        <Branch height={18} width={18} />
                        <Typography variant="body2" fontWeight="medium">
                          {merge.sourceBranchName}
                        </Typography>
                      </Box>
                      {merge.mergedAt == null && selectedTab === 'CONFLICT' && (
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => handleResolveAll('SOURCE')}
                          disabled={resolveAllMutation.isLoading}
                          endIcon={<CheckDone />}
                        >
                          <T keyName="branch_merges_accept_all" />
                        </Button>
                      )}
                    </ConflictsHeaderColumn>
                  )}
                  <ConflictsHeaderColumn>
                    <Box display="flex" gap={1}>
                      <Branch height={18} width={18} />
                      <Typography variant="body2" fontWeight="medium">
                        {merge.targetBranchName}
                      </Typography>
                    </Box>
                    {merge.mergedAt == null && selectedTab === 'CONFLICT' && (
                      <Button
                        size="small"
                        variant="outlined"
                        onClick={() => handleResolveAll('TARGET')}
                        disabled={resolveAllMutation.isLoading}
                        endIcon={<CheckDone />}
                      >
                        <T keyName="branch_merges_accept_all" />
                      </Button>
                    )}
                  </ConflictsHeaderColumn>
                </ConflictsHeader>
                {changes.map((change) => {
                  const sourceKey = change.sourceKey;
                  const targetKey = change.targetKey;
                  const isConflict = change.type === 'CONFLICT';
                  const isModification = change.type === 'UPDATE';
                  const showTwoColumns = isConflict || isModification;
                  const acceptedSource = change.resolution === 'SOURCE';
                  const acceptedTarget = change.resolution === 'TARGET';

                  return (
                    <ChangeCard
                      key={change.id}
                      data-cy="project-branch-merge-change"
                    >
                      {showTwoColumns ? (
                        <ConflictColumns>
                          {sourceKey && (
                            <ConflictKeyPanel
                              keyData={sourceKey}
                              accepted={isConflict ? acceptedSource : undefined}
                              conflictHandler={
                                isConflict && merge.mergedAt == null
                                  ? () =>
                                      handleResolve(
                                        change as BranchMergeConflictModel,
                                        'SOURCE'
                                      )
                                  : undefined
                              }
                            />
                          )}
                          {targetKey && (
                            <ConflictKeyPanel
                              keyData={targetKey}
                              accepted={isConflict ? acceptedTarget : undefined}
                              conflictHandler={
                                isConflict && merge.mergedAt == null
                                  ? () =>
                                      handleResolve(
                                        change as BranchMergeConflictModel,
                                        'TARGET'
                                      )
                                  : undefined
                              }
                            />
                          )}
                        </ConflictColumns>
                      ) : (
                        <KeyPanel>
                          <KeyHeader>
                            <SimpleCellKey data={(sourceKey || targetKey)!} />
                          </KeyHeader>
                          <KeyTranslations
                            keyData={(sourceKey || targetKey)!}
                          />
                        </KeyPanel>
                      )}
                    </ChangeCard>
                  );
                })}
              </ConflictsWrapper>
            ) : (
              <Typography variant="body2" color="textSecondary">
                <T
                  keyName={
                    selectedTab === 'CONFLICT'
                      ? 'branch_merges_no_conflicts'
                      : selectedTab === 'ADD'
                      ? 'branch_merges_no_additions'
                      : selectedTab === 'UPDATE'
                      ? 'branch_merges_no_modifications'
                      : 'branch_merges_no_deletions'
                  }
                />
              </Typography>
            )}
          </Box>
        )}
        {merge && (
          <Box display="flex" justifyContent="end" columnGap={2}>
            <Button
              variant="outlined"
              color="primary"
              onClick={goBackToBranches}
            >
              <T keyName="branch_merge_cancel_button" />
            </Button>
            {merge.mergedAt == null && (
              <Button
                variant="contained"
                color="primary"
                onClick={handleApply}
                disabled={!readyToMerge || applyMutation.isLoading}
              >
                <T keyName="branch_merges_apply_button" />
              </Button>
            )}
          </Box>
        )}
      </Box>
    </BaseProjectView>
  );
};
