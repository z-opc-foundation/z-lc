import { theme, type ThemeConfig } from 'antd';

/**
 * Compact, dense data-console theme (Teable / NocoDB / Directus feel):
 * small radius, tabular numerics, quiet chrome, one accent colour.
 */
export const antdTheme: ThemeConfig = {
  algorithm: [theme.compactAlgorithm],
  token: {
    colorPrimary: '#2f6feb',
    colorInfo: '#2f6feb',
    colorSuccess: '#1a9e5c',
    colorWarning: '#c78104',
    colorError: '#d43b3b',
    colorLink: '#2f6feb',
    borderRadius: 4,
    borderRadiusLG: 6,
    fontSize: 13,
    controlHeight: 28,
    controlHeightSM: 22,
    wireframe: false,
    colorBgLayout: '#f6f7f9',
    colorBgContainer: '#ffffff',
    colorBorder: '#e3e6ea',
    colorBorderSecondary: '#eceef1',
    colorTextBase: '#1c2430',
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Roboto, sans-serif',
    fontFamilyCode:
      '"SFMono-Regular", "JetBrains Mono", Menlo, Consolas, "Liberation Mono", monospace',
    boxShadowTertiary: '0 1px 2px rgba(16, 24, 40, 0.06)',
  },
  components: {
    Layout: {
      headerBg: '#ffffff',
      headerHeight: 48,
      headerPadding: '0 16px',
      bodyBg: '#f6f7f9',
      siderBg: '#1c2430',
    },
    Menu: {
      darkItemBg: '#1c2430',
      darkSubMenuItemBg: '#182029',
      darkItemSelectedBg: '#2f6feb',
      itemHeight: 32,
      iconSize: 14,
      fontSize: 13,
    },
    Table: {
      headerBg: '#fafbfc',
      headerColor: '#5b6675',
      headerSplitColor: '#eceef1',
      borderColor: '#eceef1',
      cellPaddingBlock: 6,
      cellPaddingInline: 10,
      fontSize: 13,
      rowHoverBg: '#f3f6fb',
      stickyScrollBarBg: '#e3e6ea',
    },
    Card: {
      headerFontSize: 14,
      paddingLG: 16,
      headerHeight: 44,
    },
    Button: {
      fontWeight: 500,
      paddingInline: 12,
    },
    Tabs: {
      horizontalItemPadding: '8px 12px',
      cardGutter: 6,
    },
    Descriptions: {
      itemPaddingBottom: 8,
    },
    Form: {
      verticalLabelPadding: '0 0 2px',
      itemMarginBottom: 14,
    },
    Modal: {
      paddingContentHorizontal: 20,
    },
    Tag: {
      borderRadiusSM: 3,
    },
  },
};

/** Shared spacing rhythm so pages do not invent their own magic numbers. */
export const space = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
} as const;
