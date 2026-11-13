import { MuiTelInputInfo } from 'mui-tel-input';
import { readPhoneValue } from './phoneValue';

const touched = (nationalNumber: string, numberValue: string) =>
  ({ nationalNumber, numberValue } as MuiTelInputInfo);

describe('readPhoneValue', () => {
  it('keeps a prefilled number the user never touched', () => {
    expect(readPhoneValue('+420601234567', null)).toEqual('+420601234567');
  });

  it('reads an empty untouched field as empty', () => {
    expect(readPhoneValue('', null)).toEqual('');
  });

  it('reads a cleared field as empty, so the stored number is dropped', () => {
    expect(readPhoneValue('+420', touched('', '+420'))).toEqual('');
  });

  it('returns the E.164 number once the field has been typed into', () => {
    expect(
      readPhoneValue('+420 601 234 567', touched('601234567', '+420601234567'))
    ).toEqual('+420601234567');
  });

  it('refuses a number that is not valid', () => {
    expect(readPhoneValue('+420 1', touched('1', '+4201'))).toBeNull();
  });
});
