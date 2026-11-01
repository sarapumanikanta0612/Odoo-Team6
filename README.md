# Wedding Agency Management System (WAMS)

A responsive web-based management system for a single wedding-planning agency. It brings enquiries, wedding projects, functions, clients, employees, vendors, quotations, tasks, budgets, payments, documents and basic communication into one role-based application.

> **MVP constraint:** 20 elapsed development hours with four developers working simultaneously (about 80 person-hours). The MVP is a carefully reduced, functional subset of the complete product vision. See the SRS for full requirements and priority classifications.

## MVP priorities

### MUST HAVE
- Public agency website: Home, About, Services/Packages, Portfolio, Contact and enquiry submission.
- Email/password authentication, role-based access and protected APIs.
- Enquiry and follow-up management.
- Package customization and agreement/deposit workflow.
- Strict project creation gate: **signed service agreement AND booking deposit recorded as paid**.
- Wedding projects, multiple functions, basic venue records and employee assignment.
- Task templates that generate tasks on project creation; planner can manage tasks and employees can update assigned tasks.
- Vendor directory and vendor portal.
- End-to-end vendor quotation request, submission, comparison and client approval.
- Client portal for wedding details, progress, budget/payment summary, selected vendors, quotation decisions and authorized documents.
- Planner/employee portal.
- Basic project budget, client payments, expenses and vendor payments (manual recording; INR).
- Project document upload/download with categories and access control.
- Basic project-based messaging and in-app notifications.
- Admin dashboard, basic calendar and reports with PDF/Excel/CSV export where feasible.
- Core unit/API/database/permission/workflow testing.

### Explicitly not in the MVP
Guest management; hotel and transport logistics; venue booking/payment management; GST calculations; payment gateway; cloud deployment; WhatsApp integration; external calendar sync; real-time/group chat; advanced analytics; native mobile app; payroll/inventory; advanced event-day coordination; advanced cancellation/postponement automation.

A reduced feature must still work with real persisted data. Do not ship fake buttons, static records presented as live data, or placeholder actions.

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | React.js |
| UI | Ant Design |
| Backend | Java, Spring Boot |
| API | REST |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security |
| Repository | GitHub |

Development and demonstration are local for the MVP. Keep configuration cloud-ready, but do not describe the MVP as cloud-deployed.

## User roles

- **Admin:** agency-wide management, users, vendors, projects, approvals and reports.
- **Lead Planner:** assigned projects, functions, tasks, quotations, vendors and permitted expenses.
- **Employee:** assigned weddings, tasks, deadlines and relevant documents.
- **Primary Client:** own wedding information, progress, quotations, payments and authorized documents; can invite a partner and configure partner permissions.
- **Partner Client:** access granted by the primary client.
- **Vendor:** own profile, quotation requests/submissions, assigned bookings and permitted documents/payment information.
- **Public Visitor:** public agency pages and enquiry/consultation request.

Backend authorization is mandatory. Frontend route hiding alone is not security.

## Repository structure

The exact structure may evolve. Keep frontend and backend clearly separated. A suggested layout:

```text
wams/
├── frontend/                 # React + Ant Design
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── layouts/
│   │   ├── services/         # REST API clients
│   │   ├── hooks/
│   │   └── utils/
│   └── package.json
├── backend/                  # Spring Boot
│   ├── src/main/java/...
│   ├── src/main/resources/
│   └── pom.xml
├── database/                 # Optional SQL, migrations or seed scripts
├── docs/
│   └── SRS.md
├── .env.example
└── README.md
```

## Prerequisites

Install the versions agreed by the team:
- Git
- Node.js and npm
- Java JDK compatible with the Spring Boot project
- Maven (or use the Maven wrapper)
- PostgreSQL

The repository currently requires Java 21 (see `backend/pom.xml`), Node.js compatible with `frontend/package.json`, and PostgreSQL.

## Local setup

The commands below match the environment variables and ports used by the application.

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd wams
```

### 2. Configure PostgreSQL

Create a local database and a dedicated development user. Example:

```sql
CREATE DATABASE wams_db;
CREATE USER wams_user WITH PASSWORD 'change_this_local_password';
GRANT ALL PRIVILEGES ON DATABASE wams_db TO wams_user;
```

For PostgreSQL versions/configurations that require schema privileges, grant the required privileges on the `public` schema as well.

### 3. Configure backend environment

Create the backend environment configuration using the variable names implemented by the application. Example values:

```env
# Required PostgreSQL connection settings
DB_HOST=localhost
DB_PORT=5432
DB_NAME=wams_db
DB_USERNAME=wams_user
DB_PASSWORD=change_this_local_password

# Required. Use at least 32 random bytes; never use a sample or committed value.
JWT_SECRET=<secure-random-secret-at-least-32-bytes>

# Optional runtime settings
SERVER_PORT=8080
CORS_ORIGINS=http://localhost:5173,http://localhost:3000

# Optional first-admin bootstrap. Set email and password together on a clean DB.
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=<strong-local-password>
BOOTSTRAP_ADMIN_NAME=Agency Administrator
BOOTSTRAP_ADMIN_EMPLOYEE_CODE=ADMIN-001
```

`DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` have no application defaults. Startup fails when any required value is missing. `DB_PORT` defaults to `5432`. The bootstrap Employee code defaults to `ADMIN-001` and must be unique.

Do not commit real passwords, database credentials, signing keys or production secrets. Environment files containing real values must remain local.

### 4. Start the backend

Use the command appropriate to the repository:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

If the project does not include Maven Wrapper:

```bash
mvn spring-boot:run
```

### 5. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Use the URL printed by the frontend development server. Configure the frontend API base URL to point to the Spring Boot backend.

### 6. Verify the application

- Confirm the backend starts and connects to PostgreSQL.
- Confirm the frontend can reach the backend.
- Register or seed accounts for each role.
- Verify that protected routes and APIs reject unauthorized users.
- Run the critical acceptance flow below.

## Critical demo flow

The most important business rule is that a wedding project must not be created until both the service agreement is signed and the booking deposit is recorded as paid.

1. Submit a public enquiry.
2. Open the enquiry as an authorized planner/admin and record requirements/follow-ups.
3. Select/customize a package.
4. Attach or record the service agreement and mark it signed/accepted.
5. Record the booking deposit as paid.
6. Attempt project creation before both conditions are met; the backend must reject it.
7. Create the project after both conditions are met.
8. Add wedding functions and basic venue records.
9. Assign employees and generate tasks from task templates.
10. Create a vendor quotation request.
11. Submit quotations through vendor accounts.
12. Compare quotations and approve one through the client account.
13. Confirm selected vendor and close alternatives.
14. Record a client payment, expense and vendor payment.
15. Upload a categorized document and verify role-based access.
16. Verify project progress, dashboard figures and basic reports.

## GitHub collaboration workflow

Four developers work on separate computers in one shared repository. Use module ownership with shared integration coordination.

1. Pull the latest `main`.
2. Create a focused feature branch:
   ```bash
   git checkout main
   git pull
   git checkout -b feature/<short-module-name>
   ```
3. Commit small, understandable changes.
4. Push the branch and open a pull request.
5. Request review; run the relevant tests.
6. Merge through the agreed integration coordinator.
7. Pull the updated `main` before starting the next task.

Avoid direct commits to `main`. Integrate early and frequently; do not wait until the final hours. Agree on API paths, DTOs, authentication approach, database naming and shared layout before parallel work accelerates.

### Suggested workstream split

- **Developer 1 – Core backend/security:** authentication, roles, schema foundation, project lifecycle gate and shared API conventions.
- **Developer 2 – Wedding operations:** enquiries, packages, projects, functions, tasks and employee assignment.
- **Developer 3 – Vendor/client workflows:** vendor portal, quotations, client approval and documents.
- **Developer 4 – Frontend integration/finance/admin:** public website, dashboards, finance, calendar, reports, integration tests and coordination.

Adjust the split to the team's strengths. Every module owner must document API contracts, environment variables and test steps.

## Security and data handling

- Hash passwords; never store plaintext passwords.
- Enforce role and record ownership on the backend.
- Restrict access to client, vendor and financial records.
- Validate all input on the server.
- Protect file downloads with authorization checks.
- Keep secrets out of Git.
- Use HTTPS in deployed environments.
- Do not claim email verification/reset works unless a real email provider is configured and tested.

## Testing checklist

- [ ] Backend starts and connects to PostgreSQL.
- [ ] Frontend connects to backend.
- [ ] Login and role-based access work.
- [ ] Client cannot access another client's wedding.
- [ ] Vendor cannot access another vendor's data.
- [ ] Employee access is limited to assigned work.
- [ ] Project creation is blocked without signed agreement.
- [ ] Project creation is blocked without paid booking deposit.
- [ ] Project creation succeeds when both conditions are met.
- [ ] Task templates generate persisted tasks.
- [ ] Vendor quotation submission and client approval work end-to-end.
- [ ] Financial records persist and totals calculate correctly.
- [ ] Document upload/download enforces permissions.
- [ ] Critical acceptance flow works from a clean local setup.

## Documentation

- `docs/SRS.md` should contain the complete product requirements, business rules, MVP priorities, acceptance criteria, data entities, risks and future scope.
- Keep this README synchronized with actual scripts, ports, environment variables and repository layout.
- Document anything not implemented rather than presenting it as complete.

## Scope reminder

The complete product vision includes advanced guest management, venue booking, logistics, GST, vendor performance, event-day coordination, cancellation/postponement workflows, advanced analytics, cloud deployment and integrations. These are future scope unless explicitly promoted after the core MVP is stable.

**Definition of done:** a feature is complete only when its UI uses a real API, the backend validates and authorizes the operation, data persists, errors are handled honestly, and the workflow has been tested.
