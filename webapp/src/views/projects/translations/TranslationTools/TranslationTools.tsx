import React from 'react';

import { makeStyles } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { ToolsTab } from './ToolsTab';
import { useTranslationTools } from './useTranslationTools';
import { TranslationMemory } from './TranslationMemory';
import { MachineTranslation } from './MachineTranslation';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useConfig } from 'tg.hooks/useConfig';

const HORIZONTAL_BRAKEPOINT = 500;

const useStyles = makeStyles((theme) => ({
  container: {
    overflow: 'auto',
  },
  grid: {
    display: 'grid',
    gridAutoFlow: 'dense',
  },
  loadingWrapper: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    borderRadius: '4px 4px 0px 0px',
    overflow: 'hidden',
  },
  loading: {
    background: theme.palette.grey[400],
  },
}));

export type Props = {
  width: number | string;
  data: ReturnType<typeof useTranslationTools>;
};

const TranslationTools = React.memo(function TranslationTools({
  width,
  data,
}: Props) {
  const classes = useStyles();
  const t = useTranslate();
  const config = useConfig();

  const isVertical = width === undefined || width < HORIZONTAL_BRAKEPOINT;

  const mtEnabled = Object.values(
    config?.machineTranslationServices.services
  ).some(({ enabled }) => enabled);

  const numberOfItems = mtEnabled ? 2 : 1;

  const gridTemplateColumns = isVertical ? '1fr' : '1fr '.repeat(numberOfItems);

  return (
    <div className={classes.container}>
      <div className={classes.grid} style={{ width, gridTemplateColumns }}>
        <ToolsTab
          title={t(
            'translation_tools_translation_memory',
            'Translation memory'
          )}
          icon={<TranslationMemoryIcon fontSize="small" color="inherit" />}
          badgeNumber={
            data.memory?.data?._embedded?.translationMemoryItems?.length
          }
          data={data.memory}
        >
          <TranslationMemory
            data={data.memory?.data}
            operationsRef={data.operationsRef}
          />
        </ToolsTab>

        {mtEnabled && (
          <ToolsTab
            title={t(
              'translation_tools_machine_translation',
              'Machine translation'
            )}
            icon={<MachineTranslationIcon fontSize="small" color="inherit" />}
            badgeNumber={
              Object.keys(data.machine?.data?.machineTranslations || {}).length
            }
            data={data.machine}
          >
            <MachineTranslation
              data={data.machine?.data}
              operationsRef={data.operationsRef}
            />
          </ToolsTab>
        )}
      </div>
      <div className={classes.loadingWrapper}>
        <SmoothProgress loading={data.isFetching} className={classes.loading} />
      </div>
    </div>
  );
});

export default TranslationTools;
