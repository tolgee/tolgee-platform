export function createAdder<T>(props: { referencingProperty: string }) {
  return (
      items: T[],
      position: { position: 'before' | 'after' | 'start' | 'end'; value?: any }
    ) =>
    (existingItems: T[]) => {
      const newItems: T[] = [];
      existingItems.forEach((item) => {
        if (
          position.position === 'before' &&
          item[props.referencingProperty] === position.value
        ) {
          newItems.push(...items);
        }
        newItems.push(item);
        if (
          position.position === 'after' &&
          item[props.referencingProperty] === position.value
        ) {
          newItems.push(...items);
        }
      });
      if (position.position === 'start') {
        newItems.unshift(...items);
      }
      if (position.position === 'end') {
        newItems.push(...items);
      }
      return newItems;
    };
}
