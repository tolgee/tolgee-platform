import { useFormikContext } from 'formik';
import { Box, Link as MuiLink } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { ChevronRight } from '@untitled-ui/icons-react';

import { Switch } from 'tg.component/common/Switch';
import { LabelHint } from 'tg.component/common/LabelHint';
import { LinkExternal } from 'tg.component/LinkExternal';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useHasPublicContribution, useUser } from 'tg.globalContext/helpers';

export const ProfileVisibilityField = () => {
  const { t } = useTranslate();
  const user = useUser();
  const formik = useFormikContext();
  const visibility = formik.getFieldMeta('profileVisibility');
  const hasPublicContribution = useHasPublicContribution();

  const savedAsPublic = visibility.initialValue === 'PUBLIC';
  const visibilityWhenOn = savedAsPublic ? 'PUBLIC' : 'DEFAULT';
  const savedAsWaiting =
    !hasPublicContribution && visibility.initialValue === 'DEFAULT';
  const isOn = !savedAsWaiting && visibility.value !== 'PRIVATE';
  const savesAsWaiting =
    !hasPublicContribution && visibility.value === 'DEFAULT';

  const profileLink = (
    <LinkExternal
      href={LINKS.USER_COMMUNITY_PROFILE.build({
        [PARAMS.USER_ID]: user!.id,
      })}
      underline="hover"
      data-cy="user-profile-visibility-view-link"
    >
      {t('community_profile_visibility_open_link', 'Open profile')}
    </LinkExternal>
  );

  const findProjectLink = (
    <MuiLink
      component={Link}
      to={LINKS.COMMUNITY_PROJECTS.build()}
      underline="hover"
      display="inline-flex"
      alignItems="center"
      data-cy="user-profile-visibility-find-link"
    >
      {t('community_profile_visibility_find_link', 'Find a project')}
      <ChevronRight width={14} height={14} />
    </MuiLink>
  );

  const sentence = savesAsWaiting
    ? t(
        'community_profile_visibility_status_not_yet',
        'Your profile appears after your first contribution.'
      )
    : isOn
    ? t(
        'community_profile_visibility_status_public',
        'Anyone can see your profile.'
      )
    : t(
        'community_profile_visibility_status_private',
        'Only you can see your profile.'
      );
  const link = savesAsWaiting ? findProjectLink : profileLink;

  return (
    <Box mb={2}>
      <Switch
        checked={isOn}
        disabled={savedAsWaiting}
        onChange={(_, checked) =>
          formik.setFieldValue(
            'profileVisibility',
            checked ? visibilityWhenOn : 'PRIVATE'
          )
        }
        data-cy="user-profile-visibility-switch"
        label={
          <LabelHint
            title={t(
              'community_profile_visibility_hint',
              'Your profile shows up once you contribute to any public project, also as a team member.'
            )}
          >
            {t(
              'community_profile_visibility_public_label',
              'Public community profile'
            )}
          </LabelHint>
        }
        helperText={
          <Box
            display="flex"
            flexWrap="wrap"
            columnGap={0.5}
            whiteSpace="normal"
            fontSize={14}
            data-cy="user-profile-visibility-status"
          >
            <span>{sentence}</span>
            {link}
          </Box>
        }
      />
    </Box>
  );
};
