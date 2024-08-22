import React, { FC } from 'react';
import { Box, Button } from '@mui/material';

export const CollapsibleActivityGroup: FC<{
  expandedChildren?: React.ReactNode;
}> = (props) => {
  const [expanded, setExpanded] = React.useState(false);

  const expandedContent = expanded
    ? props.expandedChildren
      ? props.expandedChildren
      : false
    : null;

  return (
    <Box>
      {props.children}
      {props.expandedChildren !== undefined && (
        <Button onClick={() => setExpanded(!expanded)}>Toggle</Button>
      )}
      {expandedContent && <Box>{expandedContent}</Box>}
    </Box>
  );
};
