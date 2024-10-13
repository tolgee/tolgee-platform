import { useTranslate, T } from '@tolgee/react';

export const Namespaces = () => {
  const { t } = useTranslate('namespaced');

  return (
    <div className="tiles namespaces">
      <div>
        <h1>t function with namespace</h1>
        <div>{t('this_is_a_key')}</div>
      </div>

      <div>
        <h1>t function with default namespace</h1>
        <div>{t('this_is_a_key', { ns: '' })}</div>
      </div>

      <div>
        <h1>T component with namespace</h1>
        <div>
          <T keyName="this_is_a_key" ns="namespaced" />
        </div>
      </div>
    </div>
  );
};
