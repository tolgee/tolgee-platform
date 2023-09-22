import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';
import { ItemType } from './types';
import { Check } from '@mui/icons-material';
import { StyledLink } from './StyledComponents';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { LINKS, PARAMS } from 'tg.constants/links';

const StyledContainer = styled(Box)`
  display: flex;
  margin: 0px 8px;
  border-radius: 8px;
  padding: 8px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  gap: 8px;
  align-items: center;
  &.disabled {
    color: ${({ theme }) => theme.palette.emphasis[500]};
  }
  &.active {
    background: ${({ theme }) => theme.palette.quickStart.successBackground};
  }
  &.done {
    background: ${({ theme }) => theme.palette.quickStart.successBackground};
  }
`;

const StyledIndex = styled(Box)`
  display: flex;
  width: 25px;
  height: 25px;
  margin: 0px 3px;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.quickStart.circleNormal};
  border-radius: 50%;
  &.done {
    background: ${({ theme }) => theme.palette.quickStart.circleSuccess};
    color: ${({ theme }) => theme.palette.emphasis[50]};
  }
`;

type Props = {
  index: number;
  item: ItemType;
  done: boolean;
  projectId?: number;
};

export const QuickStartStep = ({ item, index, projectId, done }: Props) => {
  const projectRoute = useRouteMatch(LINKS.PROJECT.template);
  const actions = item.actions?.({ projectId });
  const { quickStartBegin, setQuickStartOpen } = useGlobalActions();
  const quickStartFloating = useGlobalContext(
    (c) => c.quickStartGuide.floating
  );
  const links = actions
    ?.map((i) => i.link)
    .filter((i) => Boolean(i)) as string[];

  const isInProject = !isNaN(Number(projectRoute?.params[PARAMS.PROJECT_ID]));

  const route = useRouteMatch(links);
  const active = Boolean(route && links?.length);

  const disabled = item.needsProject && projectId === undefined;
  return (
    <StyledContainer
      className={clsx({
        disabled,
        active,
        done,
      })}
      data-cy="quick-start-step"
      data-cy-step={item.step}
    >
      <StyledIndex className={clsx({ done })}>
        {done ? <Check fontSize="small" /> : <span>{index}</span>}
      </StyledIndex>
      <Box display="grid" gap={0.5}>
        <div>{item.name}</div>
        <Box display="flex" gap={2}>
          {item.actions?.({ projectId }).map((action, i) => {
            const linkToProject =
              projectId &&
              LINKS.PROJECT_DASHBOARD.build({
                [PARAMS.PROJECT_ID]: projectId,
              });
            const link =
              action.link ||
              (item.needsProject && !isInProject && linkToProject);
            return (
              <StyledLink
                // @ts-ignore
                component={link ? Link : undefined}
                data-cy="quick-start-action"
                key={i}
                to={(!disabled && link) || ''}
                onClick={
                  !disabled
                    ? () => {
                        if (action.highlightItems) {
                          quickStartBegin(item.step, action.highlightItems);
                          if (quickStartFloating) {
                            setQuickStartOpen(false);
                          }
                        }
                      }
                    : undefined
                }
                className={clsx({
                  disabled,
                  secondary: done,
                })}
              >
                {action.label}
              </StyledLink>
            );
          })}
        </Box>
      </Box>
    </StyledContainer>
  );
};
