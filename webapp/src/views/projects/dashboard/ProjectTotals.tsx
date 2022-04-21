import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { Box, styled, Typography } from '@mui/material';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

const StyledTiles = styled(Box)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
`;

const StyledTile = styled(Box)`
  background-color: #f8f8f8;
  border-radius: 20px;
  height: 180px;
  display: flex;
  flex-direction: row;
  justify-content: space-around;
  align-items: center;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledDoubleTile = styled(StyledTile)`
  grid-column: auto / span 2;
`;

const StyledTileDataItem = styled(Box)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`;

const StyledTileValue = styled(Box)<{ fontSize?: number }>`
  font-size: ${(props) =>
    props.fontSize !== undefined ? props.fontSize : 36}px;
`;

const StyledTileDescription = styled(Typography)`
  font-size: 18px;
`;

const StyledTileSubDescription = styled(Typography)`
  font-size: 13px;
  color: #808080;
`;

export const ProjectTotals: FC<{
  stats: components['schemas']['ProjectStatsModel'];
}> = ({ stats }) => {
  const t = useTranslate();
  const project = useProject();

  return (
    <>
      <StyledTiles>
        <StyledTile>
          <StyledTileDataItem>
            <StyledTileValue>{stats.languageCount}</StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_language_count', 'Languages', {
                count: stats.languageCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>

        <StyledDoubleTile>
          <StyledTileDataItem>
            <StyledTileValue fontSize={28}>{stats.keyCount}</StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_key_count', 'Keys', {
                count: stats.keyCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
          <StyledTileDataItem>
            <StyledTileValue fontSize={28}>
              {stats.baseWordsCount}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_base_words_count', 'Base words', {
                count: stats.baseWordsCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledDoubleTile>

        <StyledDoubleTile>
          <StyledTileDataItem>
            <StyledTileValue fontSize={28}>
              {!isNaN(stats.translatedPercentage)
                ? t(
                    'project_dashboard_percent_count',
                    '{percentage, number, :: % }',
                    {
                      percentage: stats.translatedPercentage / 100,
                    }
                  )
                : t('project_dashboard_percent_nan', '-')}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_translated_percent', 'Translated')}
            </StyledTileDescription>
          </StyledTileDataItem>
          <StyledTileDataItem>
            <StyledTileValue fontSize={28}>
              {!isNaN(stats.reviewedPercentage)
                ? t(
                    'project_dashboard_percent_count',
                    '{percentage, number, :: % }',
                    {
                      percentage: stats.reviewedPercentage / 100,
                    }
                  )
                : t('project_dashboard_percent_nan', '-')}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_reviewed_percent', 'Reviewed')}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledDoubleTile>

        <StyledDoubleTile>
          <StyledTileDataItem>
            <Box>
              <OwnerAvatar
                userOwner={project.userOwner}
                organizationOwner={project.organizationOwner}
              />
            </Box>
            <StyledTileDescription>
              {project.userOwner?.name || project.organizationOwner?.name}
            </StyledTileDescription>
            <StyledTileSubDescription>
              {t('project_dashboard_project_owner', 'Project Owner')}
            </StyledTileSubDescription>
          </StyledTileDataItem>
          <StyledTileDataItem>
            <StyledTileValue fontSize={28}>
              {stats.membersCount}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_member_count', 'Members')}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledDoubleTile>

        <StyledTile>
          <StyledTileDataItem>
            <StyledTileValue>{stats.tagCount}</StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_tag_count', 'Tags')}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>
      </StyledTiles>
    </>
  );
};

type OwnerAvatarProps = {
  userOwner?: components['schemas']['UserAccountModel'];
  organizationOwner?: components['schemas']['SimpleOrganizationModel'];
};

const OwnerAvatar = (props: OwnerAvatarProps) => (
  <AvatarImg
    size={32}
    owner={{
      avatar: props.userOwner?.avatar || props.organizationOwner?.avatar,
      id: props.userOwner?.id || props.organizationOwner!.id,
      name: props.userOwner?.name || props.organizationOwner?.name,
      type: props.userOwner ? 'PROJECT' : 'ORG',
    }}
    autoAvatarType={props.userOwner ? 'IDENTICON' : 'INITIALS'}
    circle={true}
  />
);
