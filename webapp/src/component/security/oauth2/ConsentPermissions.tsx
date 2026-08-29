import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Chip, Link, styled } from '@mui/material';

import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { useScopeTranslations } from 'tg.component/PermissionsSettings/useScopeTranslations';
import { Hierarchy } from 'tg.component/PermissionsSettings/Hierarchy';
import {
  limitStructureToOptions,
  usePermissionsStructure,
} from 'tg.component/PermissionsSettings/usePermissionsStructure';
import {
  PermissionAdvancedState,
  PermissionModelScope,
} from 'tg.component/PermissionsSettings/types';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { groupConsentScopes } from 'tg.component/security/oauth2/consentScopeGroups';
import { clampApprovedScopes } from 'tg.component/security/oauth2/consentScopeSelection';

const StyledPermissionsHeader = styled(Box)`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: ${({ theme }) => theme.spacing(2)};
`;

const StyledSectionTitle = styled('div')`
  font-weight: 600;
`;

const StyledGroup = styled('div')`
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-top: ${({ theme }) => theme.spacing(1)};
`;

const StyledGroupLabel = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-right: ${({ theme }) => theme.spacing(0.5)};
`;

type Props = {
  requestedScopes: string[];
  requiredScopes: string[];
  selectedScopes: string[];
  onSelectedScopesChange: (scopes: string[]) => void;
};

export const ConsentPermissions: React.FC<Props> = ({
  requestedScopes,
  requiredScopes,
  selectedScopes,
  onSelectedScopesChange,
}) => {
  const { t } = useTranslate();
  const { getScopeTranslation } = useScopeTranslations();
  const structure = usePermissionsStructure();
  const [editing, setEditing] = React.useState(false);

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  const grantedGroups = groupConsentScopes(
    requestedScopes.filter((s) => selectedScopes.includes(s)),
    structure
  );
  const limitedStructure = limitStructureToOptions(
    [structure],
    requestedScopes as PermissionModelScope[]
  );

  const handleChange = (data: PermissionAdvancedState) =>
    onSelectedScopesChange(
      clampApprovedScopes(
        data.scopes as string[],
        requestedScopes,
        requiredScopes
      )
    );

  return (
    <>
      <StyledPermissionsHeader>
        <StyledSectionTitle>
          <T
            keyName="oauth2_consent_permissions_title"
            defaultValue="Permissions"
          />
        </StyledSectionTitle>
        <Link
          component="button"
          type="button"
          data-cy="oauth2-consent-modify"
          onClick={() => setEditing((prev) => !prev)}
        >
          {editing
            ? t('oauth2_consent_modify_done', 'Done')
            : t('oauth2_consent_modify', 'Modify')}
        </Link>
      </StyledPermissionsHeader>

      {!editing &&
        grantedGroups.map((group, i) => (
          <StyledGroup key={group.label ?? `_${i}`}>
            {group.label && (
              <StyledGroupLabel>
                <T
                  keyName="oauth2_consent_scope_group_label"
                  defaultValue="{group}:"
                  params={{ group: group.label }}
                />
              </StyledGroupLabel>
            )}
            {group.scopes.map((s) => (
              <Chip
                key={s}
                size="small"
                data-cy="oauth2-consent-scope"
                data-cy-scope={s}
                label={getScopeTranslation(s as PermissionModelScope)}
              />
            ))}
          </StyledGroup>
        ))}

      {editing &&
        (dependenciesLoadable.isLoading || !dependenciesLoadable.data ? (
          <Box sx={{ mt: 1 }}>
            <SpinnerProgress />
          </Box>
        ) : (
          <Box sx={{ mt: 1 }} data-cy="oauth2-consent-scopes">
            {limitedStructure.map((structureItem, i) => (
              <Hierarchy
                key={i}
                structure={structureItem}
                dependencies={dependenciesLoadable.data}
                state={{ scopes: selectedScopes as PermissionModelScope[] }}
                onChange={handleChange}
                lockedScopes={requiredScopes as PermissionModelScope[]}
              />
            ))}
          </Box>
        ))}
    </>
  );
};
