import { useState } from 'react';
import {
  Box,
  IconButton,
  Link as MuiLink,
  styled,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { Calendar, Edit02, Link01 } from '@untitled-ui/icons-react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';

import { LINKS, PARAMS } from 'tg.constants/links';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { ClipboardCopy } from 'tg.component/common/ClipboardCopy';
import { UserProfileAvatar } from 'tg.views/userSettings/userProfile/UserProfileAvatar';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

import { ProfileSectionTitle } from './ProfileSectionTitle';
import { StyledShowMore } from './StyledShowMore';

type CommunityProfile = components['schemas']['CommunityProfile'];

const COMPACT_PROJECTS = 2;

const StyledNameRow = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;

  & .editProfile {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }

  &:hover .editProfile,
  & .editProfile:focus-visible {
    opacity: 1;
  }
`;

type Props = {
  profile: CommunityProfile;
  isMe: boolean;
  isPublic: boolean;
};

export const ProfileIdentity = ({ profile, isMe, isPublic }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const theme = useTheme();
  const compact = useMediaQuery(theme.breakpoints.down('md'));
  const phone = useMediaQuery(theme.breakpoints.down('sm'));
  const avatarSize = phone ? 64 : compact ? 80 : 200;
  const [allProjects, setAllProjects] = useState(false);
  const [photoHint, setPhotoHint] = useState(false);
  const [photoMenuOpen, setPhotoMenuOpen] = useState(false);
  const shownProjects =
    compact && !allProjects
      ? profile.projects.slice(0, COMPACT_PROJECTS)
      : profile.projects;
  const hiddenProjects = profile.projects.length - shownProjects.length;

  const firstContribution = profile.projects
    .map((p) => p.firstContributionAt)
    .filter((d): d is string => d !== undefined)
    .map((d) => new Date(d).getTime())
    .sort((a, b) => a - b)[0];

  const profileUrl = () =>
    window.location.origin +
    LINKS.USER_COMMUNITY_PROFILE.build({ [PARAMS.USER_ID]: profile.id });

  return (
    <Box
      display="grid"
      gridTemplateColumns={
        compact && !phone ? 'minmax(0, 3fr) minmax(0, 2fr)' : 'minmax(0, 1fr)'
      }
      columnGap={3}
      rowGap={3}
      alignItems="start"
      data-cy="community-profile-identity"
    >
      <Box
        display="grid"
        gridTemplateColumns={compact ? 'auto minmax(0, 1fr)' : 'minmax(0, 1fr)'}
        columnGap={2}
        alignItems="center"
        justifyItems="start"
      >
        {isMe ? (
          <Tooltip
            title={t('community_profile_change_photo', 'Change photo')}
            open={photoHint && !photoMenuOpen}
            onOpen={() => setPhotoHint(true)}
            onClose={() => setPhotoHint(false)}
            disableFocusListener
            placement="bottom"
            slotProps={{
              popper: {
                modifiers: [
                  {
                    name: 'offset',
                    options: { offset: [0, -avatarSize / 2 + 12] },
                  },
                ],
              },
            }}
          >
            <Box
              data-cy="community-profile-avatar-edit"
              onClick={() => setPhotoMenuOpen(true)}
              onMouseLeave={() => setPhotoMenuOpen(false)}
            >
              <UserProfileAvatar size={avatarSize} />
            </Box>
          </Tooltip>
        ) : (
          <AvatarImg
            owner={{
              id: profile.id,
              name: profile.name,
              avatar: profile.avatar,
              type: 'USER',
            }}
            size={avatarSize}
          />
        )}
        <Box
          display="grid"
          gap={compact ? 0.75 : 1.5}
          justifyItems="start"
          mt={compact ? 0 : 2}
          minWidth={0}
        >
          <StyledNameRow>
            <Typography variant={compact ? 'h5' : 'h4'} component="h1">
              {profile.name}
            </Typography>
            {isMe && isPublic && (
              <ClipboardCopy
                value={profileUrl}
                tooltip={t('community_profile_copy_link', 'Copy profile link')}
                icon={<Link01 width={20} height={20} />}
              />
            )}
            {isMe && (
              <Tooltip
                title={t('community_profile_edit_tooltip', 'Edit your profile')}
              >
                <IconButton
                  className="editProfile"
                  component={Link}
                  to={LINKS.USER_PROFILE.build()}
                  aria-label={t(
                    'community_profile_edit_tooltip',
                    'Edit your profile'
                  )}
                  data-cy="community-profile-edit-name"
                >
                  <Edit02 width={20} height={20} />
                </IconButton>
              </Tooltip>
            )}
          </StyledNameRow>
          {profile.languages.length > 0 && (
            <Box display="flex" gap={0.75} alignItems="center">
              {profile.languages.map((l) => (
                <Tooltip key={l.tag} title={l.name}>
                  <span>
                    <FlagImage
                      flagEmoji={languageInfo[l.tag]?.flags?.[0] ?? ''}
                      height={20}
                    />
                  </span>
                </Tooltip>
              ))}
            </Box>
          )}
          {firstContribution !== undefined && (
            <Typography
              variant="body2"
              fontSize={13}
              color="text.secondary"
              display="flex"
              alignItems="center"
              gap={0.5}
            >
              <Calendar width={16} height={16} />
              {t('community_profile_since', 'Since {date}', {
                date: formatDate(firstContribution, {
                  month: 'long',
                  year: 'numeric',
                }),
              })}
            </Typography>
          )}
        </Box>
      </Box>

      {profile.projects.length > 0 && (
        <Box display="grid" gap={0.5} pt={compact ? 0 : 1}>
          <ProfileSectionTitle>
            {t('community_profile_projects', 'Projects')}
          </ProfileSectionTitle>
          {shownProjects.map((p) => (
            <Box
              key={p.id}
              display="flex"
              alignItems="center"
              gap={1}
              py={compact ? 0.5 : 1}
              minWidth={0}
            >
              <Box flexShrink={0} display="flex">
                <AvatarImg
                  owner={{
                    id: p.id,
                    name: p.name,
                    avatar: p.avatar,
                    type: 'PROJECT',
                  }}
                  size={24}
                />
              </Box>
              <MuiLink
                component={Link}
                to={LINKS.PROJECT_DASHBOARD.build({
                  [PARAMS.PROJECT_ID]: p.id,
                })}
                variant="body2"
                color="text.primary"
                underline="hover"
                noWrap
                minWidth={0}
                sx={{ '&:hover': { color: 'primary.main' } }}
                data-cy="community-profile-project"
              >
                {p.name}
              </MuiLink>
            </Box>
          ))}
          {hiddenProjects > 0 && (
            <StyledShowMore
              type="button"
              onClick={() => setAllProjects(true)}
              style={{ justifySelf: 'start', margin: '4px 0 0' }}
              data-cy="community-profile-projects-show-all"
            >
              {t('community_profile_projects_show_all', 'Show all ({count})', {
                count: profile.projects.length,
              })}
            </StyledShowMore>
          )}
        </Box>
      )}
    </Box>
  );
};
