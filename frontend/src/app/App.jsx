import { App as AntApp, ConfigProvider } from 'antd'
import { RouterProvider } from 'react-router'
import AuthProvider from '../auth/AuthProvider'
import { router } from './router'
import { antdTheme } from './theme'

/**
 * Provider order matters:
 *   ConfigProvider (theme) > AntApp (message/notification/modal context)
 *   > AuthProvider (uses AntApp messages) > RouterProvider (pages use auth).
 * Inside components, use `App.useApp()` for message/notification/modal instead of the static APIs.
 */
export default function App() {
  return (
    <ConfigProvider theme={antdTheme}>
      <AntApp>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  )
}
