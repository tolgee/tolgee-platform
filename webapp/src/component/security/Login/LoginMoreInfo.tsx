import { Box, Typography, styled, Link, useMediaQuery } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import { ReactComponent as ComeIn } from 'tg.svgs/signup/comeIn.svg';
import { SPLIT_CONTENT_BREAK_POINT } from '../SplitContent';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: space-between;
`;

const StyledIllustration = styled(Box)`
  display: grid;
  position: relative;
  margin-bottom: 100px;
`;

const StyledComeIn = styled(ComeIn)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledMouse = styled('img')`
  position: absolute;
  bottom: -25px;
  right: -130px;
  user-select: none;
  pointer-events: none;
`;

export const LoginMoreInfo = () => {
  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);
  const { t } = useTranslate();
  return (
    <StyledContainer>
      {!isSmall && (
        <StyledIllustration>
          <Box
            display="flex"
            flexGrow="1"
            alignItems="end"
            justifyContent="center"
          >
            <StyledComeIn />
          </Box>
          <StyledMouse src="/images/standardMouse.svg" />
        </StyledIllustration>
      )}
      <Box mb={2}>
        <Typography color="textSecondary" variant="h5">
          {t('login_more_title')}
        </Typography>
        <Box mb={3} />
        <Typography color="textSecondary" variant="body2" fontSize={14}>
          <T
            keyName="login_tolgee_website_link"
            params={{
              link: <Link href="https://tolgee.io" target="_blank" />,
            }}
          />
        </Typography>

        <Typography color="textSecondary" variant="body2" fontSize={14}>
          <T
            keyName="login_tolgee_documentation_link"
            params={{
              link: <Link href="https://tolgee.io/platform" target="_blank" />,
            }}
          />
        </Typography>
      </Box>
    </StyledContainer>
  );
};
