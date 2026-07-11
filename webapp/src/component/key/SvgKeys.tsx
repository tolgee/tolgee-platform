import {
  ArrowUp,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  CornerDownLeft,
} from '@untitled-ui/icons-react';

interface CommandIconProps {
  children: React.ReactNode;
}

const svgStyle = { display: 'inline', width: 15, height: 17 };

const SvgTemplate = (props: CommandIconProps) => (
  <svg style={svgStyle}>{props.children}</svg>
);

export const KeyCtrl = () => (
  <SvgTemplate>
    <path
      d="M4.505 4.496h2M5.505 5.496v5M8.216 4.496l.055 5.993M10 7.5c.333.333.5.667.5 1v2M12.326 4.5v5.996M8.384 4.496c1.674 0 2.116 0 2.116 1.5s-.442 1.5-2.116 1.5M3.205 9.303c-.09.448-.277 1.21-1.241 1.203C1 10.5.5 9.513.5 8V7c0-1.57.5-2.5 1.464-2.494.964.006 1.134.598 1.24 1.342M12.553 10.5h1.953"
      strokeWidth="1.2"
      stroke="currentColor"
      fill="none"
      strokeLinecap="square"
    />
  </SvgTemplate>
);

export const KeyShift = () => (
  <SvgTemplate>
    <g
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.2"
      transform="scale(.6)"
    >
      <path d="M5.0039 21 18.9961 21ZM15.9961 12 15.9961 17.0039 8.0039 17.0039 8.0039 12 3 12 12 3 21 12ZM15.9961 12"></path>
    </g>
  </SvgTemplate>
);

export const KeyEnter = () => <CornerDownLeft style={svgStyle} />;

export const KeyUp = () => <ArrowUp style={svgStyle} />;

export const KeyDown = () => <ArrowDown style={svgStyle} />;

export const KeyLeft = () => <ArrowLeft style={svgStyle} />;

export const KeyRight = () => <ArrowRight style={svgStyle} />;
