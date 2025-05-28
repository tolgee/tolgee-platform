import { Box, DialogTitle, Link } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { DOCS_ROOT } from 'tg.constants/docLinks';

type Props = {
  title: string;
};

export const LlmDialogTitleWithLink = ({ title }: Props) => {
  const { t } = useTranslate();
  return (
    <Box
      display="grid"
      sx={{
        gridTemplateColumns: '1fr auto',
        alignItems: 'center',
        pr: 4,
      }}
    >
      <DialogTitle>{title}</DialogTitle>
      <Link
        target="_blank"
        rel="noreferrer noopener"
        href={`${DOCS_ROOT}/platform/projects_and_organizations/llm-providers`}
      >
        {t('llm_provider_docs_link')}
      </Link>
    </Box>
  );
};
