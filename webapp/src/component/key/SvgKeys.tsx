import {
  ArrowUpward,
  ArrowDownward,
  ArrowBack,
  ArrowForward,
} from '@mui/icons-material';

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

export const KeyEnter = () => (
  <SvgTemplate>
    <g
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.2"
    >
      <path d="M12 3.53088v3c0 1-1 2-2 2H4M7 11.53088l-3-3 3-3" />
    </g>
  </SvgTemplate>
);

export const KeyUp = () => <ArrowUpward style={svgStyle} />;

export const KeyDown = () => <ArrowDownward style={svgStyle} />;

export const KeyLeft = () => <ArrowBack style={svgStyle} />;

export const KeyRight = () => <ArrowForward style={svgStyle} />;
