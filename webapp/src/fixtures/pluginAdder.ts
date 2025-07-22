type Insertion<T> = {
  items: T[];
  placement: {
    position: 'before' | 'after' | 'start' | 'end';
    value?: any;
    fallbackPosition?: 'start' | 'end';
  };
};

function _applyInsertions<T>(
  insertions: Insertion<T>[],
  referencingProperty: string,
  existingItems: T[]
): T[] {
  let newItems: T[] = [...existingItems];

  insertions.forEach(({ items, placement }) => {
    if (placement.position === 'start') {
      newItems = [...items, ...newItems];
    }
    if (placement.position === 'end') {
      newItems = [...newItems, ...items];
    }
  });

  insertions.forEach(({ items, placement }) => {
    if (placement.position === 'before' || placement.position === 'after') {
      const tempItems: T[] = [];
      let itemFound = false;

      newItems.forEach((item) => {
        if (
          placement.position === 'before' &&
          item[referencingProperty] === placement.value
        ) {
          tempItems.push(...items);
          itemFound = true;
        }
        tempItems.push(item);
        if (
          placement.position === 'after' &&
          item[referencingProperty] === placement.value
        ) {
          tempItems.push(...items);
          itemFound = true;
        }
      });

      if (!itemFound && placement.fallbackPosition) {
        if (placement.fallbackPosition === 'start') {
          newItems = [...items, ...tempItems];
        } else if (placement.fallbackPosition === 'end') {
          newItems = [...tempItems, ...items];
        } else {
          newItems = tempItems;
        }
      } else {
        newItems = tempItems;
      }
    }
  });

  return newItems;
}

export function createAdder<T>(props: { referencingProperty: string }) {
  return (
      items: T[],
      position: {
        position: 'before' | 'after' | 'start' | 'end';
        value?: any;
        fallbackPosition?: 'start' | 'end';
      }
    ) =>
    (existingItems: T[]) =>
      _applyInsertions(
        [{ items, placement: position }],
        props.referencingProperty,
        existingItems
      );
}

export function createMultiAdder<T>(props: { referencingProperty: string }) {
  return (insertions: Insertion<T>[]) => (existingItems: T[]) =>
    _applyInsertions(insertions, props.referencingProperty, existingItems);
}
