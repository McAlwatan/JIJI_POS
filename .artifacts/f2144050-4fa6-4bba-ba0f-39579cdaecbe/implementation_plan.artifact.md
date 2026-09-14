# Redesign Cashier and Manager Dashboards

This plan outlines the redesign of the Cashier and Manager home screens to match the new visual style established in the Customer dashboard. The goal is to provide a consistent, modern UI with a dark header, large metrics, and an elevated action grid.

## Proposed Changes

### UI Components & Fragments

#### [NEW] [fragment_cashier_home.xml](file:///C:/Users/hp/AndroidStudioProjects/JIJIPOS/app/src/main/res/layout/fragment_cashier_home.xml)
A new layout for the Cashier dashboard featuring:
- **Header:** Total Sales for the current period (Today/Week/Month/Year).
- **Floating Action Button:** Quick access to "New Sale" (QR Generator).
- **Grid Actions:**
    - Recent Transactions (History)
    - Inventory Check
    - Register Customer
    - Offline Sync Queue

#### [NEW] [CashierHomeFragment.java](file:///C:/Users/hp/AndroidStudioProjects/JIJIPOS/app/src/main/java/com/example/jijipos/fragments/CashierHomeFragment.java)
Fragment logic to handle the Cashier dashboard, including navigation to Sales and History.

---

#### [NEW] [fragment_manager_home.xml](file:///C:/Users/hp/AndroidStudioProjects/JIJIPOS/app/src/main/res/layout/fragment_manager_home.xml)
A new layout for the Manager dashboard featuring:
- **Header:** Total Business Revenue/Sales metrics.
- **Floating Action Button:** Analytics/Reports summary.
- **Grid Actions:**
    - Detailed Sales Reports
    - Staff Management
    - Inventory Control
    - Business Settings/Notifications

#### [NEW] [ManagerHomeFragment.java](file:///C:/Users/hp/AndroidStudioProjects/JIJIPOS/app/src/main/java/com/example/jijipos/fragments/ManagerHomeFragment.java)
Fragment logic to handle the Manager dashboard, including navigation to Staff and Inventory management.

---

### Navigation & Activity Wiring

#### [MODIFY] [DashboardActivity.java](file:///C:/Users/hp/AndroidStudioProjects/JIJIPOS/app/src/main/java/com/example/jijipos/DashboardActivity.java)
- Update `onCreate` to load `CashierHomeFragment` for cashiers and `ManagerHomeFragment` for managers.
- Properly wire the bottom navigation tabs for these roles to ensure they can navigate back to their respective home screens.
- Fix commented-out navigation logic for role-based fragments.

## Verification Plan

### Automated Tests
- Build the project to ensure all new XML and Java files are correctly compiled.
- `gradlew :app:assembleDebug`

### Manual Verification
- Log in as a **Cashier** and verify the dashboard layout matches the new design.
- Log in as a **Manager** and verify the dashboard layout matches the new design.
- Verify that clicking the "Home" tab in the bottom nav for each role correctly returns to their respective dashboard.
- Verify the Floating Action Button in each dashboard triggers the correct primary action.
