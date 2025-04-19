import { messageService } from '../service/MessageService';

export const useSuccessMessage = () =>
  messageService.success.bind(messageService);

export const useMessage = () => ({
  success: messageService.success.bind(messageService),
  error: messageService.error.bind(messageService),
});
