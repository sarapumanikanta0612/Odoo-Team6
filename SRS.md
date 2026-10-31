# Wedding Agency Management System
## Software Requirements Specification (SRS)

**Version:** 1.0  
**Status:** MVP baseline and full-product requirements  
**Target platform:** Responsive web application  
**Region / currency / language:** India / INR / English  
**Development constraint:** 20 elapsed hours, four developers working simultaneously (approximately 80 person-hours)

---

## 1. Purpose and scope

The Wedding Agency Management System (WAMS) is an internal and client-facing web platform for a single wedding-planning agency. It centralizes enquiries, wedding projects, functions, clients, employees, vendors, quotations, tasks, budgets, payments, expenses, documents, communications and operational reporting.

The system is designed around an agency handling multiple luxury weddings per season, each potentially involving hundreds of guests, multiple functions, many vendors, substantial budgets and frequent schedule changes. The existing process may rely on messaging applications, paper diaries, spreadsheets, PDFs and disconnected records. WAMS should provide a shared, permission-controlled source of truth.

This specification deliberately distinguishes:
- **MUST HAVE (MVP):** Required for the 20-hour implementation. These features must work end-to-end.
- **SHOULD HAVE:** Valuable but may be simplified or omitted if the MUST HAVE workflows are not stable.
- **FUTURE SCOPE:** Part of the complete product vision but not expected in the 20-hour MVP.

A feature labelled MUST HAVE may be implemented in a deliberately simplified form. A simplified implementation must still persist and process real data; a static screen, fake button, hard-coded demonstration record or nonfunctional placeholder does not count as implemented.

## 2. Product boundaries

### 2.1 Included product
- One agency and its employees, clients, partners and vendors.
- Public agency website and authenticated role-based portals.
- Responsive website for desktop, tablet and mobile browser.
- Wedding-level and function-level planning.
- Project-based financial tracking, not full accounting software.
- INR only and English only.
- Manual recording of payments; no payment gateway.
- Cloud file storage is the intended full-product direction. For the MVP, use a working configured storage approach appropriate to the local demo; do not claim cloud storage integration unless it is actually connected.

### 2.2 Excluded boundaries
- Multi-agency SaaS tenancy or marketplace.
- Native Android/iOS application.
- Inventory, equipment or material stock management.
- Payroll.
- Full accounting ledger, double-entry accounting or bank reconciliation.
- Payment gateway processing.
- WhatsApp Business API.
- Multi-currency and foreign-exchange handling.
- Separate personalized wedding websites.
- Guest management in the MVP.
- Venue logistics and hotel/transport coordination in the MVP.

## 3. Stakeholders and user roles

| Role | Purpose | Main access |
|---|---|---|
| Super Admin / Admin | Operates the agency system | Agency-wide setup, users, packages, vendors, projects, approvals, reports and oversight |
| Lead Planner | Owns assigned wedding delivery | Assigned projects, client coordination, quotations, functions, tasks, vendors and project expenses within authority |
| Employee | Performs assigned work | Assigned weddings, tasks, deadlines, relevant documents and updates |
| Primary Client | Main client account holder | Own wedding information, progress, approved budget/payment information, documents and quotation decisions |
| Partner Client | Invited second client | Access only to information and actions granted by the primary client / configured permissions |
| Vendor | Provides a service for a wedding | Own profile, quotation requests, own submissions, assigned bookings, documents and permitted status/payment information |
| Public Visitor | Prospective customer | Public website, package information and enquiry/consultation submission |

### 3.1 Access principles
- Enforce authorization on the backend; hiding a frontend control is not sufficient.
- A client or vendor must never access another wedding's private records.
- Vendors can access only their own profile, quotation submissions and assigned bookings.
- Employees can access only assigned work and permitted project data.
- A lead planner can access projects assigned to them.
- Admin has agency-wide access.
- The primary client is the account holder and can invite a partner with configurable permissions.
- Sensitive actions (budget changes, approvals, payment verification, project status changes and expense approvals) must be restricted to authorized roles.
- Record important actions and their actor/timestamp where practical in the MVP; expand to full audit trails in future.

## 4. Technology and architecture

### 4.1 Selected stack
- Frontend: React.js
- UI library: Ant Design
- Backend: Java with Spring Boot
- API style: REST
- Persistence: PostgreSQL
- ORM/data access: Spring Data JPA / Hibernate
- Security: Spring Security
- Collaboration: GitHub shared repository
- Development: local environment
- Deployment target: cloud-ready architecture, but cloud deployment is not part of the 20-hour MVP.

### 4.2 Architecture expectations
Use a conventional client-server architecture:
1. React application renders public pages and authenticated portals.
2. React calls Spring Boot REST endpoints.
3. Spring Security authenticates users and enforces role/resource permissions.
4. Spring Data JPA persists business data in PostgreSQL.
5. Files are stored through a storage abstraction or configured file store; file metadata and access associations are persisted.
6. Configuration and secrets are externalized; do not commit credentials.

### 4.3 Suggested backend domains
Authentication/users; enquiries/leads; clients; packages; weddings/projects; functions/events; employees/assignments; vendors; quotations; tasks; budgets/expenses/payments; documents; messages/notifications; calendar; reports.

The team may adjust package structure, but should avoid one giant controller/service and avoid duplicating business rules across endpoints.

## 5. Core business rules and lifecycle

### 5.1 Enquiry-to-project lifecycle
1. A visitor submits an enquiry or a planner/admin records one.
2. The enquiry is assigned and followed up.
3. The agency discusses requirements and prepares a suitable package.
4. The agency prepares a customized service agreement.
5. The client signs/accepts the agreement and the booking deposit is recorded as paid.
6. **Only after BOTH the signed agreement and paid booking deposit are recorded may an official Wedding Project be created.**
7. Before both conditions are met, the record remains an enquiry/prospective booking.
8. The system must prevent premature project creation through backend validation, not only frontend logic.

### 5.2 Package rules
- Admin maintains reusable package templates.
- Planner can customize services, add-ons, prices, deliverables and payment terms for an enquiry.
- The final customized package and agreed commercial terms are associated with the agreement.
- Optional modules/services should be enabled per project only when included in the client's agreed package.

### 5.3 Wedding and function rules
- A wedding project may contain multiple functions.
- Each function has its own date/time, venue reference, schedule, tasks, responsible people and vendors.
- Function-specific expenses roll up into the overall wedding budget.
- Function-specific guest lists are part of the full vision but Guest Management is excluded from MVP.
- Project-level progress should be calculated from task and milestone status rather than manually fabricated.

### 5.4 Client and partner rules
- The primary client is the account holder.
- The primary client can invite a partner to a separate login.
- Partner permissions are configurable; partner access must not automatically equal primary-client access.
- Client-facing financial information and documents must be permission-controlled.

### 5.5 Vendor selection rules
- Agency/admin can add trusted vendors.
- Vendors may also self-register, subject to admin approval.
- Planner requests quotations from suitable vendors and compares submissions.
- Client makes the final approval/selection.
- Once selected, the selected vendor is confirmed and competing quotations are closed/rejected with a recorded status.
- The system must preserve quotation history and decision status.

### 5.6 Financial rules
- Currency is INR.
- No payment gateway; payments are manually recorded and verified by authorized staff.
- Maintain distinct values for original approved budget, revised approved budget and actual expenses.
- Expenses and budget changes are subject to configured approval limits.
- If an expense exceeds a project/category limit, approval is required.
- Lead planner may approve expenses only within configured authority; higher amounts go to admin.
- If an expense increases the client's financial contribution, client approval is also required.
- Record client payments, vendor payments, expenses, outstanding balances and project profitability.
- This is project-based financial tracking, not a general ledger.
- GST calculations and tax automation are excluded from MVP.

### 5.7 Cancellation and postponement
Full product requirements:
- Cancellation charges follow the signed agreement.
- Calculate/record refunds and outstanding amounts.
- Require admin approval.
- Notify affected vendors and employees.
- Preserve project history.
- Postponement retains the project, checks vendor availability, updates functions/tasks/schedules, revises budgets and charges as required, obtains approvals and preserves prior schedule history.
These advanced workflows are future scope unless a minimal status/change record is needed for demo integrity.

## 6. Functional requirements

Priority notation: **M = MUST HAVE (MVP)**, **S = SHOULD HAVE**, **F = FUTURE SCOPE**.

### 6.1 Public agency website
- **M:** Home, About, Services/Packages, Portfolio/Gallery and Contact pages.
- **M:** Public enquiry submission form with validation and persisted enquiry record.
- **M:** Consultation booking request/date capture (a request, not an external calendar booking).
- **S:** Editable public content and package descriptions through admin.
- **F:** SEO/content management suite, personalized wedding microsites and digital invitations.

### 6.2 Authentication and account management
- **M:** Email/password login and logout.
- **M:** Secure password hashing; never store plaintext passwords.
- **M:** Role-based access control with backend enforcement.
- **M:** Admin-created employee accounts; client/vendor self-registration where applicable.
- **M:** Vendor self-registration enters pending admin approval; admin may also add vendors.
- **M:** Client self-registration or planner invitation; primary client can invite partner.
- **M:** Email verification, password-strength policy, login attempt limits and password reset.
- **M:** Input validation and protected routes/API endpoints.
- **F:** Mandatory admin 2FA, device/session monitoring and advanced security event console.

### 6.3 Enquiry and lead management
- **M:** Create enquiries from public form and internal entry.
- **M:** Store contact details, event/wedding dates, destination, estimated guest count, budget range, services of interest and notes.
- **M:** Assign enquiry to planner/admin, update status and record follow-up date/notes.
- **M:** Track enquiry through new, contacted, consultation, proposal, converted or lost states (labels may be adapted).
- **M:** Convert to project only after agreement signed AND booking deposit paid.
- **S:** Lead-source reporting and consultation reminders.
- **F:** Full integrated email communication history, advanced lead scoring and conversion analytics.

### 6.4 Client requirements and package configuration
- **M:** Record key wedding requirements: dates, destination, guest estimate, budget, theme/style, service requirements, functions and special requests.
- **M:** Admin-managed package templates and planner customization of services/add-ons/pricing/deliverables/payment terms.
- **M:** Associate customized package with the prospective booking and agreement.
- **S:** Inspiration image collection and detailed preference questionnaires.
- **F:** Advanced package configurator, automated pricing rules and client self-service package builder.

### 6.5 Agreement and booking qualification
- **M:** Store agreement document and status (draft, sent/pending, signed/accepted, rejected/expired as appropriate).
- **M:** Support upload of signed agreement; record who uploaded/confirmed and timestamp.
- **M:** Record booking deposit as paid manually with date, amount and payment method/reference.
- **M:** Enforce the two-condition project creation gate.
- **S:** Generate agreement from template and produce PDF.
- **F:** Automated document numbering, advanced template merge and certified third-party digital-signature integration.

### 6.6 Wedding project management
- **M:** Create project only after the strict booking gate.
- **M:** Store wedding overview, client, dates, destination, agreed package, budget and assigned lead planner.
- **M:** Assign multiple employees; employees may work on multiple projects.
- **M:** Display project status and task-derived progress.
- **M:** Support cancellation/postponement status fields only if needed; full financial/vendor workflows are future scope.
- **S:** Project milestones and configurable project templates.
- **F:** Full change-control, cancellation/refund and postponement workflows.

### 6.7 Function and event management
- **M:** Add, edit and remove functions within a wedding.
- **M:** Store function name, date, start/end time, venue reference, notes and status.
- **M:** Associate function tasks, vendors and expenses.
- **M:** Roll function expenses into the overall project budget.
- **S:** Detailed run-of-show and function-specific progress dashboard.
- **F:** Minute-by-minute event-day control, planned-versus-actual timing, delay alerts and dependency management.

### 6.8 Vendor directory and vendor portal
- **M:** Admin can create/edit vendor records; vendor self-registration requires approval.
- **M:** Store vendor name, category, contact details and basic service description.
- **M:** Vendor login and permission-limited portal.
- **M:** Vendor sees own quotation requests and assigned bookings only.
- **M:** Vendor can submit/update quotation and upload relevant documents.
- **M:** Vendor can view quotation decision/approval status and update basic booking status.
- **M:** Vendor can upload invoices/documents and view permitted payment details.
- **S:** Availability calendar, vendor agreement and detailed booking lifecycle.
- **F:** Full vendor onboarding, availability matching, check-in/arrival, service completion, final bill verification and performance scoring.

### 6.9 Quotation comparison and approval
- **M:** Planner creates quotation request for a wedding/function/service and selects invited vendors.
- **M:** Vendors submit quotation amount, description, validity and supporting file/details.
- **M:** Planner can view and compare quotations for the same requirement.
- **M:** Client can review and approve/reject a quotation in the client portal.
- **M:** On client approval, selected vendor status becomes selected/confirmed and alternatives are closed/rejected.
- **M:** Preserve quote amount, version/status, submitter and decision timestamp.
- **S:** Structured comparison criteria and downloadable comparison sheet.
- **F:** Automated quote scoring, negotiation history and vendor recommendation engine.

### 6.10 Task and employee work management
- **M:** Predefined task templates automatically generate tasks when a project is created.
- **M:** Planner can create, edit, delete, assign and reassign tasks.
- **M:** Tasks support title, description, project/function, assignee, due date, priority and status.
- **M:** Employee sees assigned weddings, tasks and deadlines.
- **M:** Planner/admin can track pending, in-progress, completed, blocked and overdue work.
- **M:** Progress is calculated from task completion.
- **S:** Task dependencies, recurring tasks and workload view.
- **F:** Attendance, leave, timesheets, performance management and advanced workforce scheduling.

### 6.11 Client portal
- **M:** Client can view own wedding details and functions.
- **M:** Client can view progress and task progress appropriate for client visibility.
- **M:** Client can view approved budget/payment summary and selected vendors.
- **M:** Client can compare and approve/reject vendor quotations.
- **M:** Client can submit requirements and feedback/requests.
- **M:** Client can view/download authorized project documents.
- **M:** Partner access follows primary-client-configured permissions.
- **S:** More detailed timeline, milestone and budget-change approval interface.
- **F:** Full personalized interactive wedding experience, guest RSVP and invitation features.

### 6.12 Planner and employee portal
- **M:** Planner sees assigned weddings and key deadlines.
- **M:** Planner manages functions, tasks, vendor coordination, quotations and project expenses within permissions.
- **M:** Employees see assigned weddings/tasks and can update task status.
- **S:** Workload and employee calendar.
- **F:** Attendance, leave, timesheets, event-day duty rosters and performance analytics.

### 6.13 Admin dashboard
- **M:** Show counts of weddings/projects, enquiries, clients, vendors and employees.
- **M:** Show upcoming weddings and recent activity.
- **M:** Show revenue/client payments, expenses, pending client/vendor payments and wedding-wise profit where data exists.
- **M:** Show enquiry conversion summary.
- **M:** Provide role-appropriate access to dashboard data.
- **S:** Filterable charts and date-range comparisons.
- **F:** Advanced business intelligence, forecasting and customizable dashboards.

### 6.14 Financial management
- **M:** Set project budget and categories.
- **M:** Record client payments manually (amount, date, method/reference, status and recorder).
- **M:** Record project/function expenses with category, amount, date, description, payee and supporting document.
- **M:** Record vendor payments manually and track paid/outstanding amounts.
- **M:** Show budget versus actual, payment status and project-level income/expense/profit summary.
- **M:** Separate original approved budget, revised approved budget and actual expenses.
- **M:** Enforce basic approval authority for expenses/budget changes; higher-limit approval routed to admin.
- **S:** Payment instalment schedules and reminders.
- **F:** Advanced approval rules, client contribution approvals, refunds/adjustments, GST calculations, tax invoices and reconciliation.

### 6.15 Documents
- **M:** Upload/download documents associated with a project and, where relevant, a vendor/function.
- **M:** Categories: Agreements, Quotations, Invoices, Receipts and Other.
- **M:** Store file metadata, uploader and upload timestamp.
- **M:** Apply role-based access; clients and vendors see only authorized files.
- **M:** Vendors can upload invoices/documents; clients can access authorized project documents.
- **S:** Size/type restrictions, preview and search/filter.
- **F:** Automated document generation for agreements, quotations, invoices, receipts and vendor payment records; advanced cloud retention and document lifecycle.

### 6.16 Communication and notifications
- **M:** Basic project-based messages for client-planner, planner-vendor and admin-employee communication.
- **M:** Persist message history and show basic in-app notifications.
- **M:** Apply role/project access checks to messages and attachments.
- **S:** Notification preferences and reminders.
- **F:** Real-time chat, group conversations, read receipts, email/SMS/push delivery, WhatsApp integration and delivery analytics.

### 6.17 Calendar
- **M:** Basic calendar displays wedding dates, consultation/meeting dates, important deadlines and task due dates.
- **S:** Calendar filters and week/month views.
- **F:** Employee/vendor availability calendars and Google/Outlook synchronization.

### 6.18 Venue records
- **M:** Basic venue records: venue name, address and contact details.
- **M:** Associate a venue record with a wedding function.
- **F:** Venue capacity/facilities, availability, inspections, quote/booking management, venue payments and logistics.

### 6.19 Reports and exports
- **M:** Basic operational and financial reports.
- **M:** Authorized users can export relevant reports/records to PDF, Excel and CSV where practical.
- **S:** Filters by date, project, status and category.
- **F:** Custom report builder, advanced analytics and forecasting.

### 6.20 Guest management
- **F:** Guest records, household/grouping, invitations, RSVP, function-level invite lists, dietary preferences, attendance, seating and guest communication.
- **Explicit MVP exclusion:** No guest-management module in the 20-hour build.

### 6.21 Complaints and feedback
- **F:** Basic client complaint record with ID, project/client, description, date and status; admin can mark resolved.
- **F:** Client reviews/feedback about agency service.
- **Explicit MVP exclusion:** No system feedback/suggestion module or help centre.

### 6.22 Logistics
- **F:** Basic hotel and transportation details and booking records.
- **Explicit MVP exclusion:** No logistics module in MVP.

### 6.23 Cancellation, postponement and event-day coordination
- **F:** Cancellation charge/refund/outstanding workflows, approvals and notifications.
- **F:** Postponement workflow with vendor availability checks, schedule/task/budget updates and history.
- **F:** Event-day minute schedule, actual-time updates, arrival tracking, delay alerts, emergency reassignment and escalation.

### 6.24 Vendor performance
- **F:** Historical scoring for punctuality, quality, quotation accuracy, cancellations, complaints and client/planner feedback.

## 7. Non-functional requirements

### 7.1 Security
- Use HTTPS in deployed environments.
- Hash passwords using a modern adaptive password hashing algorithm supported by Spring Security.
- Enforce server-side authorization for every protected operation.
- Validate and sanitize input; use parameterized ORM queries.
- Protect secrets and database credentials outside source control.
- Restrict file type/size and authorize every file download.
- Avoid exposing sensitive client, financial or vendor data in logs.
- MVP includes email verification, password policies and login attempt limits.
- Advanced 2FA/device monitoring is future scope.

### 7.2 Usability and accessibility
- Responsive layout for desktop, tablet and mobile browsers.
- Consistent Ant Design components and clear form validation.
- Role-specific navigation; do not show irrelevant modules.
- Clear empty, loading, success and error states.
- Destructive/financial actions require confirmation and clear feedback.

### 7.3 Reliability and data integrity
- Use database constraints and transactions for critical workflows.
- Project creation gate must be enforced transactionally/server-side.
- Avoid duplicate submissions where practical.
- Store timestamps consistently; display in an appropriate local timezone.
- Do not silently discard failed writes.

### 7.4 Performance and scalability
- MVP should remain usable with representative agency data.
- Paginate potentially large lists.
- Avoid unnecessary repeated API calls.
- Full load/performance testing is future scope.

### 7.5 Maintainability
- Separate frontend components, API clients and domain concerns.
- Separate backend controllers, services, repositories and entities/DTOs.
- Use consistent naming, validation and error responses.
- Provide database migration/schema setup instructions.
- Keep API contracts and environment variables documented.

### 7.6 Compatibility
- Support current mainstream desktop and mobile browsers as practical.
- Responsive web only; no native mobile app.

## 8. Key data entities

A practical relational model should include at least:

- User, Role, Permission (or role mapping)
- Client, ClientPartnerPermission
- Employee
- Vendor, VendorApproval
- Enquiry, FollowUp
- PackageTemplate, PackageService / PackageAddon, CustomizedPackage
- Agreement, BookingDeposit
- WeddingProject, ProjectEmployee
- WeddingFunction, Venue
- TaskTemplate, Task, TaskAssignment
- QuotationRequest, Quotation, QuotationDecision
- Budget, BudgetCategory, BudgetRevision
- ClientPayment, Expense, VendorPayment, Approval
- Document, DocumentAccess/Association
- ProjectMessage, Notification
- CalendarItem / Meeting
- ActivityRecord (limited MVP activity history)

Use stable IDs, created/updated timestamps and appropriate foreign keys. Avoid storing multiple unrelated IDs in comma-separated text fields. Use explicit status enums or controlled values.

## 9. Critical end-to-end acceptance criteria

The following scenarios define whether the MVP is genuinely functional.

### AC-01: Enquiry to qualified booking
Given a new enquiry, staff can record requirements, assign a planner, prepare/customize a package, attach an agreement, record signed status and record a paid booking deposit. The project creation endpoint rejects creation if either signed agreement or paid deposit is missing. Once both are present, an authorized user can create the project.

### AC-02: Project and function setup
An authorized user can open a qualified project, add multiple functions, associate basic venue records, assign employees and view the project in the appropriate portal.

### AC-03: Automatic task creation
Project creation generates tasks from configured task templates. Planner can edit, add, delete and reassign tasks. Employee can update assigned task status. Project progress reflects persisted task statuses.

### AC-04: Vendor quotation decision
Planner sends a quotation request to vendors. Vendors submit quotations. Planner compares them. Client can approve/reject. Approval confirms the selected vendor and closes alternatives. Unauthorized users cannot alter the decision.

### AC-05: Financial record
Authorized staff can record a client payment, expense and vendor payment. Project view shows budget versus actual and payment/outstanding summary based on persisted records.

### AC-06: Role isolation
A client cannot view another client's project. A vendor cannot view another vendor's quotation or booking. An employee cannot access unassigned projects unless explicitly authorized. Admin can access agency-wide records.

### AC-07: Document access
Authorized user uploads a categorized project document. Correctly authorized users can download it; unauthorized users are denied even if they know the file identifier.

### AC-08: Local demonstration
A new developer can follow README instructions to start PostgreSQL, backend and frontend locally and complete the critical demo flow without relying on production credentials or cloud deployment.

## 10. MVP prioritization and implementation guardrails

### MUST HAVE
1. Local full-stack application and database connection.
2. Authentication, roles and backend authorization.
3. Public enquiry submission and basic lead tracking.
4. Package customization and agreement/deposit booking gate.
5. Wedding project and function management.
6. Employee assignment and template-generated tasks.
7. Vendor directory and vendor portal.
8. Quotation request, vendor submission, client comparison/approval and vendor selection.
9. Client portal with project/progress/selected vendors/documents and quotation decisions.
10. Planner/employee portal.
11. Basic project budget, client payments, expenses and vendor payments.
12. Basic document upload/download and categories.
13. Basic project messages and in-app notifications.
14. Admin dashboard.
15. Basic calendar and venue records.
16. Basic reports with PDF/Excel/CSV export where feasible.
17. Critical workflow testing and local setup documentation.

### SHOULD HAVE (only after MUST HAVE flows work)
- More polished dashboards and charting.
- Richer filtering/search.
- Detailed package and agreement PDF generation.
- Better document preview and search.
- More complete notification preferences.
- More detailed calendar views.
- More granular approval settings.
- Additional automated reminders.

### FUTURE SCOPE
Guest management; hotel/transport logistics; venue booking and venue payments; GST; cloud deployment; external calendar/email/SMS integrations; real-time/group chat; WhatsApp integration; advanced BI; full audit console; 2FA/device monitoring; attendance/payroll; event-day command centre; vendor performance scoring; full cancellation/postponement workflows; advanced document automation; native mobile app; cloud backup/recovery management.

### MVP implementation rule
If a MUST HAVE module is too large, reduce its depth, not its integrity. Example: a basic but persisted quotation lifecycle is acceptable; a comparison screen with hard-coded quotations and a fake approve button is not. If a workflow cannot be made functional, mark it as not implemented and move it to future scope rather than presenting it as complete.

## 11. Team execution and Git workflow

Four developers work in parallel for 20 elapsed hours (approximately 80 person-hours). Use a hybrid model: module ownership plus shared integration responsibilities.

Recommended collaboration:
- Shared GitHub repository.
- Each developer works on a feature branch.
- Pull requests into `main`; avoid direct commits to `main`.
- One integration coordinator manages merge order, shared contracts and conflict resolution.
- Agree early on API paths, DTO shapes, auth token/session approach, database naming and shared UI layout.
- Integrate continuously; do not wait until the final hours.
- Keep a working `main` branch and a runnable demo at each integration checkpoint.
- Use seed/demo data only for development convenience; acceptance flows must also work with newly created records.

Suggested workstream split (adjust based on team strengths):
1. **Core backend and security:** authentication, roles, schema foundation, project lifecycle gate and shared API conventions.
2. **Wedding operations:** enquiries, packages, projects, functions, tasks and employee assignment.
3. **Vendor and client workflows:** vendor portal, quotation lifecycle, client approval and documents.
4. **Frontend integration and finance/admin:** public site, dashboards, finance, calendar, reports, integration testing and release coordination.

The integration coordinator should not become a single point of failure. Every module owner must document endpoints, required environment variables and test steps.

## 12. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Scope exceeds 80 person-hours | Incomplete core flows | Protect MUST HAVE list; defer SHOULD/FUTURE |
| Late integration | Broken demo | Merge small PRs early; agree API contracts first |
| Role leakage | Exposure of private financial/client data | Backend authorization tests and ownership checks |
| Project created too early | Invalid booking lifecycle | Enforce signed agreement + paid deposit in service layer and tests |
| Hard-coded demo data | False appearance of functionality | Persist records; test create/update/approve flows |
| File upload complexity | Broken document flow | Start with simple local storage abstraction and strict access checks |
| Financial inconsistencies | Incorrect budget/profit figures | Keep original/revised/actual values distinct; test calculations |
| Time lost on visual polish | Missing business workflows | Use Ant Design defaults; polish after critical flows |

## 13. Assumptions and open implementation choices

- Agency name, branding, logo, visual theme and final color palette are to be chosen by the frontend developer.
- Exact expense approval thresholds are configurable implementation values and must be agreed by the agency; do not hard-code arbitrary business limits without documenting them.
- Cloud provider and production hosting are not selected in this MVP baseline.
- Cloud file-storage provider is not selected; local demo storage may be used if clearly documented and isolated behind a storage service.
- Exact email provider is not selected; email verification/reset may require a configured SMTP/provider. If unavailable in the demo, document the dependency and do not fake successful delivery.
- Exact package templates, task templates, sample categories and report formats should be seeded as editable starter data.
- No GST, payment gateway, guest module, logistics module or cloud deployment in MVP.

## 14. Definition of Done

A MUST HAVE feature is done only when:
1. Its UI is connected to a real API.
2. The API validates input and enforces permissions.
3. Data is persisted in PostgreSQL (or an explicitly documented storage layer for files).
4. Success and failure states are visible and truthful.
5. At least one relevant test or documented manual acceptance test passes.
6. It is integrated into the shared branch and documented.
7. It does not rely on hard-coded records to simulate user actions.

The MVP is demo-ready when the team can complete the critical acceptance scenarios locally from a clean setup.
