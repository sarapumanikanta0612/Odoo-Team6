import { Empty } from 'antd'
import { useMemo } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roles'
import PageHeader from '../../components/PageHeader'
import { modulesForUser } from '../modules'

/** Landing page after sign-in: who is signed in and which modules their role can open. */
export default function WorkspaceHome() {
  const { user } = useAuth()
  const visibleModules = useMemo(() => modulesForUser(user), [user])
  const firstName = user?.fullName?.trim().split(/\s+/)[0] || 'there'

  return (
    <>
      <PageHeader
        eyebrow="Workspace"
        title={`Welcome, ${firstName}`}
        subtitle={`Signed in as ${user?.email} · ${roleLabel(user?.role)}`}
      />
      <section className="wams-panel" aria-labelledby="workspace-modules">
        <h2 id="workspace-modules" className="wams-panel__title">
          Your modules
        </h2>
        {visibleModules.length === 0 ? (
          <Empty description="No modules are available for your role yet." />
        ) : (
          <div className="wams-module-grid">
            {visibleModules.map((module) => (
              <Link key={module.key} to={`/app/${module.path}`} className="wams-module-card">
                <span className="wams-module-card__icon">{module.icon}</span>
                <strong>{module.label}</strong>
                {module.description && <span className="wams-module-card__text">{module.description}</span>}
              </Link>
            ))}
          </div>
        )}
      </section>
    </>
  )
}
