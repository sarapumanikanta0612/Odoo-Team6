/**
 * Standard page heading for signed-in pages. Also sets the browser tab title.
 *
 *   <PageHeader title="Finance" subtitle="Budgets and payments" extra={<Button>Record payment</Button>} />
 */
export default function PageHeader({ title, subtitle, eyebrow, extra }) {
  return (
    <header className="wams-page-header">
      <title>{`${title} · WAMS`}</title>
      <div className="wams-page-header__text">
        {eyebrow && <div className="wams-eyebrow">{eyebrow}</div>}
        <h1 className="wams-page-header__title">{title}</h1>
        {subtitle && <p className="wams-page-header__subtitle">{subtitle}</p>}
      </div>
      {extra && <div className="wams-page-header__extra">{extra}</div>}
    </header>
  )
}
