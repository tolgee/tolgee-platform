import { container } from 'tsyringe';
import { useSelector } from 'react-redux';
import { AppState } from '../store';
import { useEffect } from 'react';
import { UserDTO } from '../service/response.types';
import { UserActions } from '../store/global/UserActions';

const userActions = container.resolve(UserActions);

export const useUser = (): UserDTO | null => {
  const userDTO = useSelector(
    (state: AppState) => state.user.loadables.userData.data
  );
  const loadError = useSelector(
    (state: AppState) => state.user.loadables.userData.error
  );
  const loading = useSelector(
    (state: AppState) => state.user.loadables.userData.loading
  );
  const jwt = useSelector((state: AppState) => state.global.security.jwtToken);

  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  useEffect(() => {
    if (!userDTO && !loading && !loadError && jwt) {
      userActions.loadableActions.userData.dispatch();
    }
  }, [userDTO, loading, loadError, jwt]);

  if (loadError) {
    throw loadError;
  }

  if (!allowPrivate) {
    return null;
  }

  return userDTO as UserDTO | null;
};
