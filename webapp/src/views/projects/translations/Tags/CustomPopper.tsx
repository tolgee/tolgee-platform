import { Popper } from '@mui/material';

type Props = React.ComponentProps<typeof Popper>;

export const CustomPopper: React.FC<Props> = ({ children, ...props }) => {
  return (
    // override width, so it can be wider than ref element
    <Popper
      {...props}
      style={{ minWidth: props.style?.width }}
      placement="bottom-start"
      modifiers={[
        {
          name: 'offset',
          options: {
            offset: () => {
              // offset from top
              return [0, 2];
            },
          },
        },
      ]}
    >
      {children}
    </Popper>
  );
};
