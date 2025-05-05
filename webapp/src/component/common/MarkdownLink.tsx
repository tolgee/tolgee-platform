import { Link as MuiLink } from '@mui/material';
import { stopBubble } from 'tg.fixtures/eventHandler';

type Props = React.DetailedHTMLProps<
  React.AnchorHTMLAttributes<HTMLAnchorElement>,
  HTMLAnchorElement
>;

export const MarkdownLink = (props: Props) => {
  return (
    <MuiLink
      onClick={stopBubble()}
      href={props.href || ''}
      target="_blank"
      rel="nofollow noreferrer noopener"
    >
      {props.children}
    </MuiLink>
  );
};
