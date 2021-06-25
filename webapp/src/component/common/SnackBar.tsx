import { useEffect } from 'react';
import { useSnackbar } from 'notistack';
import { connect } from 'react-redux';
import { container } from 'tsyringe';

import { MessageActions } from 'tg.store/global/MessageActions';
import { Message } from 'tg.store/global/types';
import { AppState } from 'tg.store/index';

interface Props {
  messages: Message[];
}

const messageActions = container.resolve(MessageActions);

function SnackBar(props: Props) {
  const { enqueueSnackbar } = useSnackbar();

  // const handleClose = (event: SyntheticEvent | MouseEvent, reason?: string) => {
  //     if (reason === 'clickaway') {
  //         return;
  //     }
  //     messageActions.messageExited.dispatch(props.message);
  // };

  // const handleExited = () => {
  //     messageActions.messageExited.dispatch(props.message);
  // };

  useEffect(() => {
    props.messages.forEach((m) => {
      enqueueSnackbar(m.text, { variant: m.variant });
    });
    if (props.messages.length) {
      //todo remove this ugly solution
      messageActions.clear.dispatch();
    }
  }, [props.messages]);

  // const actions = [
  //     <IconButton
  //         key="close"
  //         aria-label="close"
  //         color="inherit"
  //         className={classes.close}
  //         onClick={handleClose}
  //     >
  //         <CloseIcon/>
  //     </IconButton>,
  // ];

  // if (props.message && props.message.undoAction) {
  //     actions.unshift(
  //         <Button key="undo" color="secondary" size="small" onClick={handleClose}>
  //             UNDO
  //         </Button>);
  // }

  return null;

  // return (
  //     <div>
  //         <Snackbar
  //             /* Dont know why. https://material-ui.com/components/snackbars/#ConsecutiveSnackbars.tsx */
  //             key={new Date().getTime()}
  //             anchorOrigin={{
  //                 vertical: 'bottom',
  //                 horizontal: 'left',
  //             }}
  //             open={!!props.message}
  //             autoHideDuration={4000}
  //             onClose={handleClose}
  //             onExited={handleExited}
  //             ContentProps={{
  //                 'aria-describedby': 'message-id',
  //             }}
  //             message={<span id="message-id">{props.message && props.message.text}</span>}
  //             action={actions}
  //         />
  //     </div>
  // );
}

export default connect((state: AppState) => ({
  messages: state.message.messages,
}))(SnackBar);
