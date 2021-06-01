import { createRef, FunctionComponent } from 'react';
import { BoxLoading } from './BoxLoading';
import { Box } from '@material-ui/core';

export const LazyLoadOnScroll: FunctionComponent<{
  onLoad: () => void;
  isMore: Boolean;
  maxHeight: number;
}> = (props) => {
  const wrapperRef = createRef<HTMLDivElement>();
  const loadingDivRef = createRef<HTMLDivElement>();

  const onScroll = () => {
    const wrapper = wrapperRef.current;
    const loadingDiv = loadingDivRef.current;

    if (wrapper && loadingDiv) {
      const minimalScrollTop =
        wrapper.scrollTop + wrapper.clientHeight + loadingDiv.clientHeight;
      if (minimalScrollTop > wrapper.scrollHeight) {
        props.onLoad();
      }
    }
  };

  return (
    <div
      style={{
        maxHeight: props.maxHeight + 'px',
        overflowY: 'auto',
        overflowX: 'hidden',
      }}
      ref={wrapperRef}
      onScroll={onScroll}
    >
      <Box>
        {props.children}
        <div ref={loadingDivRef}>{props.isMore && <BoxLoading p={2} />}</div>
      </Box>
    </div>
  );
};
