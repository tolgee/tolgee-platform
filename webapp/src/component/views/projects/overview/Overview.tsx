import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Navigation } from '../../../navigation/Navigation';
import { Path } from '../../../navigation/Path';
import { BaseView } from '../../../layout/BaseView';
import { useRepository } from '../../../../hooks/useRepository';
import { LINKS, PARAMS } from '../../../../constants/links';

const useStyles = makeStyles({
  container: {
    display: 'flex',
  },
});

const Overview = () => {
  const repository = useRepository();

  return (
    <BaseView
      navigation={
        <Navigation>
          <Path
            path={[
              [
                repository.name,
                LINKS.REPOSITORY.build({
                  [PARAMS.REPOSITORY_ID]: repository.id,
                }),
              ],
            ]}
          />
        </Navigation>
      }
    >
      <div>Hello</div>
    </BaseView>
  );
};

export default Overview;
