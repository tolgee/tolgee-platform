import { TasksHeaderBig } from './TasksHeaderBig';
import { TasksHeaderCompact } from './TasksHeaderCompact';

type Props = React.ComponentProps<typeof TasksHeaderBig> & { isSmall: boolean };

export const TasksHeader = ({ isSmall, ...props }: Props) => {
  return (
    <>
      {isSmall ? (
        <TasksHeaderCompact {...props} />
      ) : (
        <TasksHeaderBig {...props} />
      )}
    </>
  );
};
