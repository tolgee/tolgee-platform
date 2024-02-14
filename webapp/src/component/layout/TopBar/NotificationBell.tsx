/**
 * Copyright (C) 2024 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import { Box, IconButton, styled } from '@mui/material';
import { Notifications } from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
  position: relative;
`;

const StyledNotificationCount = styled(Box)`
  border: 3px ${({ theme }) => theme.palette.navbar.background} solid;
  border-radius: 0.6rem;
  background-color: ${({ theme }) => theme.palette.error.main};
  color: ${({ theme }) => theme.palette.error.contrastText};
  height: 1.2rem;
  min-width: 1.2rem;
  padding: 0 2px;
  font-size: 0.6rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  bottom: 0.2rem;
  right: 0.2rem;
`;

export const NotificationBell: React.FC = () => {
  return (
    <StyledIconButton
      component={Link}
      to={LINKS.NOTIFICATIONS_INBOX.build()}
      color="inherit"
      size="large"
      data-cy="global-notifications-button"
    >
      <Notifications />
      <StyledNotificationCount data-cy="global-notifications-count">
        1
      </StyledNotificationCount>
    </StyledIconButton>
  );
};
