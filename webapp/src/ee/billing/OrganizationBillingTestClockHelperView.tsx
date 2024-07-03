import {
  default as React,
  FunctionComponent,
  useEffect,
  useState,
} from 'react';
import { useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { LINKS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { UseQueryResult } from 'react-query';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import {
  Box,
  Button,
  ButtonProps,
  TextField as MuiTextField,
} from '@mui/material';
import { useDateFormatter } from 'tg.hooks/useLocale';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type InfoType = {
  currentTimestamp: number;
  times: {
    name: string;
    timestamp: number;
    organizationId: number;
    organizationName: string;
    organizationSlug: string;
  }[];
};

export const OrganizationBillingTestClockHelperView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const infoLoadable = useApiQuery({
    url: '/internal/test-clock-helper/get-info' as any,
    method: 'get',
    options: {
      onSuccess: (data) => {
        setStringValue(timeToString(data.currentTimestamp));
      },
    },
  }) as UseQueryResult<InfoType>;

  const moveMutation = useApiMutation({
    url: '/internal/time/{dateTimeString}' as any,
    method: 'put',
    invalidatePrefix: '/internal/test-clock-helper' as any,
  });

  const resetMutation = useApiMutation({
    url: '/internal/time' as any,
    method: 'delete',
    invalidatePrefix: '/internal/test-clock-helper' as any,
  });

  const info = infoLoadable.data;

  const timeToString = (value) => new Date(value).toISOString();

  const [value, setValue] = useState<number>(new Date().getTime());
  const [stringValue, setStringValue] = useState<string>(timeToString(value));

  function parseStringValue() {
    try {
      const parsed = Date.parse(stringValue);
      if (!isNaN(parsed)) {
        return parsed;
      }
    } catch (e) {
      return null;
    }
  }

  useEffect(() => {
    const parsed = parseStringValue();
    if (parsed) {
      setValue(parsed);
    }
  }, [stringValue]);

  const formatter = useDateFormatter();

  const formatDateTime = (value: number) =>
    formatter(value, {
      timeZone: 'UTC',
      dateStyle: 'short',
      timeStyle: 'long',
    });

  const SelectButton: FunctionComponent<{ timestamp } & ButtonProps> = ({
    timestamp,
    ...props
  }) => {
    return (
      <Button
        {...props}
        variant={'outlined'}
        onClick={() => {
          setStringValue(timeToString(timestamp));
        }}
      >
        {formatDateTime(timestamp)}
      </Button>
    );
  };

  function moveClock() {
    moveMutation.mutate({
      path: {
        dateTimeString: value,
      },
    });
  }

  function resetClock() {
    resetMutation.mutate({});
  }

  return (
    <BaseOrganizationSettingsView
      hideChildrenOnLoading={true}
      link={LINKS.ORGANIZATION_BILLING}
      navigation={[
        [
          t('organization-menu-billing-test-clock'),
          LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.build({
            slug: organization!.slug,
          }),
        ],
      ]}
      windowTitle={t({
        key: 'organization-menu-billing-test-clock',
        noWrap: true,
      })}
      maxWidth="normal"
    >
      {info && (
        <>
          Current time is: {formatDateTime(info.currentTimestamp)}
          <br />
          <br />
          {info.times.map((time) => (
            <Box mt={2} key={time.name}>
              {time.organizationName} - {time.name}
              <SelectButton sx={{ ml: 2 }} timestamp={time.timestamp} />
            </Box>
          ))}
          <Box mt={2}>
            + 1 hour
            <SelectButton sx={{ ml: 2 }} timestamp={value + 60 * 60000} />
          </Box>
          <Box mt={2}>
            + 30 seconds
            <SelectButton sx={{ ml: 2 }} timestamp={value + 30000} />
          </Box>
          <br />
          <br />
          <MuiTextField
            onChange={(e) => setStringValue(e.target.value)}
            value={stringValue}
          />
          <Box display="flex" gap={1} justifyContent="end" mt={2}>
            <LoadingButton
              loading={resetMutation.isLoading}
              onClick={resetClock}
              variant="contained"
              color="secondary"
            >
              Reset
            </LoadingButton>
            <LoadingButton
              disabled={value !== parseStringValue()}
              loading={moveMutation.isLoading}
              onClick={moveClock}
              color="primary"
              variant="contained"
            >
              Move clock!
            </LoadingButton>
          </Box>
        </>
      )}
    </BaseOrganizationSettingsView>
  );
};
