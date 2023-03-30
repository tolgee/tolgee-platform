type Props = {
  code: string;
};

export function TranslatedError({ code }: Props) {
  return (
    <>
      {(() => {
        switch (code) {
          default:
            return code;
        }
      })()}
    </>
  );
}
