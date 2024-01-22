import { useEffect } from 'react';
import { useSnackbar } from 'notistack';
import { connect } from 'react-redux';

import { messageActions } from 'tg.store/global/MessageActions';
import { Message } from 'tg.store/global/types';
import { AppState } from 'tg.store/index';

interface Props {
  messages: Message[];
}

function SnackBar(props: Props) {
  const { enqueueSnackbar } = useSnackbar();

  useEffect(() => {
    props.messages.forEach((m) => {
      enqueueSnackbar(m.text, { variant: m.variant });
    });
    if (props.messages.length) {
      //todo remove this ugly solution
      messageActions.clear.dispatch();
    }
  }, [props.messages]);

  return null;
}

export default connect((state: AppState) => ({
  messages: state.message.messages,
}))(SnackBar);
