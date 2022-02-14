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

const HORIZONTAL_BRAKEPOINT = 500;

const useStyles = makeStyles((theme) => ({
  container: {
    overflow: 'auto',
  },
  grid: {
    display: 'grid',
    gridAutoFlow: 'dense',
  },
  loading: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    background: theme.palette.lightDivider.main,
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

  const isVertical = width === undefined || width < HORIZONTAL_BRAKEPOINT;

  const gridTemplateColumns = isVertical ? '1fr' : '1fr 1fr';

  return (
    <div className={classes.container}>
      <div className={classes.grid} style={{ width, gridTemplateColumns }}>
        <ToolsTab
          title={t({
            key: 'translation_tools_translation_memory',
            defaultValue: 'Translation memory',
          })}
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

        <ToolsTab
          title={t({
            key: 'translation_tools_machine_translation',
            defaultValue: 'Machine translation',
          })}
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
      </div>
    </div>
  );
});

export default TranslationTools;
