import { ReactNode, useEffect, useState } from 'react';
import { Box, Link as MuiLink, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import {
  ArrowUpRight,
  ChevronDown,
  ChevronUp,
  CursorClick02,
} from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { LabelHint } from 'tg.component/common/LabelHint';
import { UserName } from 'tg.component/common/UserName';
import { useDateFormatter } from 'tg.hooks/useLocale';

import { ExpandableMarkdown } from 'tg.views/projects/community/ExpandableMarkdown';
import { isValidHttpUrl } from 'tg.fixtures/isValidUrl';

type CommunityContributorModel =
  components['schemas']['CommunityContributorModel'];

const IN_APP_DOCS_URL = 'https://docs.tolgee.io/translate-with-tolgee';

const StyledSectionHeader = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  min-height: 39px;
`;

const StyledSectionTitle = styled('h2')`
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  text-transform: uppercase;
  white-space: nowrap;
`;

const StyledToggle = styled('button')`
  display: grid;
  border: none;
  background: none;
  padding: 0;
  color: inherit;
  cursor: pointer;
`;

const StyledBanner = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-top: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(2, 2.5)};
  border-radius: 10px;
  background: ${({ theme }) => theme.palette.tipsBanner.background};
  color: ${({ theme }) => theme.palette.text.primary};
`;

type SectionProps = {
  title: ReactNode;
  aside?: ReactNode;
  collapsible: boolean;
  defaultOpen: boolean;
  children: ReactNode;
  'data-cy': string;
};

const Section = ({
  title,
  aside,
  collapsible,
  defaultOpen,
  children,
  ...props
}: SectionProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(defaultOpen);

  useEffect(() => {
    setOpen(defaultOpen);
  }, [defaultOpen]);
  const visible = !collapsible || open;

  return (
    <Box component="section" data-cy={props['data-cy']}>
      <StyledSectionHeader>
        <StyledSectionTitle>{title}</StyledSectionTitle>
        {collapsible && (
          <StyledToggle
            type="button"
            aria-expanded={open}
            aria-label={
              open
                ? t('community_section_collapse', 'Collapse')
                : t('community_section_expand', 'Expand')
            }
            onClick={() => setOpen(!open)}
            data-cy="community-section-toggle"
          >
            {open ? (
              <ChevronUp width={20} height={20} />
            ) : (
              <ChevronDown width={20} height={20} />
            )}
          </StyledToggle>
        )}
        {aside && <Box ml="auto">{aside}</Box>}
      </StyledSectionHeader>
      {visible && (
        <Box display="grid" gap={1} pt={0.5} pb={2}>
          {children}
        </Box>
      )}
    </Box>
  );
};

type Props = {
  compact: boolean;
  guidelines?: string;
  showInAppBanner: boolean;
  appUrl?: string;
  topContributors: CommunityContributorModel[];
  topPeriod: 'month' | 'year';
  details: {
    projectId: number;
    lastActivity?: string;
    createdAt?: string;
    wordsToTranslate?: number;
    contributors?: number;
  };
};

export const CommunitySidebar = ({
  compact,
  guidelines,
  showInAppBanner,
  appUrl,
  topContributors,
  topPeriod,
  details,
}: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const appHref = appUrl && isValidHttpUrl(appUrl) ? appUrl : undefined;

  const detailRows: [ReactNode, ReactNode][] = [
    [t('community_details_project_id', 'Project ID'), details.projectId],
    [
      t('community_details_last_activity', 'Last activity'),
      details.lastActivity
        ? formatDate(new Date(details.lastActivity), { dateStyle: 'long' })
        : '—',
    ],
    [
      t('community_details_created', 'Created'),
      details.createdAt
        ? formatDate(new Date(details.createdAt), {
            month: 'long',
            year: 'numeric',
          })
        : '—',
    ],
    [
      t('community_details_words_to_translate', 'Words to translate'),
      details.wordsToTranslate?.toLocaleString() ?? '—',
    ],
    [
      t('community_details_contributors', 'Contributors'),
      details.contributors ?? '—',
    ],
  ];

  return (
    <Box display="grid" gap={compact ? 0 : 1} alignContent="start">
      <Section
        title={t('community_how_to_contribute_title', 'How to contribute')}
        collapsible={true}
        defaultOpen={!compact}
        data-cy="community-how-to-contribute"
      >
        {guidelines ? (
          <ExpandableMarkdown
            text={guidelines}
            data-cy="community-guidelines"
          />
        ) : (
          <Box
            lineHeight={1.5}
            whiteSpace="pre-line"
            data-cy="community-guidelines"
          >
            {t(
              'community_guidelines_default',
              'Click on a language, sign in and start translating. Suggestions are used once the project team approves them. Thanks for helping!'
            )}
          </Box>
        )}
        {showInAppBanner && (
          <StyledBanner data-cy="community-in-app-banner">
            <Box
              display="flex"
              alignItems="center"
              justifyContent="space-between"
              gap={2}
            >
              <Box display="flex" alignItems="center" gap={1}>
                <CursorClick02 width={20} height={20} />
                <Typography variant="subtitle2">
                  {t('community_in_app_title', 'Translate right in the app')}
                </Typography>
              </Box>
              {appHref && (
                <MuiLink
                  href={appHref}
                  underline="hover"
                  target="_blank"
                  rel="noreferrer noopener"
                  variant="body2"
                  data-cy="community-in-app-open"
                >
                  {t('community_in_app_open', 'Open app')}
                </MuiLink>
              )}
            </Box>
            <Typography variant="body2">
              {t(
                'community_in_app_text',
                'Click any text in the app and translate it on the spot.'
              )}
            </Typography>
            <MuiLink
              href={IN_APP_DOCS_URL}
              underline="hover"
              target="_blank"
              rel="noreferrer noopener"
              variant="body2"
              sx={{ justifySelf: 'start' }}
              data-cy="community-in-app-guide"
            >
              {t('community_in_app_link', 'How it works')}
            </MuiLink>
          </StyledBanner>
        )}
      </Section>

      <Section
        title={
          <LabelHint
            title={t(
              'community_top_contributors_info',
              'Contributions are translations, suggestions, reviews and comments in this project.'
            )}
          >
            {t('community_top_contributors_title', 'Top contributors')}
          </LabelHint>
        }
        aside={
          <Typography
            variant="body2"
            color="text.secondary"
            data-cy="community-top-contributors-period"
            data-cy-period={topPeriod}
          >
            {topPeriod === 'month'
              ? t('community_period_month', 'Last 30 days')
              : t('community_period_year', 'Last 12 months')}
          </Typography>
        }
        collapsible={true}
        defaultOpen={!compact}
        data-cy="community-top-contributors"
      >
        {topContributors.length ? (
          <>
            {topContributors.map((c) => (
              <Box
                key={c.id}
                display="flex"
                alignItems="center"
                gap={1}
                data-cy="community-top-contributor"
              >
                <AvatarImg
                  owner={{
                    name: c.name,
                    avatar: c.avatar,
                    type: 'USER',
                    id: c.id,
                  }}
                  size={24}
                />
                <UserName name={c.name} />
              </Box>
            ))}
            <MuiLink
              component={Link}
              to={{
                pathname: LINKS.PROJECT_COMMUNITY_CONTRIBUTORS.build({
                  [PARAMS.PROJECT_ID]: details.projectId,
                }),
                search: `?period=${topPeriod}`,
              }}
              underline="none"
              sx={{ justifySelf: 'start' }}
              data-cy="community-all-contributors"
            >
              {t('community_all_contributors', 'All contributors')}
              <ArrowUpRight
                style={{
                  display: 'inline-flex',
                  width: '1.43em',
                  height: '1.43em',
                  verticalAlign: 'bottom',
                }}
              />
            </MuiLink>
          </>
        ) : (
          <Typography variant="body2" color="text.secondary" fontStyle="italic">
            {t('community_top_contributors_empty', 'No contributions yet.')}
          </Typography>
        )}
      </Section>

      <Section
        title={t('community_details_title', 'Project details')}
        collapsible={true}
        defaultOpen={false}
        data-cy="community-details"
      >
        <Box
          component="dl"
          m={0}
          display="grid"
          gridTemplateColumns="1fr auto"
          columnGap={2}
          rowGap={0.5}
          color="text.secondary"
        >
          {detailRows.map(([label, value], i) => (
            <Box key={i} display="contents">
              <Box component="dt">{label}</Box>
              <Box component="dd" m={0} textAlign="right">
                {value}
              </Box>
            </Box>
          ))}
        </Box>
      </Section>
    </Box>
  );
};
