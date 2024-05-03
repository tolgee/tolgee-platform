import { ActivityModel, ActivityTypeEnum } from 'tg.component/activity/types';
import { TranslationHistoryModel } from './HistoryTypes';

const TYPE_MAP: Record<
  TranslationHistoryModel['revisionType'],
  ActivityTypeEnum
> = {
  ADD: 'TRANSLATION_HISTORY_ADD',
  MOD: 'TRANSLATION_HISTORY_MODIFY',
  DEL: 'TRANSLATION_HISTORY_DELETE',
};

export const mapHistoryToActivity = (
  data: TranslationHistoryModel
): ActivityModel => {
  const result: ActivityModel = {
    revisionId: 0,
    type: TYPE_MAP[data.revisionType],
    author: data.author,
    timestamp: data.timestamp,
    modifiedEntities: {
      Translation: [
        {
          entityId: 0,
          modifications: data.modifications,
          entityClass: 'Translation',
        },
      ],
    },
  };

  return result;
};
