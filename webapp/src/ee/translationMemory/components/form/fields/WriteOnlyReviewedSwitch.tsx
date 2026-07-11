import { FormControlLabel, Switch } from '@mui/material';
import { T } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';

type Props = {
  checked: boolean;
  onChange: (value: boolean) => void;
  disabled?: boolean;
  switchDataCy?: string;
};

/**
 * Controlled "Accept only reviewed translations" switch used by both the full TM settings
 * form and the focused project-TM dialog. Always editable: the flag only gates the virtual
 * half of TM content (translations pulled from write-access-assigned projects). Stored
 * entries are user-managed and unaffected, so flipping the switch post-creation is safe
 * on every TM type.
 */
export const WriteOnlyReviewedSwitch = ({
  checked,
  onChange,
  disabled,
  switchDataCy,
}: Props) => (
  <FormControlLabel
    control={
      <Switch
        checked={checked}
        onChange={(_, v) => onChange(v)}
        disabled={disabled}
        data-cy={switchDataCy}
      />
    }
    label={
      <LabelHint
        title={
          <T
            keyName="translation_memory_settings_write_only_reviewed_hint"
            defaultValue="Only translations in the Reviewed state are surfaced as virtual TM entries from connected projects. Stored entries (manual or imported via TMX) are unaffected."
          />
        }
      >
        <T
          keyName="translation_memory_settings_write_only_reviewed"
          defaultValue="Accept only reviewed translations"
        />
      </LabelHint>
    }
  />
);
