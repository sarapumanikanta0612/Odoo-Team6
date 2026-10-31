/**
 * WAMS visual identity ("Temple Luxe"): ivory grounds, deep maroon, antique gold,
 * with forest green reserved for positive states.
 *
 * Keep `palette` in sync with the CSS variables in src/styles/global.css.
 */
export const palette = Object.freeze({
  ivory: '#FAF6EE',
  ivoryDeep: '#F3EBDD',
  paper: '#FFFDF8',
  maroon: '#6B1E2E',
  maroonDeep: '#4E1421',
  gold: '#B08D57',
  goldLight: '#D9C29A',
  forest: '#2F5D50',
  slate: '#3E5C76',
  ink: '#2A211C',
  inkSoft: '#4A3F38',
  muted: '#6F6259',
  line: '#E6DCCB',
  lineSoft: '#EFE6D6',
  danger: '#A3302B',
  warning: '#B7791F',
})

export const fonts = Object.freeze({
  display: "'Cormorant Garamond', Georgia, 'Times New Roman', serif",
  body: "'Hind Madurai', 'Segoe UI', system-ui, -apple-system, 'Helvetica Neue', Arial, sans-serif",
})

const quietShadow = '0 1px 2px rgba(42, 33, 28, 0.06), 0 6px 16px rgba(42, 33, 28, 0.06)'

/** Ant Design theme passed to the root <ConfigProvider>. */
export const antdTheme = {
  token: {
    colorPrimary: palette.maroon,
    colorInfo: palette.slate,
    colorSuccess: palette.forest,
    colorWarning: palette.warning,
    colorError: palette.danger,
    colorLink: palette.maroon,
    colorLinkHover: '#8A2A3D',
    colorText: palette.ink,
    colorTextSecondary: palette.muted,
    colorTextHeading: palette.ink,
    colorBgLayout: palette.ivory,
    colorBgContainer: palette.paper,
    colorBorder: palette.line,
    colorBorderSecondary: palette.lineSoft,
    fontFamily: fonts.body,
    fontSize: 14,
    borderRadius: 4,
    borderRadiusLG: 6,
    borderRadiusSM: 3,
    controlHeight: 36,
    motionDurationFast: '0.1s',
    motionDurationMid: '0.16s',
    motionDurationSlow: '0.22s',
    boxShadow: quietShadow,
    boxShadowSecondary: quietShadow,
    wireframe: false,
  },
  components: {
    Layout: {
      bodyBg: palette.ivory,
      headerBg: palette.paper,
      headerHeight: 60,
      headerPadding: '0 24px',
      siderBg: palette.maroonDeep,
      footerBg: palette.ivory,
    },
    Menu: {
      darkItemBg: palette.maroonDeep,
      darkSubMenuItemBg: palette.maroonDeep,
      darkPopupBg: palette.maroonDeep,
      darkItemColor: 'rgba(250, 246, 238, 0.74)',
      darkItemHoverColor: palette.ivory,
      darkItemHoverBg: 'rgba(217, 194, 154, 0.10)',
      darkItemSelectedBg: 'rgba(217, 194, 154, 0.18)',
      darkItemSelectedColor: '#F6E7C8',
      darkGroupTitleColor: 'rgba(217, 194, 154, 0.78)',
      itemBorderRadius: 4,
      itemMarginInline: 10,
    },
    Button: {
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
      fontWeight: 500,
    },
    Card: {
      headerFontSize: 16,
    },
    Table: {
      headerBg: '#F4ECDF',
      headerColor: palette.ink,
      rowHoverBg: '#FBF5EA',
      borderColor: palette.lineSoft,
    },
    Tag: {
      defaultBg: palette.ivoryDeep,
    },
  },
}
