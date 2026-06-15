import { Box, Link as MuiLink } from '@mui/material';
import ReactMarkdown from 'react-markdown';

type Props = {
  description: string;
};

export const ProviderDescription: React.FC<React.PropsWithChildren<Props>> = ({
  description,
}) => {
  return (
    <Box>
      <ReactMarkdown
        components={{
          a: (props) => (
            <MuiLink
              href={props.href || ''}
              target="_blank"
              rel="nofollow noreferrer noopener"
            >
              {props.children}
            </MuiLink>
          ),
        }}
      >
        {description}
      </ReactMarkdown>
    </Box>
  );
};
