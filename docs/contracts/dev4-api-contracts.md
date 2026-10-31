# DEV 4 API contracts: finance, dashboard, calendar, reports, frontend shell

Owner: DEV 4 (@Vivekr818), branch `dev4`.
Status: **v0.2, proposed.** Decisions marked *Decided* were confirmed by the team lead. Everything else is open for comment in the linked GitHub issues. Nothing in sections 3 to 6 is implemented yet.

## 1. Decisions

| Topic | Decision | Status |
|---|---|---|
| Frontend stack | Vite + React + Ant Design, JavaScript | Decided |
| Charts | Hand-built SVG components, no chart library | Decided |
| Exports | Server-generated CSV (opens in Excel) + browser print-to-PDF. No POI/OpenPDF | Decided |
| Expense approval | Configurable `app.finance.planner-expense-limit`, default INR 50,000 | Decided |
| Booking deposit | Option A: project creation calls finance onboarding (section 3.5) | Decided, contract open |

## 2. Conventions

| Topic | Convention |
|---|---|
| Backend packages | Feature packages `com.wams.finance`, `com.wams.dashboard`, `com.wams.calendar`, `com.wams.report` (same style as `com.wams.enquiry`, `com.wams.vendor`) |
| Entities | Extend `com.wams.entity.BaseEntity` for `created_at` / `updated_at` |
| Security | `@PreAuthorize` on controllers plus project-ownership checks in services. DEV 4 does not edit `SecurityConfig` |
| Errors | `BusinessRuleException` (422), `ResourceNotFoundException` (404), `AccessDeniedException` (403), returned as `ApiError` |
| Money | `BigDecimal`, `NUMERIC(14,2)`, INR, JSON numbers |
| Dates | `LocalDate` as `YYYY-MM-DD`; timestamps as ISO-8601 `Instant` |
| Current user | JWT subject (email) resolved with `UserRepository.findByEmailIgnoreCase` |

### Fields DEV 4 reads from other modules (read-only)

Please tell DEV 4 before renaming any of these.

| Owner | Entity | Fields |
|---|---|---|
| DEV 1 | `WeddingProject` | `id, projectName, weddingDate, venue, projectStatus, leadPlanner, primaryClient, partnerClient, createdAt` |
| DEV 1 | `User`, `Employee`, `Client` | `User.email, fullName, role, active`; `Employee.active, user`; `Client.user` |
| DEV 2 | `Enquiry` | `id, contactName, status, source, createdAt, weddingStartDate, preferredConsultationDate, destination` |
| DEV 3 | `Vendor` | `id, businessName, category, status` |

## 3. Financial management (`/api/finance`)

### 3.1 Tables

| Table | Key columns |
|---|---|
| `project_budgets` | `project_id` (unique FK), `contract_value`, `original_approved_amount`, `current_approved_amount`, `created_by_user_id` |
| `budget_categories` | `budget_id`, `name` (unique per budget), `original_allocated`, `current_allocated` |
| `budget_revisions` | `budget_id`, `category_id` (null = total), `previous_amount`, `requested_amount`, `reason`, `status` (PENDING/APPROVED/REJECTED), `requested_by`, `decided_by`, `decided_at`, `decision_note` |
| `client_payments` | `project_id`, `amount`, `payment_date`, `method` (CASH/BANK_TRANSFER/UPI/CHEQUE/CARD/OTHER), `reference`, `payment_type` (BOOKING_DEPOSIT/INSTALMENT/FINAL/OTHER), `status` (PENDING_VERIFICATION/VERIFIED/REJECTED), `source_deposit_id` (unique, nullable), `recorded_by`, `verified_by`, `verified_at` |
| `expenses` | `project_id`, `category_id`, `vendor_id` (nullable FK `vendors`), `payee_name`, `function_id` (nullable), `document_id` (nullable), `amount`, `expense_date`, `description`, `status` (APPROVED/PENDING_ADMIN_APPROVAL/REJECTED), `recorded_by`, `decided_by`, `decided_at`, `decision_note` |
| `vendor_payments` | `project_id`, `vendor_id` (FK `vendors`), `expense_id` (nullable), `amount`, `payment_date`, `method`, `reference`, `recorded_by` |

### 3.2 Rules

| ID | Rule |
|---|---|
| B0 | One budget per project, created with categories. Category allocations must not exceed the approved total. Original values are never overwritten. |
| B1 | ADMIN budget revision is approved and applied immediately; history is kept. |
| B2 | LEAD_PLANNER budget revision stays PENDING until ADMIN approves (applied) or rejects. |
| E1 | ADMIN-recorded expense is APPROVED. |
| E2 | LEAD_PLANNER-recorded expense is APPROVED when `amount <= app.finance.planner-expense-limit` and category actual + amount <= category current allocation. |
| E3 | Otherwise PENDING_ADMIN_APPROVAL. Only ADMIN approves or rejects; rejection requires a note. |
| P1 | ADMIN-recorded client payment is VERIFIED. LEAD_PLANNER-recorded payment is PENDING_VERIFICATION until ADMIN verifies. Only VERIFIED payments count as income. |
| V1 | A vendor payment cannot exceed that vendor's outstanding amount on the project (approved vendor expenses minus payments). Violations return 422. |
| R1 | Approved, verified and rejected records cannot be edited or deleted in the MVP. |

### 3.3 Calculated figures

- Income received = sum of VERIFIED client payments
- Client outstanding = contract value - income received
- Actual expenses = sum of APPROVED expenses
- Budget remaining = current approved budget - actual expenses
- Vendor billed / paid / outstanding = approved vendor expenses / vendor payments / difference
- Profit to date = income received - actual expenses
- Expected profit = contract value - current approved budget

### 3.4 Endpoints

| Method and path | Roles | Purpose |
|---|---|---|
| `GET /api/finance/projects` | ADMIN (all), LEAD_PLANNER (own) | Projects with summary figures |
| `GET /api/finance/projects/{projectId}/summary` | ADMIN, LEAD_PLANNER (own) | Summary plus category budget vs actual |
| `POST /api/finance/projects/{projectId}/budget` | ADMIN, LEAD_PLANNER (own) | Create budget and categories (once) |
| `GET /api/finance/projects/{projectId}/budget` | ADMIN, LEAD_PLANNER (own) | Budget with categories |
| `POST /api/finance/projects/{projectId}/budget/categories` | ADMIN, LEAD_PLANNER (own) | Add category (allocation 0, change through revision) |
| `GET/POST /api/finance/projects/{projectId}/budget/revisions` | ADMIN, LEAD_PLANNER (own) | List or request revision |
| `POST /api/finance/budget-revisions/{id}/approve`, `/reject` | ADMIN | Decide revision |
| `GET/POST /api/finance/projects/{projectId}/client-payments` | ADMIN, LEAD_PLANNER (own) | List or record payment |
| `POST /api/finance/client-payments/{id}/verify`, `/reject` | ADMIN | Verify payment |
| `GET/POST /api/finance/projects/{projectId}/expenses` | ADMIN, LEAD_PLANNER (own) | List or record expense |
| `POST /api/finance/expenses/{id}/approve`, `/reject` | ADMIN | Decide expense |
| `GET/POST /api/finance/projects/{projectId}/vendor-payments` | ADMIN, LEAD_PLANNER (own) | List or record vendor payment |
| `GET /api/finance/approvals/pending` | ADMIN | Pending expenses, revisions, unverified payments |
| `GET /api/finance/projects/{projectId}/client-summary` | PRIMARY_CLIENT (own project) | Client-safe view for DEV 3's client portal |

### 3.5 Java contracts for other modules

**Booking deposit and agreed price (DEV 2, Option A).** Called once inside the project-creation transaction.

```java
package com.wams.finance;

public interface ProjectFinanceOnboarding {
    /** Idempotent per sourceDepositId. Throws BusinessRuleException (422) on invalid input,
     *  which rolls back the project creation transaction. */
    void onProjectCreated(ProjectFinanceOnboardingCommand command);
}

public record ProjectFinanceOnboardingCommand(
        Long projectId,                 // required
        BigDecimal agreedContractValue, // optional, final agreed package price (INR)
        BigDecimal depositAmount,       // required, > 0
        LocalDate depositPaidOn,        // required, not in the future
        PaymentMethod depositMethod,    // required: CASH, BANK_TRANSFER, UPI, CHEQUE, CARD, OTHER
        String depositReference,        // optional, max 100 chars
        Long sourceDepositId,           // required, DEV 2's deposit record id (idempotency key)
        String recordedByEmail          // required, user who confirmed the deposit
) {}
```

Effect: one `client_payments` row with `payment_type = BOOKING_DEPOSIT`, `status = VERIFIED`, `source_deposit_id = sourceDepositId`, and the contract value stored on the project's finance record.

**Vendor portal payments (DEV 3).**

```java
com.wams.finance.VendorPaymentQueryService#listForVendor(Long vendorId) -> List<VendorPaymentView>
// VendorPaymentView: id, projectId, projectName, amount, paymentDate, method, reference
```

## 4. Admin dashboard (`GET /api/dashboard/admin`, ADMIN only)

- `counts`: projects (total, by status), enquiries (total, by status), clients, vendors (approved, pending), active employees
- `enquiryConversion`: by status, converted / total, lost / total
- `upcomingWeddings`: next 8 projects with `weddingDate >= today`, excluding CANCELLED and COMPLETED
- `finance`: income received, actual expenses, client outstanding, vendor outstanding, pending approvals
- `monthlyCashflow`: last 12 months, verified income vs approved expenses
- `projectProfit`: per project contract value, income, expenses, profit to date
- `recentActivity`: latest 15 events derived from persisted records (enquiries, projects, payments, expenses)

## 5. Calendar

| Method and path | Roles |
|---|---|
| `GET /api/calendar/items?from=YYYY-MM-DD&to=YYYY-MM-DD` (max 62 days) | ADMIN (all), LEAD_PLANNER (own projects and events), EMPLOYEE (after assignments exist) |
| `POST /api/calendar/events`, `PUT/DELETE /api/calendar/events/{id}` | ADMIN, LEAD_PLANNER (own project or own event) |

`calendar_events`: `title, type (MEETING/CONSULTATION/DEADLINE), event_date, start_time, end_time, project_id, enquiry_id, notes, created_by`.

Extension point for DEV 2 (functions and task due dates):

```java
package com.wams.calendar;

public interface CalendarItemProvider {
    List<CalendarItem> findItems(LocalDate from, LocalDate to, CalendarScope scope);
}

public record CalendarItem(String key, CalendarItemType type, String title, LocalDate date,
                           LocalTime startTime, LocalTime endTime,
                           Long projectId, Long sourceId, String status) {}

// CalendarItemType: WEDDING, FUNCTION, CONSULTATION_REQUEST, CONSULTATION, MEETING, DEADLINE, TASK_DUE
// CalendarScope: boolean admin; String userEmail; Set<Long> visibleProjectIds (null = all)
```

## 6. Reports (`GET /api/reports/{key}?from=&to=&projectId=&format=json|csv`)

| Key | Roles |
|---|---|
| `project-financials`, `client-payments`, `expenses`, `vendor-payments` | ADMIN, LEAD_PLANNER (own projects) |
| `enquiries`, `projects` | ADMIN |

CSV is UTF-8 with BOM so Excel opens it correctly. PDF is produced by the browser's print dialog from a print stylesheet.

## 7. Frontend shell

See [`frontend/README.md`](../../frontend/README.md) for structure, the module registry and conventions.

| Concern | Contract |
|---|---|
| API base | Relative `/api/...`; Vite dev server proxies to `http://localhost:8080` |
| Auth | `POST /api/auth/login`, `GET /api/auth/me` (DEV 1). Token in `sessionStorage`, sent as `Authorization: Bearer` |
| 401 | Session cleared, user returned to `/login` |
| 403 | Shown as access denied; session kept |
| Roles | `ADMIN, LEAD_PLANNER, EMPLOYEE, PRIMARY_CLIENT, PARTNER_CLIENT, VENDOR` |
| Public pages | `/` (landing), `/login`. Authenticated pages live under `/app/*` |
