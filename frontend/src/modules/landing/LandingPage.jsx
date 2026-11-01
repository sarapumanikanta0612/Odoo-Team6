import {
  InboxOutlined,
  ProjectOutlined,
  SafetyCertificateOutlined,
  ShopOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { Button, ConfigProvider, Space } from 'antd'
import { useNavigate } from 'react-router'
import { palette } from '../../app/theme'
import { useAuth } from '../../auth/AuthContext'
import { GopuramIllustration, TempleMark } from '../../components/TempleArt'
import './landing.css'

const CAPABILITIES = [
  {
    icon: <InboxOutlined />,
    title: 'Persisted enquiries',
    text: 'Public visitors can submit validated wedding and consultation details. Staff receive a searchable work queue for assignment, follow-ups and conversion.',
  },
  {
    icon: <TeamOutlined />,
    title: 'Accounts and profiles',
    text: 'Admins maintain users, staff and client profiles. Lead planners have read access to the team and can maintain client contact details.',
  },
  {
    icon: <SafetyCertificateOutlined />,
    title: 'Booking qualification',
    text: 'Agreement and deposit evidence are stored against the enquiry. The backend blocks project creation until both records qualify.',
  },
  {
    icon: <ProjectOutlined />,
    title: 'Wedding projects',
    text: 'Admins and assigned planners maintain wedding dates, venues, status and lead ownership. Clients can read only projects linked to their account.',
  },
  {
    icon: <ShopOutlined />,
    title: 'Vendor directory',
    text: 'Admins maintain approved vendor records and account links. Planners browse approved vendors; linked vendor users see only their own profile.',
  },
]

const WORKFLOW = [
  { title: 'Enquiry submitted', text: 'Contact, wedding dates, destination, guest estimate and budget range are persisted.' },
  { title: 'Profiles prepared', text: 'The agency links the primary client and an active authorized lead planner.' },
  { title: 'Agreement signed', text: 'A signed agreement reference, signer and timestamp are recorded.' },
  { title: 'Deposit paid', text: 'The amount, date, method and payment reference are recorded.', gate: true },
  { title: 'Project created', text: 'Only the qualified enquiry can become one wedding project.' },
]

const PORTALS = [
  { role: 'Admin', accent: 'maroon', text: 'Users, employees, clients, qualified projects and vendor directory management.' },
  { role: 'Lead planner', accent: 'gold', text: 'Assigned projects, booking qualification, clients, staff directory and approved vendors.' },
  { role: 'Client', accent: 'forest', text: 'A read-only view of wedding projects linked to the client account.' },
  { role: 'Vendor', accent: 'gold', text: 'A private read-only view of the vendor profile linked to the signed-in account.' },
]

const HERO_FACTS = ['Real persisted enquiries', 'Server-enforced role access', 'Agreement and deposit project gate']

// Gold buttons on the maroon closing band.
const darkBandTheme = { token: { colorPrimary: palette.goldLight, colorTextLightSolid: palette.maroonDeep } }

/**
 * Public introduction to the WAMS platform. It has no app shell; public visitors
 * can submit a persisted enquiry or continue to sign-in.
 */
export default function LandingPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const entry = isAuthenticated
    ? { label: 'Open workspace', to: '/app' }
    : { label: 'Sign in', to: '/login' }

  return (
    <div className="wams-landing">
      <title>WAMS · Wedding Agency Management System</title>

      <header className="wams-landing__header">
        <div className="wams-landing__container wams-landing__header-inner">
          <a href="#top" className="wams-brand" aria-label="WAMS home">
            <TempleMark size={34} className="wams-brand__mark" />
            <span>
              <span className="wams-brand__name">WAMS</span>
              <span className="wams-brand__tag">Wedding Agency Management System</span>
            </span>
          </a>
          <nav className="wams-landing__nav" aria-label="Sections">
            <a href="#capabilities">Capabilities</a>
            <a href="#workflow">Workflow</a>
            <a href="#portals">Portals</a>
          </nav>
          <Space>
            <Button onClick={() => navigate('/enquire')}>Submit enquiry</Button>
            <Button type="primary" onClick={() => navigate(entry.to)}>
              {entry.label}
            </Button>
          </Space>
        </div>
      </header>

      <main id="top">
        <section className="wams-hero" aria-labelledby="hero-title">
          <div className="wams-landing__container wams-hero__grid">
            <div>
              <div className="wams-eyebrow wams-rise">Agency operations platform</div>
              <h1 id="hero-title" className="wams-rise" style={{ '--delay': '60ms' }}>
                Run every wedding your agency plans from one system.
              </h1>
              <p className="wams-hero__lead wams-rise" style={{ '--delay': '120ms' }}>
                WAMS connects validated enquiries, secure account profiles, booking qualification, wedding projects and
                the vendor directory. Each signed-in role sees only the records its backend permissions allow.
              </p>
              <div className="wams-hero__actions wams-rise" style={{ '--delay': '180ms' }}>
                <Button type="primary" size="large" onClick={() => navigate('/enquire')}>
                  Submit an enquiry
                </Button>
                <Button size="large" onClick={() => navigate(entry.to)}>
                  {isAuthenticated ? 'Open your workspace' : 'Staff and client sign in'}
                </Button>
                <Button type="link" size="large" href="#capabilities">
                  See what it covers
                </Button>
              </div>
              <ul className="wams-hero__facts wams-rise" style={{ '--delay': '240ms' }}>
                {HERO_FACTS.map((fact) => (
                  <li key={fact}>{fact}</li>
                ))}
              </ul>
            </div>
            <div className="wams-hero__art wams-rise" style={{ '--delay': '120ms' }}>
              <GopuramIllustration />
            </div>
          </div>
        </section>

        <div className="wams-temple-rule" aria-hidden="true" />

        <section id="capabilities" className="wams-section" aria-labelledby="capabilities-title">
          <div className="wams-landing__container">
            <div className="wams-section__head">
              <div className="wams-eyebrow">Capabilities</div>
              <h2 id="capabilities-title">Available in this build</h2>
              <p>Every item below is connected to a persisted backend API rather than demonstration data.</p>
            </div>
            <div className="wams-cap-grid">
              {CAPABILITIES.map((capability) => (
                <article key={capability.title} className="wams-cap">
                  <span className="wams-cap__icon" aria-hidden="true">
                    {capability.icon}
                  </span>
                  <h3>{capability.title}</h3>
                  <p>{capability.text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section id="workflow" className="wams-section wams-section--tinted" aria-labelledby="workflow-title">
          <div className="wams-landing__container">
            <div className="wams-section__head">
              <div className="wams-eyebrow">Workflow</div>
              <h2 id="workflow-title">From enquiry to qualified project</h2>
              <p>The backend verifies persisted agreement and deposit records before it creates a project.</p>
            </div>
            <ol className="wams-flow">
              {WORKFLOW.map((step, index) => (
                <li key={step.title} className={`wams-flow__step${step.gate ? ' wams-flow__step--gate' : ''}`}>
                  <span className="wams-flow__node" aria-hidden="true">
                    <span>{index + 1}</span>
                  </span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                  {step.gate && <span className="wams-flow__gate">Project gate</span>}
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section id="portals" className="wams-section" aria-labelledby="portals-title">
          <div className="wams-landing__container">
            <div className="wams-section__head">
              <div className="wams-eyebrow">Portals</div>
              <h2 id="portals-title">Current role workspaces</h2>
              <p>Navigation follows the signed-in role, while every API request is authorized again by the server.</p>
            </div>
            <div className="wams-roles">
              {PORTALS.map((portal) => (
                <article key={portal.role} className={`wams-role wams-role--${portal.accent}`}>
                  <h3>{portal.role}</h3>
                  <p>{portal.text}</p>
                </article>
              ))}
            </div>
            <p className="wams-security-note">
              <SafetyCertificateOutlined aria-hidden="true" />
              Access is checked by the server on every request, not only hidden in the interface.
            </p>
          </div>
        </section>
      </main>

      <footer className="wams-landing__footer">
        <div className="wams-temple-rule wams-temple-rule--on-dark" aria-hidden="true" />
        <div className="wams-landing__container wams-cta">
          <div>
            <h2>Sign in to your workspace</h2>
            <p>Use the account your agency set up for you.</p>
          </div>
          <ConfigProvider theme={darkBandTheme}>
            <Button type="primary" size="large" onClick={() => navigate(entry.to)}>
              {entry.label}
            </Button>
          </ConfigProvider>
        </div>
        <div className="wams-landing__container wams-landing__legal">
          <span className="wams-landing__legal-brand">
            <TempleMark size={20} /> WAMS · Wedding Agency Management System
          </span>
          <span>INR · English · Responsive web</span>
        </div>
      </footer>
    </div>
  )
}
