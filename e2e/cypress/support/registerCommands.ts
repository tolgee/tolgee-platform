//@ts-ignore
import { addCustomCommand } from 'cy-verify-downloads';

export const register = () => {
  Cypress.Commands.add(
    'closestDcy',
    { prevSubject: true },
    (subject, dataCy) => {
      return subject.closest('[data-cy="' + dataCy + '"]');
    }
  );

  Cypress.Commands.add('gcy', (dataCy, options) => {
    return cy.get('[data-cy="' + dataCy + '"]', options);
  });

  Cypress.Commands.add('findDcy', { prevSubject: true }, (subject, dataCy) => {
    return subject.find('[data-cy="' + dataCy + '"]');
  });

  Cypress.Commands.add(
    'nextUntilDcy',
    { prevSubject: true },
    (subject, dataCy) => {
      return subject.nextUntil('[data-cy="' + dataCy + '"]');
    }
  );

  Cypress.Commands.add(
    'findInputByName',
    { prevSubject: true },
    (subject, name) => {
      return subject.find('input[name="' + name + '"]');
    }
  );

  addCustomCommand();

  Cypress.Commands.add('waitForDom', () => {
    cy.window().then((win: any) => {
      let timeElapsed = 0;

      cy.log('Waiting for DOM mutations to complete');

      return new Cypress.Promise((resolve) => {
        // set the required variables

        // eslint-disable-next-line
        const async = require('async');
        // eslint-disable-next-line prefer-const
        let observerConfig = {
          attributes: true,
          childList: true,
          subtree: true,
        };
        // eslint-disable-next-line
        let items = Array.apply(null, { length: 50 }).map(Number.call, Number);
        win.mutationCount = 0;
        win.previousMutationCount = null;

        // create an observer instance
        const observer = new win.MutationObserver((mutations) => {
          mutations.forEach((mutation) => {
            // Only record "attributes" type mutations that are not a "class" mutation.
            // If the mutation is not an "attributes" type, then we always record it.
            if (
              mutation.type === 'attributes' &&
              mutation.attributeName !== 'class'
            ) {
              win.mutationCount += 1;
            } else if (mutation.type !== 'attributes') {
              win.mutationCount += 1;
            }
          });

          // initialize the previousMutationCount
          if (win.previousMutationCount == null) win.previousMutationCount = 0;
        });

        // watch the document body for the specified mutations
        observer.observe(win.document.body, observerConfig);

        // check the DOM for mutations up to 50 times for a maximum time of 5 seconds
        async.eachSeries(
          items,
          function iteratee(item, callback) {
            // keep track of the elapsed time so we can log it at the end of the command
            timeElapsed = timeElapsed + 100;

            // make each iteration of the loop 100ms apart
            setTimeout(() => {
              if (win.mutationCount === win.previousMutationCount) {
                // pass an argument to the async callback to exit the loop
                return callback('Resolved - DOM changes complete.');
              } else if (win.previousMutationCount != null) {
                // only set the previous count if the observer has checked the DOM at least once
                win.previousMutationCount = win.mutationCount;
                return callback();
              } else if (
                win.mutationCount === 0 &&
                win.previousMutationCount == null &&
                item === 4
              ) {
                // this is an early exit in case nothing is changing in the DOM. That way we only
                // wait 500ms instead of the full 5 seconds when no DOM changes are occurring.
                return callback(
                  'Resolved - Exiting early since no DOM changes were detected.'
                );
              } else {
                // proceed to the next iteration
                return callback();
              }
            }, 100);
          },
          function done() {
            // Log the total wait time so users can see it
            cy.log(
              `DOM mutations ${
                timeElapsed >= 5000 ? 'did not complete' : 'completed'
              } in ${timeElapsed} ms`
            );

            // disconnect the observer and resolve the promise
            observer.disconnect();
            resolve();
          }
        );
      });
    });
  });

  Cypress.Commands.add(
    'chooseDatePicker',
    (selector: string, value: string) => {
      cy.get(selector).find('input').clear().type(value);
    }
  );
};
