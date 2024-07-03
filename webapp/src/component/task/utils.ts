import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type TaskModel = components['schemas']['TaskModel'];

export const getLinkToTask = (project: SimpleProjectModel, task: TaskModel) => {
  const languages = new Set([project.baseLanguage!.tag, task.language.tag]);

  return (
    `${LINKS.PROJECT_TRANSLATIONS.build({
      [PARAMS.PROJECT_ID]: project.id,
    })}?task=${task.id}&` +
    Array.from(languages)
      .map((l) => `languages=${l}`)
      .join('&')
  );
};
