type Insertion<T> = {
  items: T[];
  placement: { position: 'before' | 'after' | 'start' | 'end'; value?: any };
};

function _applyInsertions<T>(
  insertions: Insertion<T>[],
  referencingProperty: string,
  existingItems: T[]
): T[] {
  let newItems: T[] = [...existingItems];

  // Handle 'start' and 'end'
  insertions.forEach(({ items, placement }) => {
    if (placement.position === 'start') {
      newItems = [...items, ...newItems];
    }
    if (placement.position === 'end') {
      newItems = [...newItems, ...items];
    }
  });

  // Handle 'before' and 'after'
  insertions.forEach(({ items, placement }) => {
    if (placement.position === 'before' || placement.position === 'after') {
      const tempItems: T[] = [];
      newItems.forEach((item) => {
        if (
          placement.position === 'before' &&
          item[referencingProperty] === placement.value
        ) {
          tempItems.push(...items);
        }
        tempItems.push(item);
        if (
          placement.position === 'after' &&
          item[referencingProperty] === placement.value
        ) {
          tempItems.push(...items);
        }
      });
      newItems = tempItems;
    }
  });

  return newItems;
}

export function createAdder<T>(props: { referencingProperty: string }) {
  return (
      items: T[],
      position: { position: 'before' | 'after' | 'start' | 'end'; value?: any }
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
