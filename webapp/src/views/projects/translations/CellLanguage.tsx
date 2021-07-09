import React from 'react';
import { makeStyles } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { CellPlain } from './CellPlain';
import { CircledLanguageIcon } from './CircledLanguageIcon';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles({
  content: {
    display: 'flex',
    alignItems: 'center',
    position: 'relative',
    flexGrow: 1,
    '& > * + *': {
      marginLeft: '5px',
    },
  },
  handle: {
    position: 'absolute',
    right: 4,
    top: 0,
    width: 15,
    height: '100%',
    background: 'grey',
    cursor: 'grab',
  },
});

type Props = {
  language: LanguageModel;
};

export const CellLanguage: React.FC<Props> = ({ language }) => {
  // const [offset, setOffset] = useState(left);
  // const [isDragging, setIsDragging] = useState(false);
  // const [position, setPosition] = useState({ x: 0, y: 0 });
  // const dispatch = useTranslationsDispatch();

  // useEffect(() => {
  //   if (!isDragging) {
  //     setOffset(left);
  //   }
  // }, [isDragging, left]);

  // useEffect(() => {
  //   dispatch;
  // }, [position]);

  const classes = useStyles();
  return (
    // <Draggable
    //   axis="x"
    //   position={position}
    //   onStart={() => {
    //     setIsDragging(true);
    //   }}
    //   onDrag={(e, data) => {
    //     setPosition({ x: data.x, y: 0 });
    //   }}
    //   onStop={() => {
    //     setIsDragging(false);
    //     setPosition({ x: 0, y: 0 });
    //   }}
    //   handle=".handle"
    // >
    //   <Box
    //     position="absolute"
    //     zIndex={isDragging ? 10 : undefined}
    //     flexGrow={1}
    //     bgcolor={isDragging ? '#efefef' : 'transparent'}
    //     left={offset}
    //     width={width}
    //   >
    <CellPlain>
      <div className={classes.content}>
        <CircledLanguageIcon flag={language.flagEmoji} />
        <div>{language.name}</div>
      </div>
      {/* <div className={`${classes.handle} handle`} /> */}
    </CellPlain>
    //   </Box>
    // </Draggable>
  );
};
