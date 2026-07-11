import { Link, LinkTypeMap } from '@mui/material';
import { OverridableComponent } from '@mui/material/OverridableComponent';
import { ArrowUpRight } from '@untitled-ui/icons-react';

export const LinkExternal: OverridableComponent<LinkTypeMap> = (props) => {
  return (
    <Link target="_blank" rel="noreferrer noopener" {...(props as any)}>
      {(props as any).children}
      <ArrowUpRight
        style={{
          display: 'inline-flex',
          width: '1.43em',
          height: '1.43em',
          verticalAlign: 'bottom',
        }}
      />
    </Link>
  );
};
