import { ReactNode } from 'react';
import { Plus } from '@untitled-ui/icons-react';
import { Button } from '@mui/material';

type Props = {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
  'data-cy'?: string;
};

/**
 * AddButton is a primary call-to-action button with a plus icon. The label is
 * supplied by the consumer, so the component stays generic and free of any
 * translation wiring.
 */
export const AddButton = ({
  children,
  onClick,
  disabled,
  className,
  ...rest
}: Props) => {
  return (
    <Button
      color="primary"
      variant="contained"
      startIcon={<Plus width={19} height={19} />}
      onClick={onClick}
      disabled={disabled}
      className={className}
      {...rest}
    >
      {children}
    </Button>
  );
};
