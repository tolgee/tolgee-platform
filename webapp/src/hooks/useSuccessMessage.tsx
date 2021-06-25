import { container } from 'tsyringe';

import { MessageService } from '../service/MessageService';

const messageService = container.resolve(MessageService);

export const useSuccessMessage = () =>
  messageService.success.bind(messageService);
