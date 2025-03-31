import { TextField } from 'tg.component/common/form/fields/TextField';

export const LLMProviderForm = () => {
  return (
    <>
      <TextField name="type" label="type" />
      <TextField name="name" label="name" />
      <TextField name="apiKey" label="apiKey" />
      <TextField name="apiUrl" label="apiUrl" />
      <TextField name="deployment" label="deployment" />
      <TextField name="format" label="format" />
      <TextField name="keepAlive" label="keepAlive" />
      <TextField name="model" label="model" />
      <TextField name="priority" label="priority" />
    </>
  );
};
