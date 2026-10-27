import { useLayoutEffect } from 'react';
import { Box, Link as MuiLink, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { CircledLanguageIconList } from 'tg.component/languages/CircledLanguageIconList';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { useUserName } from 'tg.component/common/UserName';

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
  font-weight: ${({ theme }) => theme.typography.fontWeightMedium};
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
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledRateTrack = styled('div')`
  height: 6px;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  overflow: hidden;
  background: ${({ theme }) =>
    theme.palette.tokens._components.progressbar.background};
`;

const StyledRateFill = styled('div')`
  height: 100%;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  background: ${({ theme }) => theme.palette.tokens.success.main};
`;

type Props = {
  author: SimpleUserAccountModel;
  onContentChange: () => void;
};

export const SuggestionAuthorCard = ({ author, onContentChange }: Props) => {
  const project = useProject();
  const getUserName = useUserName();
  const projectLanguages = useTranslationsSelector((c) => c.languages);
  const formatDate = useDateFormatter();
  const { data, isLoading, error } = useApiQuery({
    url: '/v2/projects/{projectId}/community/contributors/{userId}',
    method: 'get',
    path: { projectId: project.id, userId: author.id },
    query: {},
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { staleTime: 60_000 },
  });

  useLayoutEffect(() => {
    if (!isLoading) onContentChange();
  }, [isLoading, data, error]);

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
          <StyledName>{getUserName(author)}</StyledName>
          {isLoading && <LoadingSkeleton sx={{ height: 14, width: 120 }} />}
          {error?.code === 'user_not_found' && (
            <StyledSecondary data-cy="suggestion-author-card-private">
              <T
                keyName="community_contributor_card_private"
                defaultValue="Private profile"
              />
            </StyledSecondary>
          )}
          {data && (
            <StyledLanguages>
              <CircledLanguageIconList languages={languages} />
              {data.firstContributionAt && (
                <StyledSecondary data-cy="suggestion-author-card-since">
                  {languages.length > 0 && '· '}
                  <T
                    keyName="community_contributor_card_since"
                    defaultValue="since {date}"
                    params={{
                      date: formatDate(new Date(data.firstContributionAt), {
                        month: 'long',
                        year: 'numeric',
                      }),
                    }}
                  />
                </StyledSecondary>
              )}
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
              defaultValue="<b>{accepted}</b> of {suggested, plural, one {# suggestion} other {# suggestions}} accepted · {declined, plural, one {# declined} other {# declined}}"
              params={{
                accepted: data.accepted,
                suggested: data.suggested,
                declined: data.declined,
                b: <b />,
              }}
            />
          </div>
          <StyledSecondary data-cy="suggestion-author-card-contributions">
            <T
              keyName="community_contributor_card_contributions"
              defaultValue="{contributions, plural, one {# contribution} other {# contributions}}"
              params={{ contributions: data.contributions }}
            />
          </StyledSecondary>
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
