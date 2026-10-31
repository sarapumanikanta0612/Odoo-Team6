import {
  BarChartOutlined,
  InboxOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  ShopOutlined,
  TeamOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { Button, ConfigProvider } from 'antd'
import { useNavigate } from 'react-router'
import { palette } from '../../app/theme'
import { useAuth } from '../../auth/AuthContext'
import { GopuramIllustration, TempleMark } from '../../components/TempleArt'
import './landing.css'

const CAPABILITIES = [
  {
    icon: <InboxOutlined />,
    title: 'Enquiries and bookings',
    text: 'Capture website and staff-entered enquiries, record follow-ups and customise packages. A wedding project opens only after the agreement is signed and the booking deposit is paid.',
  },
  {
    icon: <ScheduleOutlined />,
    title: 'Weddings and functions',
    text: 'Plan several functions per wedding with dates, venues and assigned staff. Tasks are generated from templates, and progress follows their status.',
  },
  {
    icon: <ShopOutlined />,
    title: 'Vendors and quotations',
    text: "Keep a vendor directory, request quotations for a function or service, compare submissions and record the client's decision with its history.",
  },
  {
    icon: <TeamOutlined />,
    title: 'Client portal',
    text: 'Clients follow progress, selected vendors, quotation decisions, payments and shared documents. The primary client decides what a partner can see.',
  },
  {
    icon: <WalletOutlined />,
    title: 'Budgets and approvals',
    text: "Track original and revised budgets by category, expenses, client payments and vendor payments in INR. Expenses above a planner's limit go to an admin for approval.",
  },
  {
    icon: <BarChartOutlined />,
    title: 'Calendar and reports',
    text: 'See weddings, consultations, meetings, deadlines and task due dates on one calendar, and export operational and financial reports.',
  },
]

const WORKFLOW = [
  { title: 'Enquiry', text: 'Contact details, dates, destination, guest estimate and budget range.' },
  { title: 'Consultation and package', text: 'Follow-ups, requirements and a customised package.' },
  { title: 'Agreement and deposit', text: 'The signed agreement and the paid booking deposit are both recorded.', gate: true },
  { title: 'Wedding project', text: 'Functions, venues, staff assignments and tasks from templates.' },
  { title: 'Vendors and quotations', text: 'Quotations compared by planners and approved by the client.' },
  { title: 'Payments and reports', text: 'Client payments, expenses and vendor payments against the budget.' },
]

const PORTALS = [
  { role: 'Admin', accent: 'maroon', text: 'Agency-wide view of users, vendors, projects, approvals and reports.' },
  { role: 'Lead planner', accent: 'gold', text: 'Assigned weddings, functions, tasks, quotations and expenses within approval limits.' },
  { role: 'Employee', accent: 'forest', text: 'Assigned weddings, tasks and deadlines, with status updates.' },
  { role: 'Client', accent: 'maroon', text: 'Progress, selected vendors, quotation decisions, payments and shared documents.' },
  { role: 'Vendor', accent: 'gold', text: 'Own profile, quotation requests and submissions, bookings and payment details.' },
]

const HERO_FACTS = ['Built for a single agency', 'Payments in INR, recorded by staff', 'A separate portal for each role']

// Gold buttons on the maroon closing band.
const darkBandTheme = { token: { colorPrimary: palette.goldLight, colorTextLightSolid: palette.maroonDeep } }

/**
 * Public introduction to the WAMS platform. It has no app shell and makes no API
 * calls; its only action is the way in to sign-in.
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
          <Button type="primary" onClick={() => navigate(entry.to)}>
            {entry.label}
          </Button>
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
                WAMS keeps enquiries, wedding projects, functions, vendors, client approvals, budgets and payments in
                one shared record. Planners, employees, clients and vendors each work in their own portal.
              </p>
              <div className="wams-hero__actions wams-rise" style={{ '--delay': '180ms' }}>
                <Button type="primary" size="large" onClick={() => navigate(entry.to)}>
                  {isAuthenticated ? 'Open your workspace' : 'Sign in to WAMS'}
                </Button>
                <Button size="large" href="#capabilities">
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
              <h2 id="capabilities-title">What WAMS covers</h2>
              <p>The records an agency needs from the first enquiry to the final payment, kept in one place.</p>
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
              <h2 id="workflow-title">From first enquiry to final payment</h2>
              <p>Every wedding follows the same path. The project opens only once the booking is confirmed.</p>
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
              <h2 id="portals-title">A portal for every role</h2>
              <p>People see the weddings and records they are responsible for, and nothing else.</p>
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
