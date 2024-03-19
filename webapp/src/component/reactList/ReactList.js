/* eslint-disable */
/**
 * source: https://github.com/caseywebdev/react-list/blob/master/react-list.es6
 */

import PropTypes from 'prop-types';
import React, { Component } from 'react';

const CLIENT_SIZE_KEYS = { x: 'clientWidth', y: 'clientHeight' };
const CLIENT_START_KEYS = { x: 'clientTop', y: 'clientLeft' };
const INNER_SIZE_KEYS = { x: 'innerWidth', y: 'innerHeight' };
const OFFSET_SIZE_KEYS = { x: 'offsetWidth', y: 'offsetHeight' };
const OFFSET_START_KEYS = { x: 'offsetLeft', y: 'offsetTop' };
const OVERFLOW_KEYS = { x: 'overflowX', y: 'overflowY' };
const SCROLL_SIZE_KEYS = { x: 'scrollWidth', y: 'scrollHeight' };
const SCROLL_START_KEYS = { x: 'scrollLeft', y: 'scrollTop' };
const SIZE_KEYS = { x: 'width', y: 'height' };

/**
 * custom thing for Tolgee
 * when editor is open, cell expands, so we keep some reserve at the bottom
 * so the layout is not overflowing
 */
const SIZE_RESERVE_IF_ITEMS_EXPAND = 300;

const NOOP = () => {};

// If a browser doesn't support the `options` argument to
// add/removeEventListener, we need to check, otherwise we will
// accidentally set `capture` with a truthy value.
const PASSIVE = (() => {
  if (typeof window === 'undefined') return false;
  let hasSupport = false;
  try {
    document.createElement('div').addEventListener('test', NOOP, {
      get passive() {
        hasSupport = true;
        return false;
      },
    });
  } catch (e) {
    // noop
  }
  return hasSupport;
})()
  ? { passive: true }
  : false;

const UNSTABLE_MESSAGE = 'ReactList failed to reach a stable state.';
const MAX_SYNC_UPDATES = 40;

const isEqualSubset = (a, b) => {
  for (const key in b) if (a[key] !== b[key]) return false;

  return true;
};

const defaultScrollParentGetter = (component) => {
  const { axis } = component.props;
  let el = component.getEl();
  const overflowKey = OVERFLOW_KEYS[axis];
  while ((el = el.parentElement)) {
    switch (window.getComputedStyle(el)[overflowKey]) {
      case 'auto':
      case 'scroll':
      case 'overlay':
        return el;
    }
  }
  return window;
};

const defaultScrollParentViewportSizeGetter = (component) => {
  const { axis } = component.props;
  const { scrollParent } = component;
  return scrollParent === window
    ? window[INNER_SIZE_KEYS[axis]]
    : scrollParent[CLIENT_SIZE_KEYS[axis]];
};

const constrain = (props, state) => {
  const { length, minSize, type } = props;
  let { from, size, itemsPerRow } = state;
  size = Math.max(size, minSize);
  let mod = size % itemsPerRow;
  if (mod) size += itemsPerRow - mod;
  if (size > length) size = length;
  from =
    type === 'simple' || !from ? 0 : Math.max(Math.min(from, length - size), 0);

  if ((mod = from % itemsPerRow)) {
    from -= mod;
    size += mod;
  }

  if (from === state.from && size == state.size) return state;

  return { ...state, from, size };
};

export class ReactList extends Component {
  static displayName = 'ReactList';

  static propTypes = {
    axis: PropTypes.oneOf(['x', 'y']),
    initialIndex: PropTypes.number,
    itemRenderer: PropTypes.func,
    itemSizeEstimator: PropTypes.func,
    itemSizeGetter: PropTypes.func,
    itemsRenderer: PropTypes.func,
    length: PropTypes.number,
    minSize: PropTypes.number,
    pageSize: PropTypes.number,
    scrollParentGetter: PropTypes.func,
    scrollParentViewportSizeGetter: PropTypes.func,
    threshold: PropTypes.number,
    type: PropTypes.oneOf(['simple', 'variable', 'uniform']),
    useStaticSize: PropTypes.bool,
    useTranslate3d: PropTypes.bool,
  };

  static defaultProps = {
    axis: 'y',
    itemRenderer: (index, key) => React.createElement('div', { key }, index),
    itemsRenderer: (items, ref) => React.createElement('div', { ref }, items),
    length: 0,
    minSize: 1,
    pageSize: 10,
    scrollParentGetter: defaultScrollParentGetter,
    scrollParentViewportSizeGetter: defaultScrollParentViewportSizeGetter,
    threshold: 100,
    type: 'simple',
    useStaticSize: false,
    useTranslate3d: false,
  };

  static getDerivedStateFromProps(props, state) {
    const newState = constrain(props, state);
    return newState === state ? null : newState;
  }

  constructor(props) {
    super(props);
    this.state = constrain(props, {
      itemsPerRow: 1,
      from: props.initialIndex,
      size: 0,
    });
    this.cache = {};
    this.cachedScrollPosition = null;
    this.prevPrevState = {};
    this.unstable = false;
    this.updateCounter = 0;
  }

  componentDidMount() {
    this.updateFrameAndClearCache = this.updateFrameAndClearCache.bind(this);
    window.addEventListener('resize', this.updateFrameAndClearCache);
    this.updateFrame(this.scrollTo.bind(this, this.props.initialIndex));
  }

  componentDidUpdate(prevProps) {
    // Viewport scroll is no longer useful if axis changes
    if (this.props.axis !== prevProps.axis) this.clearSizeCache();

    // If the list has reached an unstable state, prevent an infinite loop.
    if (this.unstable) return;

    if (++this.updateCounter > MAX_SYNC_UPDATES) {
      this.unstable = true;
      return console.error(UNSTABLE_MESSAGE);
    }

    if (!this.updateCounterTimeoutId) {
      this.updateCounterTimeoutId = setTimeout(() => {
        this.updateCounter = 0;
        delete this.updateCounterTimeoutId;
      }, 0);
    }

    this.updateFrame();
  }

  maybeSetState(b, cb) {
    if (isEqualSubset(this.state, b)) return cb();

    this.setState(b, cb);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateFrameAndClearCache);
    this.scrollParent.removeEventListener(
      'scroll',
      this.updateFrameAndClearCache,
      PASSIVE
    );
    this.scrollParent.removeEventListener('mousewheel', NOOP, PASSIVE);
  }

  getOffset(el) {
    const { axis } = this.props;
    let offset = el[CLIENT_START_KEYS[axis]] || 0;
    const offsetKey = OFFSET_START_KEYS[axis];
    do offset += el[offsetKey] || 0;
    while ((el = el.offsetParent));
    return offset;
  }

  getEl() {
    return this.el || this.items;
  }

  getScrollPosition() {
    // Cache scroll position as this causes a forced synchronous layout.
    if (typeof this.cachedScrollPosition === 'number') {
      return this.cachedScrollPosition;
    }
    const { scrollParent } = this;
    const { axis } = this.props;
    const scrollKey = SCROLL_START_KEYS[axis];
    const actual =
      scrollParent === window
        ? // Firefox always returns document.body[scrollKey] as 0 and Chrome/Safari
          // always return document.documentElement[scrollKey] as 0, so take
          // whichever has a value.
          document.body[scrollKey] || document.documentElement[scrollKey]
        : scrollParent[scrollKey];
    const max =
      this.getScrollSize() - this.props.scrollParentViewportSizeGetter(this);
    const scroll = Math.max(0, Math.min(actual, max));
    const el = this.getEl();
    this.cachedScrollPosition =
      this.getOffset(scrollParent) + scroll - this.getOffset(el);
    return this.cachedScrollPosition;
  }

  setScroll(offset) {
    const { scrollParent } = this;
    const { axis } = this.props;
    offset += this.getOffset(this.getEl());
    if (scrollParent === window) return window.scrollTo(0, offset);

    offset -= this.getOffset(this.scrollParent);
    scrollParent[SCROLL_START_KEYS[axis]] = offset;
  }

  getScrollSize() {
    const { scrollParent } = this;
    const { body, documentElement } = document;
    const key = SCROLL_SIZE_KEYS[this.props.axis];
    return scrollParent === window
      ? Math.max(body[key], documentElement[key])
      : scrollParent[key];
  }

  hasDeterminateSize() {
    const { itemSizeGetter, type } = this.props;
    return type === 'uniform' || itemSizeGetter;
  }

  getStartAndEnd(threshold = this.props.threshold) {
    const scroll = this.getScrollPosition();
    const start = Math.max(0, scroll - threshold);
    let end =
      scroll + this.props.scrollParentViewportSizeGetter(this) + threshold;
    if (this.hasDeterminateSize()) {
      end = Math.min(end, this.getSpaceBefore(this.props.length));
    }
    return { start, end };
  }

  getItemSizeAndItemsPerRow() {
    const { axis, useStaticSize } = this.props;
    let { itemSize, itemsPerRow } = this.state;
    if (useStaticSize && itemSize && itemsPerRow) {
      return { itemSize, itemsPerRow };
    }

    const itemEls = this.items.children;
    if (!itemEls.length) return {};

    const firstEl = itemEls[0];

    // Firefox has a problem where it will return a *slightly* (less than
    // thousandths of a pixel) different size for the same element between
    // renders. This can cause an infinite render loop, so only change the
    // itemSize when it is significantly different.
    const firstElSize = firstEl[OFFSET_SIZE_KEYS[axis]];
    const delta = Math.abs(firstElSize - itemSize);
    if (isNaN(delta) || delta >= 1) itemSize = firstElSize;

    if (!itemSize) return {};

    const startKey = OFFSET_START_KEYS[axis];
    const firstStart = firstEl[startKey];
    itemsPerRow = 1;
    for (
      let item = itemEls[itemsPerRow];
      item && item[startKey] === firstStart;
      item = itemEls[itemsPerRow]
    ) {
      ++itemsPerRow;
    }

    return { itemSize, itemsPerRow };
  }

  clearSizeCache() {
    this.cachedScrollPosition = null;
  }

  // Called by 'scroll' and 'resize' events, clears scroll position cache.
  updateFrameAndClearCache(cb) {
    this.clearSizeCache();
    return this.updateFrame(cb);
  }

  updateFrame(cb) {
    this.updateScrollParent();
    if (typeof cb != 'function') cb = NOOP;
    switch (this.props.type) {
      case 'simple':
        return this.updateSimpleFrame(cb);
      case 'variable':
        return this.updateVariableFrame(cb);
      case 'uniform':
        return this.updateUniformFrame(cb);
    }
  }

  updateScrollParent() {
    const prev = this.scrollParent;
    this.scrollParent = this.props.scrollParentGetter(this);
    if (prev === this.scrollParent) return;
    if (prev) {
      prev.removeEventListener('scroll', this.updateFrameAndClearCache);
      prev.removeEventListener('mousewheel', NOOP);
    }
    // If we have a new parent, cached parent dimensions are no longer useful.
    this.clearSizeCache();
    this.scrollParent.addEventListener(
      'scroll',
      this.updateFrameAndClearCache,
      PASSIVE
    );
    // You have to attach mousewheel listener to the scrollable element.
    // Just an empty listener. After that onscroll events will be fired synchronously.
    this.scrollParent.addEventListener('mousewheel', NOOP, PASSIVE);
  }

  updateSimpleFrame(cb) {
    const { end } = this.getStartAndEnd();
    const itemEls = this.items.children;
    let elEnd = 0;

    if (itemEls.length) {
      const { axis } = this.props;
      const firstItemEl = itemEls[0];
      const lastItemEl = itemEls[itemEls.length - 1];
      elEnd =
        this.getOffset(lastItemEl) +
        lastItemEl[OFFSET_SIZE_KEYS[axis]] -
        this.getOffset(firstItemEl);
    }

    if (elEnd > end) return cb();

    const { pageSize, length } = this.props;
    const size = Math.min(this.state.size + pageSize, length);
    this.maybeSetState({ size }, cb);
  }

  updateVariableFrame(cb) {
    if (!this.props.itemSizeGetter) this.cacheSizes();

    const { start, end } = this.getStartAndEnd();
    const { length, pageSize } = this.props;
    let space = 0;
    let from = 0;
    let size = 0;
    const maxFrom = length - 1;

    while (from < maxFrom) {
      const itemSize = this.getSizeOfItem(from);
      if (itemSize == null || space + itemSize > start) break;
      space += itemSize;
      ++from;
    }

    const maxSize = length - from;

    while (size < maxSize && space < end) {
      const itemSize = this.getSizeOfItem(from + size);
      if (itemSize == null) {
        size = Math.min(size + pageSize, maxSize);
        break;
      }
      space += itemSize;
      ++size;
    }

    this.maybeSetState(
      constrain(this.props, { from, itemsPerRow: 1, size }),
      cb
    );
  }

  updateUniformFrame(cb) {
    const { itemSize, itemsPerRow } = this.getItemSizeAndItemsPerRow();

    if (!itemSize || !itemsPerRow) return cb();

    const { start, end } = this.getStartAndEnd();

    const { from, size } = constrain(this.props, {
      from: Math.floor(start / itemSize) * itemsPerRow,
      size: (Math.ceil((end - start) / itemSize) + 1) * itemsPerRow,
      itemsPerRow,
    });

    return this.maybeSetState({ itemsPerRow, from, itemSize, size }, cb);
  }

  getSpaceBefore(index, cache = {}) {
    if (cache[index] != null) return cache[index];

    // Try the static itemSize.
    const { itemSize, itemsPerRow } = this.state;
    if (itemSize) {
      return (cache[index] = Math.floor(index / itemsPerRow) * itemSize);
    }

    // Find the closest space to index there is a cached value for.
    let from = index;
    while (from > 0 && cache[--from] == null);

    // Finally, accumulate sizes of items from - index.
    let space = cache[from] || 0;
    for (let i = from; i < index; ++i) {
      cache[i] = space;
      const itemSize = this.getSizeOfItem(i);
      if (itemSize == null) break;
      space += itemSize;
    }

    return (cache[index] = space);
  }

  cacheSizes() {
    const { cache } = this;
    const { from } = this.state;
    const itemEls = this.items.children;
    const sizeKey = OFFSET_SIZE_KEYS[this.props.axis];
    for (let i = 0, l = itemEls.length; i < l; ++i) {
      cache[from + i] = itemEls[i][sizeKey];
    }
  }

  getSizeOfItem(index) {
    const { cache, items } = this;
    const { axis, itemSizeGetter, itemSizeEstimator, type } = this.props;
    const { from, itemSize, size } = this.state;

    // Try the static itemSize.
    if (itemSize) return itemSize;

    // Try the itemSizeGetter.
    if (itemSizeGetter) return itemSizeGetter(index);

    // Try the cache.
    if (index in cache) return cache[index];

    // Try the DOM.
    if (type === 'simple' && index >= from && index < from + size && items) {
      const itemEl = items.children[index - from];
      if (itemEl) return itemEl[OFFSET_SIZE_KEYS[axis]];
    }

    // Try the itemSizeEstimator.
    if (itemSizeEstimator) return itemSizeEstimator(index, cache);
  }

  scrollTo(index) {
    if (index != null) this.setScroll(this.getSpaceBefore(index));
  }

  scrollAround(index) {
    const current = this.getScrollPosition();
    const bottom = this.getSpaceBefore(index);
    const top =
      bottom -
      this.props.scrollParentViewportSizeGetter(this) +
      this.getSizeOfItem(index);
    const min = Math.min(top, bottom);
    const max = Math.max(top, bottom);
    if (current <= min) return this.setScroll(min);
    if (current > max) return this.setScroll(max);
  }

  getVisibleRange() {
    const { from, size } = this.state;
    const { start, end } = this.getStartAndEnd(0);
    const cache = {};
    let first, last;
    for (let i = from; i < from + size; ++i) {
      const itemStart = this.getSpaceBefore(i, cache);
      const itemEnd = itemStart + this.getSizeOfItem(i);
      if (first == null && itemEnd > start) first = i;
      if (first != null && itemStart < end) last = i;
    }
    return [first, last];
  }

  renderItems() {
    const { itemRenderer, itemsRenderer } = this.props;
    const { from, size } = this.state;
    const items = [];
    for (let i = 0; i < size; ++i) items.push(itemRenderer(from + i, i));
    return itemsRenderer(items, (c) => (this.items = c));
  }

  render() {
    const { axis, length, type, useTranslate3d } = this.props;
    const { from, itemsPerRow } = this.state;

    const items = this.renderItems();
    if (type === 'simple') return items;

    const style = { position: 'relative' };
    const cache = {};
    const bottom = Math.ceil(length / itemsPerRow) * itemsPerRow;
    const size = this.getSpaceBefore(bottom, cache);
    if (size) {
      style[SIZE_KEYS[axis]] = size + SIZE_RESERVE_IF_ITEMS_EXPAND;
      if (axis === 'x') style.overflowX = 'hidden';
    }
    const offset = this.getSpaceBefore(from, cache);
    const x = axis === 'x' ? offset : 0;
    const y = axis === 'y' ? offset : 0;
    const transform = useTranslate3d
      ? `translate3d(${x}px, ${y}px, 0)`
      : `translate(${x}px, ${y}px)`;
    const listStyle = {
      msTransform: transform,
      WebkitTransform: transform,
      transform,
    };
    return React.createElement(
      'div',
      { style: style, ref: (c) => (this.el = c) },
      React.createElement('div', { style: listStyle }, items)
    );
  }
}
