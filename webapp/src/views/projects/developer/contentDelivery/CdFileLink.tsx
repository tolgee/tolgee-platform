import { styled } from '@mui/material';

const StyledFile = styled('a')`
  color: ${({ theme }) => theme.palette.text.primary};
`;

type Props = {
  link: string;
  file: string;
};

export const CdFileLink = ({ link, file }: Props) => {
  return (
    <StyledFile
      data-cy="content-delivery-published-file"
      href={link}
      target="_blank"
      rel="noreferrer noopener"
    >
      {file}
    </StyledFile>
  );
};
