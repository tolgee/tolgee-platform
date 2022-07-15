import { container } from 'tsyringe';

import { MessageService } from '../service/MessageService';

const messageService = container.resolve(MessageService);

export const useSuccessMessage = () =>
  messageService.success.bind(messageService);

export const useErrorMessage = () => messageService.error.bind(messageService);

export const useMessage = () => ({
  success: messageService.success.bind(messageService),
  error: messageService.error.bind(messageService),
});
