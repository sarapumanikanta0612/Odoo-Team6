# WAMS frontend

React + Ant Design client for the Wedding Agency Management System. It holds the public landing page, sign-in, and the signed-in shell that every module (enquiries, projects, vendors, finance, ...) plugs into.

Maintainer of the shared shell: DEV 4. Module pages belong to their module owners.

## Stack

| Package | Version | Notes |
|---|---|---|
| React | 19 | JavaScript (no TypeScript) |
| Vite | 8 | Dev server on port 5173, proxies `/api` to the backend |
| Ant Design | 6 | Theme tokens in `src/app/theme.js` |
| react-router | 7 | Import from `react-router` (not `react-router-dom`) |
| dayjs | 1 | Dates; also what Ant Design date pickers use |
| oxlint | 1 | `npm run lint` |

Do not add a dependency without agreeing it with the team (no axios, no chart or UI kits).

## Run it

Requires Node.js `^20.19.0` or `>=22.12.0`, and the Spring Boot backend on `http://localhost:8080` (see the root README).

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

| Script | Purpose |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run lint` | oxlint (must pass before a PR) |
| `npm run build` | Production build into `dist/` |
| `npm run preview` | Serve `dist/` on port 4173 |

### Backend connection

The app calls relative paths (`/api/...`). The Vite server proxies them to `VITE_API_PROXY_TARGET` (default `http://localhost:8080`), so the browser never makes cross-origin calls in development. To override, copy `.env.example` to `.env.local`. Every `VITE_*` value ends up in browser code, so never put secrets there.

If the backend is down, API calls fail with "Cannot reach the WAMS server".

To sign in as an admin on a fresh database, start the backend with `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` set (DEV 1's admin bootstrap; `BOOTSTRAP_ADMIN_NAME` is optional). Use local values only and never commit them.

## Structure

```text
src/
  main.jsx                 entry point
  app/
    App.jsx                providers: theme > antd App > auth > router
    router.jsx             route map (do not add module routes here)
    modules.js             module registry: register your module here
    theme.js               Temple Luxe palette and Ant Design tokens
    layouts/AppLayout.jsx  signed-in shell: sidebar, user menu
    pages/                 workspace home, 404, route error page
  auth/                    session storage, AuthProvider/useAuth, guards, sign-in page
  services/apiClient.js    the only way to call the backend
  hooks/useApiQuery.js     load data with loading/error state
  components/              PageHeader, ApiErrorAlert, FullPageSpinner, TempleArt
  utils/format.js          formatINR, formatDate, formatDateTime
  styles/global.css        palette CSS variables and shared `wams-` classes
  modules/
    landing/               public landing page (DEV 4)
    <your-module>/         one folder per module
```

## Routes

| Path | Page | Access |
|---|---|---|
| `/` | Landing page | Public. No API calls, no app shell |
| `/login` | Sign-in | Public |
| `/app` | Workspace home | Signed in |
| `/app/<module.path>/...` | Module pages | Roles listed in the module descriptor |

Anonymous visitors who open `/app/...` are sent to `/login` and returned to that page after sign-in.

## Adding a module

1. Create `src/modules/<name>/` with your pages.
2. Add `src/modules/<name>/index.jsx` that default-exports a descriptor:

   ```jsx
   import { ShopOutlined } from '@ant-design/icons'
   import { SECTIONS } from '../../app/modules'
   import { ROLES } from '../../auth/roles'
   import VendorListPage from './VendorListPage'
   import VendorDetailPage from './VendorDetailPage'

   export default {
     key: 'vendors',
     label: 'Vendors',
     description: 'Vendor directory and approvals.',
     icon: <ShopOutlined />,
     section: SECTIONS.PARTNERS,
     path: 'vendors',                              // -> /app/vendors
     roles: [ROLES.ADMIN, ROLES.LEAD_PLANNER],
     order: 10,
     routes: [
       { index: true, element: <VendorListPage /> },     // /app/vendors
       { path: ':vendorId', element: <VendorDetailPage /> }, // /app/vendors/12
     ],
   }
   ```

3. Register it in `src/app/modules.js`, one import and one array entry:

   ```js
   import vendorsModule from '../modules/vendors'

   export const modules = [
     vendorsModule,
   ]
   ```

The sidebar, workspace home and route guard pick it up automatically. In development, the console reports a missing field, duplicate key or duplicate path.

Sections: `OVERVIEW`, `OPERATIONS`, `PARTNERS` (vendors and clients), `FINANCE`, `INSIGHTS`, `OTHER`.

Only register a module once its pages call real endpoints. Do not register empty or placeholder pages.

## Calling the API

```jsx
import { api } from '../../services/apiClient'
import { useApiQuery } from '../../hooks/useApiQuery'
import ApiErrorAlert from '../../components/ApiErrorAlert'

const { data, loading, error, reload } = useApiQuery(
  (signal) => api.get('/api/vendors', { query: { status: 'APPROVED', page: 0 }, signal }),
  [],
)
if (error) return <ApiErrorAlert error={error} onRetry={reload} />

await api.post('/api/vendors', values)      // JSON body
await api.put(`/api/vendors/${id}`, values)
```

- The bearer token is added automatically. Pass `{ auth: false }` only for public endpoints.
- Failures throw `ApiError` with `status`, `message` (the backend's `ApiError.message`) and `details` (field errors such as `email: must not be blank`). `status` is 0 when the server is unreachable.
- A 401 caused by a rejected token signs the user out. A 401 while the token is still valid is reported as 403 and keeps the session; the backend currently returns 401 for role-denied requests (issue #3).
- Show failures honestly with `<ApiErrorAlert>`. With Ant Design forms, map `details` to field errors if you need per-field messages.

## Auth and roles

```jsx
import { useAuth } from '../../auth/AuthContext'
import { ROLES } from '../../auth/roles'

const { user, hasRole, logout } = useAuth()   // user: { id, fullName, email, role, active }
if (hasRole(ROLES.ADMIN)) { /* show admin-only actions */ }
```

Role checks in the frontend only decide what to show. Every endpoint must enforce access on the backend.

The session (JWT from `POST /api/auth/login`) is kept in `sessionStorage` for the current tab and re-checked with `GET /api/auth/me` on page load. Signing out discards the token in the browser only, because the backend issues stateless tokens (valid for 24 hours).

## UI conventions

- **Theme.** Use Ant Design components with the tokens from `theme.js`; avoid hard-coded colours. In CSS, use the `--wams-*` variables from `global.css` and prefix classes with `wams-` or your module name.
- **Page layout.** Start each page with `<PageHeader title="..." subtitle="..." extra={actions} />`. It also sets the browser tab title. Group content in `.wams-panel` or Ant Design `Card`.
- **Feedback.** Use `const { message, modal, notification } = App.useApp()` from `antd`, not the static `message.success(...)`, so the theme applies. Confirm destructive and financial actions.
- **States.** Every data view needs loading, empty (`<Empty>`), error (`<ApiErrorAlert>`) and success states.
- **Money and dates.** Use `formatINR(value)` (Indian grouping, ₹) and `formatDate(value)` / `formatDateTime(value)` from `utils/format.js`.
- **Ant Design 6.** Avoid deprecated props, which print console warnings: `Alert` uses `title` (not `message`), `Space` uses `orientation` (not `direction`), `Card` uses `variant` (not `bordered`), and sizes use `medium` (not `default` or `middle`).
- **Motion.** Keep animation short and functional. `global.css` disables it for users who prefer reduced motion.

## Known limitations

- Role-denied requests return 401 from the backend, so the client re-checks the token before deciding (issue #3).
- No server-side logout; a token stays valid until it expires.
- Fonts (Cormorant Garamond, Hind Madurai) load from Google Fonts. Offline, the page falls back to Georgia and the system sans-serif.
- Client self-registration and password reset have no screens yet; the sign-in page offers neither.
