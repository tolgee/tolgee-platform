import { render, screen } from '@testing-library/react';

import Result from '@/components/Result';

const setupCountries = () => {}; // Fake function for testing

describe('Result message', () => {
  test('should render "You Win!"', () => {
    render(<Result score={8} restart={setupCountries} />);

    const outcome = screen.getByTestId('outcome').textContent;
    expect(outcome).toEqual('You Win!');
  });

  test('should render "You Lose"', () => {
    render(<Result score={4} restart={setupCountries} />);

    const outcome = screen.getByTestId('outcome').textContent;
    expect(outcome).toEqual('You Lose!');
  });
});
