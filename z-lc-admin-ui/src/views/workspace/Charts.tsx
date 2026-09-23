import { arcPath, colorFor, donutArcs, formatNumber, niceAxis, type Point } from './chartModel';

/**
 * Hand-rolled SVG charts.
 *
 * Deliberately no chart library: the whole point of these components is that
 * every pixel is assertable from jsdom (bar heights, tick values, arc paths),
 * and a canvas-based library would turn the frontend gate back into "it
 * rendered something, hopefully right".
 */

const W = 720;
const H = 320;
const PAD = { top: 18, right: 16, bottom: 52, left: 58 };

const plotW = W - PAD.left - PAD.right;
const plotH = H - PAD.top - PAD.bottom;

export interface ChartProps {
  points: Point[];
  /** 值这一列的字段名，用于 tooltip 里说清"这是什么数"。 */
  metricLabel: string;
}

function scaleFor(points: Point[]) {
  return niceAxis(points.map((p) => p.value));
}

function yOf(value: number, max: number): number {
  return PAD.top + plotH - (value / max) * plotH;
}

function Grid({ ticks, max }: { ticks: number[]; max: number }) {
  return (
    <g className="zlc-chart-grid">
      {ticks.map((tick) => (
        <g key={tick}>
          <line
            x1={PAD.left}
            x2={PAD.left + plotW}
            y1={yOf(tick, max)}
            y2={yOf(tick, max)}
            stroke={tick === 0 ? '#c9ced6' : '#eef0f3'}
          />
          <text
            className="zlc-chart-tick"
            x={PAD.left - 8}
            y={yOf(tick, max) + 4}
            textAnchor="end"
            fontSize={11}
            fill="#8c8c8c"
          >
            {formatNumber(tick)}
          </text>
        </g>
      ))}
    </g>
  );
}

const WIDE_CHAR = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/;

/** 显示宽度：CJK 记 1、ASCII 记 0.5。按字符数截会把 '2026-09-05' 这种定长时间标签砍成 '2026-0…'。 */
function textWidth(text: string): number {
  let w = 0;
  for (const ch of text) w += WIDE_CHAR.test(ch) ? 1 : 0.5;
  return w;
}

/** 类目名太长会让整条轴糊成一片；按显示宽度截断 + 原生 title 兜住完整值。 */
function shortLabel(label: string, budget = 8): string {
  if (textWidth(label) <= budget) return label;
  let out = '';
  let w = 0;
  for (const ch of label) {
    const cw = WIDE_CHAR.test(ch) ? 1 : 0.5;
    if (w + cw > budget - 1) break;
    out += ch;
    w += cw;
  }
  return `${out}…`;
}

/**
 * 边缘标签换锚点：折线首尾的点正好落在绘图区两边，居中标签会有一半被 viewBox 裁掉
 * （"2026-09…" 那种断头）。x 不动、只换 text-anchor，标签仍然对着自己的刻度。
 */
function anchorFor(label: string, x: number): 'start' | 'middle' | 'end' {
  const half = textWidth(label) * 6;
  if (x - half < 2) return 'start';
  if (x + half > W - 2) return 'end';
  return 'middle';
}

/**
 * x 坐标由调用方算好传进来 —— 柱心与折线点的落点规则不同（单点时折线居中、
 * 柱状占满一档），在这里再推一遍必然和图形对不齐，而"标签指着错误的点"
 * 是那种比不标更有害的错。
 */
function XLabels({ points, xs }: { points: Point[]; xs: number[] }) {
  // 点多于 12 个时逐根标会让文字重叠：按密度抽样，并永远保住最后一个
  const stride = points.length > 12 ? Math.ceil(points.length / 12) : 1;
  return (
    <g className="zlc-chart-x-labels">
      {points.map((point, index) => {
        const last = index === points.length - 1;
        const shown = shortLabel(point.label);
        if (index % stride !== 0 && !last) return null;
        return (
          <text
            key={`${point.label}-${index}`}
            className="zlc-chart-axis-label"
            x={xs[index]}
            y={H - PAD.bottom + 18}
            textAnchor={anchorFor(shown, xs[index] ?? 0)}
            fontSize={11}
            fill="#595959"
          >
            {shown}
            {/* 没截断就别挂 title：它会把同一串字塞进 textContent，对读屏和测试都是重复噪声 */}
            {shown !== point.label ? <title>{point.label}</title> : null}
          </text>
        );
      })}
    </g>
  );
}

export function BarChart({ points, metricLabel }: ChartProps) {
  const { max, ticks } = scaleFor(points);
  const step = plotW / Math.max(1, points.length);
  const band = Math.min(56, step * 0.62);
  const xs = points.map((_, index) => PAD.left + index * step + step / 2);
  const centreOf = (index: number) => xs[index] ?? PAD.left + index * step + step / 2;
  return (
    <svg className="zlc-chart zlc-chart-bar" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={metricLabel}>
      <Grid ticks={ticks} max={max} />
      {points.map((point, index) => {
        const value = point.value ?? 0;
        // 非零值至少画 1px：0.2 渲染成 0 高的柱子等于说"这组没有"
        const height = value > 0 ? Math.max(1, (value / max) * plotH) : 0;
        const cx = centreOf(index);
        const x = cx - band / 2;
        const y = PAD.top + plotH - height;
        return (
          <g key={`${point.label}-${index}`} className="zlc-chart-bar-item">
            <rect x={x} y={y} width={band} height={height} rx={3} fill={colorFor(index)}>
              <title>{`${point.label} · ${metricLabel} ${formatNumber(point.value)}`}</title>
            </rect>
            <text x={cx} y={y - 5} textAnchor="middle" fontSize={11} fill="#434343">
              {formatNumber(point.value)}
            </text>
          </g>
        );
      })}
      <XLabels points={points} xs={xs} />
    </svg>
  );
}

export function LineChart({ points, metricLabel }: ChartProps) {
  const { max, ticks } = scaleFor(points);
  const step = points.length > 1 ? plotW / (points.length - 1) : 0;
  const coords = points.map((point, index) => ({
    x: PAD.left + (step ? index * step : plotW / 2),
    y: yOf(point.value ?? 0, max),
    point,
  }));
  const path = coords.map((c, index) => `${index === 0 ? 'M' : 'L'} ${c.x.toFixed(2)} ${c.y.toFixed(2)}`).join(' ');
  const first = coords[0];
  const last = coords[coords.length - 1];
  const baseline = (PAD.top + plotH).toFixed(2);
  const area = first && last
    ? `${path} L ${last.x.toFixed(2)} ${baseline} L ${first.x.toFixed(2)} ${baseline} Z`
    : '';
  return (
    <svg className="zlc-chart zlc-chart-line" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={metricLabel}>
      <Grid ticks={ticks} max={max} />
      {area ? <path className="zlc-chart-area" d={area} fill="rgba(47,111,237,0.10)" /> : null}
      {coords.length > 1 ? (
        <path className="zlc-chart-line-path" d={path} fill="none" stroke="#2f6fed" strokeWidth={2} />
      ) : null}
      {coords.map((c, index) => (
        <circle
          key={`${c.point.label}-${index}`}
          className="zlc-chart-dot"
          cx={c.x}
          cy={c.y}
          r={3.5}
          fill="#fff"
          stroke="#2f6fed"
          strokeWidth={2}
        >
          <title>{`${c.point.label} · ${metricLabel} ${formatNumber(c.point.value)}`}</title>
        </circle>
      ))}
      <XLabels points={points} xs={coords.map((c) => c.x)} />
    </svg>
  );
}

export function PieChart({ points, metricLabel }: ChartProps) {
  const arcs = donutArcs(points);
  const total = arcs.reduce((acc, arc) => acc + Math.max(0, arc.value ?? 0), 0);
  const radius = 108;
  const inner = 62;
  const size = 260;
  return (
    <div className="zlc-chart-pie-wrap">
      <svg
        className="zlc-chart zlc-chart-pie"
        viewBox={`${-size / 2} ${-size / 2} ${size} ${size}`}
        width={size}
        height={size}
        role="img"
        aria-label={metricLabel}
      >
        {arcs.map((arc, index) => (
          <path
            key={`${arc.label}-${index}`}
            className="zlc-chart-slice"
            d={arcPath(radius, inner, arc.start, arc.end)}
            fill={colorFor(index)}
          >
            <title>{`${arc.label} · ${formatNumber(arc.value)} (${(arc.fraction * 100).toFixed(1)}%)`}</title>
          </path>
        ))}
        <text textAnchor="middle" y={-4} fontSize={22} fontWeight={600} fill="#262626">
          {formatNumber(total)}
        </text>
        <text textAnchor="middle" y={18} fontSize={11} fill="#8c8c8c">
          {metricLabel}
        </text>
      </svg>
      <ul className="zlc-chart-legend">
        {arcs.map((arc, index) => (
          <li key={`${arc.label}-legend-${index}`}>
            <i style={{ background: colorFor(index) }} />
            <span className="zlc-chart-legend-label">{arc.label}</span>
            <span className="zlc-chart-legend-value">{formatNumber(arc.value)}</span>
            <span className="zlc-chart-legend-share">{(arc.fraction * 100).toFixed(1)}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function NumberCard({ points, metricLabel }: ChartProps) {
  const first = points[0];
  const value = first ? first.value : null;
  return (
    <div className="zlc-chart-number">
      <div className="zlc-chart-number-value">{formatNumber(value)}</div>
      <div className="zlc-chart-number-caption">{metricLabel}</div>
      {first && first.count > 0 && (
        <div className="zlc-chart-number-foot">{formatNumber(first.count)} 条记录参与计算</div>
      )}
    </div>
  );
}
