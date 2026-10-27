import { useLayoutEffect } from 'react';
import { Box, Link as MuiLink, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { CircledLanguageIconList } from 'tg.component/languages/CircledLanguageIconList';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';

type SimpleUserAccountModel = components['schemas']['SimpleUserAccountModel'];

const StyledCard = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1.5)};
  padding: ${({ theme }) => theme.spacing(1)};
  min-width: 240px;
  max-width: 300px;
  font-size: ${({ theme }) => theme.typography.body2.fontSize};
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledHeader = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1.5)};
  align-items: center;
`;

const StyledIdentity = styled(Box)`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0;
`;

const StyledName = styled('div')`
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledSecondary = styled('div')`
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledLanguages = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledRateTrack = styled('div')`
  height: 6px;
  border-radius: 4px;
  overflow: hidden;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.background};
`;

const StyledRateFill = styled('div')`
  height: 100%;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.tokens.success.main};
`;

type Props = {
  author: SimpleUserAccountModel;
  displayName: string;
  onContentChange: () => void;
};

export const SuggestionAuthorCard = ({
  author,
  displayName,
  onContentChange,
}: Props) => {
  const project = useProject();
  const projectLanguages = useTranslationsSelector((c) => c.languages);
  const { data, isLoading, isError } = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors/{userId}',
    method: 'get',
    path: { projectId: project.id, userId: author.id },
    query: {},
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { staleTime: 60_000 },
  });

  useLayoutEffect(() => {
    if (!isLoading) onContentChange();
  }, [isLoading, data, isError]);

  const languages =
    data?.languages
      .map((l) => projectLanguages?.find((p) => p.id === l.languageId))
      .filter((l) => l !== undefined) ?? [];
  const acceptedPercent = data
    ? (data.accepted / (data.suggested || 1)) * 100
    : 0;

  return (
    <StyledCard data-cy="suggestion-author-card">
      <StyledHeader>
        <AvatarImg owner={{ ...author, type: 'USER' }} size={32} />
        <StyledIdentity>
          <StyledName>{displayName}</StyledName>
          {isLoading && <LoadingSkeleton sx={{ height: 14, width: 120 }} />}
          {isError && (
            <StyledSecondary data-cy="suggestion-author-card-unavailable">
              <T
                keyName="community_contributor_card_unavailable"
                defaultValue="Contribution stats not available"
              />
            </StyledSecondary>
          )}
          {languages.length > 0 && (
            <StyledLanguages>
              <CircledLanguageIconList languages={languages} />
            </StyledLanguages>
          )}
        </StyledIdentity>
      </StyledHeader>
      {data && (
        <>
          <StyledRateTrack>
            <StyledRateFill style={{ width: `${acceptedPercent}%` }} />
          </StyledRateTrack>
          <div data-cy="suggestion-author-card-stats">
            <T
              keyName="community_contributor_card_stats"
              defaultValue="{accepted, plural, one {<b>#</b> of {suggested, plural, one {# suggestion} other {# suggestions}} accepted · {declined, plural, one {# declined} other {# declined}}} other {<b>#</b> of {suggested, plural, one {# suggestion} other {# suggestions}} accepted · {declined, plural, one {# declined} other {# declined}}}}"
              params={{
                accepted: data.accepted,
                suggested: data.suggested,
                declined: data.declined,
                b: <b />,
              }}
            />
          </div>
          <MuiLink
            component={Link}
            to={LINKS.USER_COMMUNITY_PROFILE.build({
              [PARAMS.USER_ID]: author.id,
            })}
            underline="hover"
            data-cy="suggestion-author-card-profile-link"
          >
            <T
              keyName="community_contributor_card_profile_link"
              defaultValue="View profile"
            />
          </MuiLink>
        </>
      )}
    </StyledCard>
  );
};
