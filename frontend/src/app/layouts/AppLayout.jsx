import { HomeOutlined, LogoutOutlined, MenuOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Button, Dropdown, Grid, Layout, Menu, Tag } from 'antd'
import { useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { roleLabel } from '../../auth/roles'
import { TempleMark } from '../../components/TempleArt'
import { groupBySection, modulesForUser } from '../modules'

const { Sider, Header, Content } = Layout
const HOME_KEY = '/app'

function buildMenuItems(visibleModules) {
  const items = [{ key: HOME_KEY, icon: <HomeOutlined />, label: <Link to={HOME_KEY}>Home</Link> }]
  for (const group of groupBySection(visibleModules)) {
    items.push({
      type: 'group',
      key: `section:${group.section}`,
      label: group.section,
      children: group.modules.map((module) => ({
        key: `/app/${module.path}`,
        icon: module.icon,
        label: <Link to={`/app/${module.path}`}>{module.label}</Link>,
      })),
    })
  }
  return items
}

function selectedKeyFor(pathname, visibleModules) {
  const match = visibleModules
    .map((module) => `/app/${module.path}`)
    .filter((key) => pathname === key || pathname.startsWith(`${key}/`))
    .sort((a, b) => b.length - a.length)[0]
  return match ?? HOME_KEY
}

/** Signed-in shell: role-filtered sidebar, user menu and the active module page. */
export default function AppLayout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const screens = Grid.useBreakpoint()
  const isMobile = screens.lg === false
  const [collapsed, setCollapsed] = useState(false)

  const visibleModules = useMemo(() => modulesForUser(user), [user])
  const menuItems = useMemo(() => buildMenuItems(visibleModules), [visibleModules])
  const selectedKey = selectedKeyFor(location.pathname, visibleModules)
  const siderHidden = isMobile && collapsed

  const userMenu = {
    items: [{ key: 'sign-out', icon: <LogoutOutlined />, label: 'Sign out' }],
    onClick: ({ key }) => {
      if (key !== 'sign-out') return
      navigate('/login', { replace: true })
      logout()
    },
  }

  return (
    <Layout className="wams-shell">
      <Sider
        width={248}
        breakpoint="lg"
        collapsedWidth={0}
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        className="wams-shell__sider"
      >
        <Link to={HOME_KEY} className="wams-shell__brand">
          <TempleMark size={30} />
          <span>
            <strong>WAMS</strong>
            <small>Agency workspace</small>
          </span>
        </Link>
        <div className="wams-temple-rule wams-temple-rule--on-dark" aria-hidden="true" />
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={() => isMobile && setCollapsed(true)}
        />
      </Sider>

      {isMobile && !collapsed && (
        <div className="wams-shell__backdrop" onClick={() => setCollapsed(true)} aria-hidden="true" />
      )}

      <Layout>
        <Header className="wams-shell__header">
          {isMobile ? (
            <Button
              type="text"
              icon={<MenuOutlined />}
              aria-label={siderHidden ? 'Open navigation' : 'Close navigation'}
              onClick={() => setCollapsed((current) => !current)}
            />
          ) : (
            <span />
          )}
          <Dropdown menu={userMenu} trigger={['click']} placement="bottomRight">
            <button type="button" className="wams-shell__user" aria-label="Account menu">
              <Avatar size="small" icon={<UserOutlined />} className="wams-shell__avatar" />
              <span className="wams-shell__user-name">{user?.fullName}</span>
              <Tag className="wams-shell__role">{roleLabel(user?.role)}</Tag>
            </button>
          </Dropdown>
        </Header>
        <Content className="wams-shell__content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
