import { createBrowserRouter } from 'react-router'
import RequireAuth from '../auth/RequireAuth'
import RequireRole from '../auth/RequireRole'
import FullPageSpinner from '../components/FullPageSpinner'
import { modules } from './modules'
import NotFoundPage from './pages/NotFoundPage'
import RouteErrorPage from './pages/RouteErrorPage'
import WorkspaceHome from './pages/WorkspaceHome'

/**
 * Route map
 *   /         public landing page (no sign-in, no app shell)
 *   /login    sign-in
 *   /app/*    signed-in shell; feature modules mount under /app/<module.path>
 *
 * Top-level pages load on demand so the landing page does not download the app shell.
 */
const lazyPage = (load) => async () => ({ Component: (await load()).default })
const pageLoading = <FullPageSpinner />

const moduleRoutes = modules.map((module) => ({
  path: module.path,
  element: <RequireRole roles={module.roles} />,
  children: module.routes,
}))

export const router = createBrowserRouter([
  {
    path: '/',
    lazy: lazyPage(() => import('../modules/landing/LandingPage')),
    hydrateFallbackElement: pageLoading,
    errorElement: <RouteErrorPage />,
  },
  {
    path: '/enquire',
    lazy: lazyPage(() => import('../modules/enquiry/PublicEnquiryPage')),
    hydrateFallbackElement: pageLoading,
    errorElement: <RouteErrorPage />,
  },
  {
    path: '/login',
    lazy: lazyPage(() => import('../auth/LoginPage')),
    hydrateFallbackElement: pageLoading,
    errorElement: <RouteErrorPage />,
  },
  {
    path: '/app',
    element: <RequireAuth />,
    hydrateFallbackElement: pageLoading,
    errorElement: <RouteErrorPage />,
    children: [
      {
        lazy: lazyPage(() => import('./layouts/AppLayout')),
        children: [
          {
            // Errors inside a module page render within the shell, keeping navigation available.
            errorElement: <RouteErrorPage embedded />,
            children: [
              { index: true, element: <WorkspaceHome /> },
              ...moduleRoutes,
              { path: '*', element: <NotFoundPage embedded /> },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
