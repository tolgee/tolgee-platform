import { singleton } from 'tsyringe';
import { Message } from '../store/global/types';
import { ReactNode } from 'react';
import { MessageActions } from '../store/global/MessageActions';
import { VariantType } from 'notistack';

@singleton()
export class MessageService {
  constructor(private actions: MessageActions) {}

  yell(message: ReactNode | string, variant: VariantType) {
    this.actions.showMessage.dispatch(new Message(message, variant));
  }

  success(message: ReactNode) {
    this.yell(message, 'success');
  }

  error(message: ReactNode) {
    this.yell(message, 'error');
  }
}
