import { FormControlLabel, Switch } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Props = {
  checked: boolean;
  onChange: (value: boolean) => void;
};

export const MyContributionsToggle: React.FC<Props> = ({
  checked,
  onChange,
}) => {
  const { t } = useTranslate();

  return (
    <FormControlLabel
      labelPlacement="start"
      data-cy="community-my-contributions-toggle"
      control={
        <Switch
          size="small"
          color="primary"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
        />
      }
      label={t('community_my_contributions_toggle', 'My contributions only')}
    />
  );
};
