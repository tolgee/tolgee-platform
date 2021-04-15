import {MessageService} from "../service/MessageService";
import {container} from "tsyringe";

const messageService = container.resolve(MessageService)

export const useSuccessMessage = () => messageService.success.bind(messageService)

