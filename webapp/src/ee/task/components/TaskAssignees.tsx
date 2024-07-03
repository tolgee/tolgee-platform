import { Box, styled, Tooltip } from '@mui/material';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';
import { components } from 'tg.service/apiSchema.generated';

type TaskModel = components['schemas']['TaskModel'];
type SimpleUserAccountModel = components['schemas']['SimpleUserAccountModel'];

const StyledAssignees = styled(Box)`
  justify-content: start;
  display: flex;
  flex-wrap: wrap;
  padding: 8px 0px;
`;

const StyledMoreAssignees = styled(Box)`
  width: 24px;
  height: 24px;
  display: flex;
  text-align: center;
  cursor: default;
`;

const renderAssignees = (assignees: SimpleUserAccountModel[]) => {
  return (
    <>
      {assignees.map((user) => (
        <Tooltip
          key={user.id}
          title={
            <div>
              <UserName {...user} />
            </div>
          }
          disableInteractive
        >
          <div>
            <AvatarImg
              owner={{
                name: user.name,
                avatar: user.avatar,
                type: 'USER',
                id: user.id,
              }}
              size={24}
            />
          </div>
        </Tooltip>
      ))}
    </>
  );
};

type Props = {
  task: TaskModel;
  assigneesLimit?: number;
};

export const TaskAssignees = ({ task, assigneesLimit = 2 }: Props) => {
  return (
    <Box display="flex" gap={1}>
      {renderAssignees(task.assignees.slice(0, assigneesLimit))}
      {task.assignees.length > assigneesLimit && (
        <Tooltip
          title={
            <StyledAssignees>
              {renderAssignees(task.assignees.slice(assigneesLimit))}
            </StyledAssignees>
          }
        >
          <StyledMoreAssignees>
            +{task.assignees.length - assigneesLimit}
          </StyledMoreAssignees>
        </Tooltip>
      )}
    </Box>
  );
};
